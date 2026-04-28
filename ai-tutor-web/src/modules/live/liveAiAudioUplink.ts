import { liveApi } from '@/api/live'

type LiveAiAudioUplinkOptions = {
  sessionId: number
  micDeviceId?: string | null
  minRms?: number
  silencePauseMs?: number
}

type LiveAiAudioUplink = {
  stop: () => void
}

const TARGET_SAMPLE_RATE = 16000

function encodePcm16Base64(samples: Float32Array) {
  const bytes = new Uint8Array(samples.length * 2)
  const view = new DataView(bytes.buffer)
  for (let i = 0; i < samples.length; i += 1) {
    const clamped = Math.max(-1, Math.min(1, samples[i] ?? 0))
    view.setInt16(i * 2, clamped < 0 ? clamped * 0x8000 : clamped * 0x7fff, true)
  }
  let binary = ''
  const chunkSize = 0x8000
  for (let i = 0; i < bytes.length; i += chunkSize) {
    binary += String.fromCharCode(...Array.from(bytes.subarray(i, i + chunkSize)))
  }
  return btoa(binary)
}

function downsample(input: Float32Array, sourceRate: number) {
  if (sourceRate === TARGET_SAMPLE_RATE) return input
  const ratio = sourceRate / TARGET_SAMPLE_RATE
  const length = Math.floor(input.length / ratio)
  const output = new Float32Array(length)
  for (let i = 0; i < length; i += 1) {
    const start = Math.floor(i * ratio)
    const end = Math.min(input.length, Math.floor((i + 1) * ratio))
    let sum = 0
    for (let j = start; j < end; j += 1) sum += input[j] ?? 0
    output[i] = sum / Math.max(1, end - start)
  }
  return output
}

function calculateRms(samples: Float32Array) {
  if (!samples.length) return 0
  let sum = 0
  for (const sample of samples) sum += sample * sample
  return Math.sqrt(sum / samples.length)
}

export async function startLiveAiAudioUplink(options: LiveAiAudioUplinkOptions): Promise<LiveAiAudioUplink> {
  const stream = await navigator.mediaDevices.getUserMedia({
    audio: options.micDeviceId ? { deviceId: { exact: options.micDeviceId } } : true,
    video: false,
  })
  const AudioContextCtor = window.AudioContext || (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
  if (!AudioContextCtor) {
    stream.getTracks().forEach((track) => track.stop())
    throw new Error('当前浏览器不支持课堂 AI 音频采集')
  }

  const audioContext = new AudioContextCtor()
  const source = audioContext.createMediaStreamSource(stream)
  const processor = audioContext.createScriptProcessor(4096, 1, 1)
  const minRms = options.minRms ?? 0.012
  const silencePauseMs = options.silencePauseMs ?? 20_000
  let sequence = 0
  let inFlight = false
  let stopped = false
  let lastVoiceAt = Date.now()
  let pending: Float32Array[] = []
  let pendingLength = 0

  async function flush(samples: Float32Array, rms: number) {
    if (stopped || inFlight) return
    inFlight = true
    try {
      const pcm = downsample(samples, audioContext.sampleRate)
      await liveApi.uploadAiAudioChunk(options.sessionId, {
        sequence,
        sampleRate: TARGET_SAMPLE_RATE,
        channelCount: 1,
        durationMs: Math.round((pcm.length / TARGET_SAMPLE_RATE) * 1000),
        rms,
        format: 'PCM16',
        audioBase64: encodePcm16Base64(pcm),
      })
      sequence += 1
    } catch {
      // AI 音频旁路失败不能影响 LiveKit 课堂本身，下一段有效语音会继续尝试。
    } finally {
      inFlight = false
    }
  }

  processor.onaudioprocess = (event) => {
    if (stopped) return
    const input = event.inputBuffer.getChannelData(0)
    pending.push(new Float32Array(input))
    pendingLength += input.length
    if (pendingLength < audioContext.sampleRate) return

    const merged = new Float32Array(pendingLength)
    let offset = 0
    for (const item of pending) {
      merged.set(item, offset)
      offset += item.length
    }
    pending = []
    pendingLength = 0

    const rms = calculateRms(merged)
    if (rms < minRms) {
      if (Date.now() - lastVoiceAt > silencePauseMs) {
        return
      }
      return
    }
    lastVoiceAt = Date.now()
    void flush(merged, rms)
  }

  source.connect(processor)
  processor.connect(audioContext.destination)

  return {
    stop() {
      stopped = true
      processor.disconnect()
      source.disconnect()
      stream.getTracks().forEach((track) => track.stop())
      void audioContext.close()
    },
  }
}
