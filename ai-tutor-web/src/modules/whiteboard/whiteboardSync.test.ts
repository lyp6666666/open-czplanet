import { describe, expect, it, vi } from 'vitest'

import type { LiveRoomDataMessage } from '@/modules/live/livekit'
import {
  publishWhiteboardScene,
  subscribeWhiteboardScene,
  WHITEBOARD_SCENE_TOPIC,
} from './whiteboardSync'
import type { WhiteboardRoomClient, WhiteboardScene } from './whiteboardTypes'

function makeScene(text: string): WhiteboardScene {
  return {
    elements: [{ id: text, type: 'freedraw', version: 1 }],
    appState: { viewBackgroundColor: '#fffaf0' },
    files: {},
  }
}

describe('whiteboardSync', () => {
  it('publishes scene snapshots over the classroom room data channel', async () => {
    const sendData = vi.fn().mockResolvedValue(undefined)
    const roomClient = {
      sendData,
      onDataReceived: vi.fn(),
    } satisfies WhiteboardRoomClient

    await publishWhiteboardScene(roomClient, 8, 1001, 2, makeScene('line-a'))

    expect(sendData).toHaveBeenCalledWith(
      WHITEBOARD_SCENE_TOPIC,
      expect.objectContaining({
        kind: 'scene',
        sessionId: 8,
        senderUid: 1001,
        version: 2,
        scene: expect.objectContaining({
          elements: [expect.objectContaining({ id: 'line-a' })],
        }),
      }),
      true,
    )
  })

  it('applies peer whiteboard updates and ignores messages from self or other sessions', () => {
    let dataHandler: ((message: LiveRoomDataMessage) => void) | null = null
    const unsubscribe = vi.fn()
    const roomClient = {
      sendData: vi.fn(),
      onDataReceived: vi.fn((handler: (message: LiveRoomDataMessage) => void) => {
        dataHandler = handler
        return unsubscribe
      }),
    } satisfies WhiteboardRoomClient
    const onRemoteScene = vi.fn()
    const onStatus = vi.fn()
    const emitData = (message: LiveRoomDataMessage) => {
      expect(dataHandler).toBeTruthy()
      dataHandler!(message)
    }

    const stop = subscribeWhiteboardScene(roomClient, 8, 1001, onRemoteScene, onStatus)

    emitData({
      topic: WHITEBOARD_SCENE_TOPIC,
      payload: { kind: 'scene', sessionId: 9, senderUid: 1002, version: 1, scene: makeScene('wrong-session') },
    })
    emitData({
      topic: WHITEBOARD_SCENE_TOPIC,
      payload: { kind: 'scene', sessionId: 8, senderUid: 1001, version: 1, scene: makeScene('self') },
    })
    emitData({
      topic: WHITEBOARD_SCENE_TOPIC,
      payload: { kind: 'scene', sessionId: 8, senderUid: 1002, version: 3, scene: makeScene('peer-line') },
    })

    expect(onRemoteScene).toHaveBeenCalledTimes(1)
    expect(onRemoteScene).toHaveBeenCalledWith(expect.objectContaining({
      elements: [expect.objectContaining({ id: 'peer-line' })],
    }), 3)
    expect(onStatus).toHaveBeenCalledWith('synced', '已接收对方白板更新')

    stop()
    expect(unsubscribe).toHaveBeenCalledTimes(1)
  })
})
