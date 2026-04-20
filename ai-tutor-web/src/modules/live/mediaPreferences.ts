const STORAGE_KEY = 'ai_tutor_live_media_preferences'

export type LiveMediaPreferences = {
  cameraEnabled: boolean
  micEnabled: boolean
  speakerChecked: boolean
  cameraDeviceId: string | null
  micDeviceId: string | null
  speakerDeviceId: string | null
}

const defaultPreferences: LiveMediaPreferences = {
  cameraEnabled: true,
  micEnabled: true,
  speakerChecked: false,
  cameraDeviceId: null,
  micDeviceId: null,
  speakerDeviceId: null,
}

export function readLiveMediaPreferences(): LiveMediaPreferences {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return { ...defaultPreferences }
    const parsed = JSON.parse(raw) as Partial<LiveMediaPreferences>
    return {
      cameraEnabled: parsed.cameraEnabled ?? defaultPreferences.cameraEnabled,
      micEnabled: parsed.micEnabled ?? defaultPreferences.micEnabled,
      speakerChecked: parsed.speakerChecked ?? defaultPreferences.speakerChecked,
      cameraDeviceId: parsed.cameraDeviceId ?? defaultPreferences.cameraDeviceId,
      micDeviceId: parsed.micDeviceId ?? defaultPreferences.micDeviceId,
      speakerDeviceId: parsed.speakerDeviceId ?? defaultPreferences.speakerDeviceId,
    }
  } catch {
    return { ...defaultPreferences }
  }
}

export function saveLiveMediaPreferences(next: LiveMediaPreferences) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
}
