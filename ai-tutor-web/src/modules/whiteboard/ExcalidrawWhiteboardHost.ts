import { createElement, useEffect, useMemo, useRef, useState } from 'react'
import { Excalidraw, CaptureUpdateAction } from '@excalidraw/excalidraw'
import type { ExcalidrawImperativeAPI } from '@excalidraw/excalidraw/types'
import '@excalidraw/excalidraw/index.css'

import {
  emptyWhiteboardScene,
  isWhiteboardContentEqual,
  isWhiteboardSceneEqual,
  loadWhiteboardSnapshot,
  publishWhiteboardScene,
  saveWhiteboardSnapshot,
  subscribeWhiteboardScene,
} from './whiteboardSync'
import type { WhiteboardHostProps, WhiteboardSaveState, WhiteboardScene } from './whiteboardTypes'

function sanitizeAppState(appState: Record<string, unknown>) {
  const allowedKeys = [
    'viewBackgroundColor',
    'scrollX',
    'scrollY',
    'zoom',
    'gridSize',
    'objectsSnapModeEnabled',
    'currentItemStrokeColor',
    'currentItemBackgroundColor',
    'currentItemFillStyle',
    'currentItemStrokeWidth',
    'currentItemRoughness',
    'currentItemOpacity',
    'currentItemFontFamily',
    'currentItemFontSize',
    'currentItemTextAlign',
    'currentItemStartArrowhead',
    'currentItemEndArrowhead',
  ]
  return allowedKeys.reduce<Record<string, unknown>>((next, key) => {
    if (key in appState) next[key] = appState[key]
    return next
  }, {})
}

function makeScene(elements: readonly unknown[], appState: Record<string, unknown>, files: Record<string, unknown>): WhiteboardScene {
  return {
    elements: Array.from(elements),
    appState: sanitizeAppState(appState),
    files,
  }
}

export default function ExcalidrawWhiteboardHost(props: WhiteboardHostProps) {
  const [initialScene, setInitialScene] = useState<WhiteboardScene | null>(null)
  const [status, setStatus] = useState<WhiteboardSaveState>('loading')
  const [message, setMessage] = useState('正在打开本节课白板')
  const apiRef = useRef<ExcalidrawImperativeAPI | null>(null)
  const sceneRef = useRef<WhiteboardScene>(emptyWhiteboardScene())
  const sceneVersionRef = useRef(0)
  const applyingRemoteRef = useRef(false)
  const broadcastTimerRef = useRef<number | null>(null)
  const saveTimerRef = useRef<number | null>(null)
  const pollTimerRef = useRef<number | null>(null)

  const initialData = useMemo(() => {
    if (!initialScene) return null
    return {
      elements: initialScene.elements,
      appState: {
        ...initialScene.appState,
        viewBackgroundColor: String(initialScene.appState.viewBackgroundColor || '#fffaf0'),
      },
      files: initialScene.files || {},
    } as unknown
  }, [initialScene])

  function setPanelStatus(nextStatus: WhiteboardSaveState, nextMessage?: string) {
    setStatus(nextStatus)
    setMessage(nextMessage || '')
    props.onStatusChange?.(nextStatus, nextMessage)
  }

  function clearTimers() {
    if (broadcastTimerRef.current != null) window.clearTimeout(broadcastTimerRef.current)
    if (saveTimerRef.current != null) window.clearTimeout(saveTimerRef.current)
    if (pollTimerRef.current != null) window.clearInterval(pollTimerRef.current)
    broadcastTimerRef.current = null
    saveTimerRef.current = null
    pollTimerRef.current = null
  }

  function applyRemoteScene(scene: WhiteboardScene, version: number, source: 'live' | 'snapshot') {
    if (version < sceneVersionRef.current) return
    if (version === sceneVersionRef.current && isWhiteboardSceneEqual(sceneRef.current, scene)) return
    applyingRemoteRef.current = true
    sceneVersionRef.current = version
    sceneRef.current = scene
    apiRef.current?.addFiles(Object.values(scene.files || {}) as never)
    apiRef.current?.updateScene({
      elements: scene.elements,
      appState: scene.appState,
      collaborators: new Map(),
      captureUpdate: CaptureUpdateAction.NEVER,
    } as never)
    setPanelStatus('synced', source === 'live' ? '已接收对方白板更新' : '已同步对方白板')
    window.setTimeout(() => {
      applyingRemoteRef.current = false
    }, 300)
  }

  async function persistCurrentScene(finalize = false) {
    if (props.readonly) return
    const scene = sceneRef.current
    setPanelStatus('saving', finalize ? '正在保存最终白板' : '正在同步白板')
    try {
      const snapshot = await saveWhiteboardSnapshot(props.sessionId, sceneVersionRef.current, scene, finalize)
      sceneVersionRef.current = snapshot.sceneVersion
      setPanelStatus(snapshot.finalized ? 'readonly' : 'synced', snapshot.finalized ? '白板已归档' : '已同步')
    } catch (error) {
      setPanelStatus('offline', error instanceof Error ? error.message : '白板暂未同步，稍后自动重试')
    }
  }

  function scheduleSync() {
    if (props.readonly) return
    if (broadcastTimerRef.current != null) window.clearTimeout(broadcastTimerRef.current)
    broadcastTimerRef.current = window.setTimeout(() => {
      sceneVersionRef.current += 1
      void publishWhiteboardScene(
        props.roomClient,
        props.sessionId,
        props.currentUser.uid,
        sceneVersionRef.current,
        sceneRef.current,
      ).catch(() => {
        setPanelStatus('offline', '实时同步较慢，已保留本地白板')
      })
    }, 120)
    if (saveTimerRef.current != null) window.clearTimeout(saveTimerRef.current)
    saveTimerRef.current = window.setTimeout(() => {
      void persistCurrentScene(false)
    }, 1200)
  }

  useEffect(() => {
    let disposed = false
    setPanelStatus('loading', '正在打开本节课白板')
    loadWhiteboardSnapshot(props.sessionId)
      .then((snapshot) => {
        if (disposed) return
        sceneRef.current = snapshot.scene
        sceneVersionRef.current = snapshot.sceneVersion
        setInitialScene(snapshot.scene)
        setPanelStatus(snapshot.finalized ? 'readonly' : 'synced', snapshot.finalized ? '白板已归档，只读查看' : '白板已打开')
      })
      .catch((error) => {
        if (disposed) return
        const fallback = emptyWhiteboardScene()
        sceneRef.current = fallback
        setInitialScene(fallback)
        setPanelStatus('offline', error instanceof Error ? error.message : '白板打开失败，已进入本地模式')
      })
    return () => {
      disposed = true
    }
  }, [props.sessionId])

  useEffect(() => {
    return subscribeWhiteboardScene(props.roomClient, props.sessionId, props.currentUser.uid, (scene, version) => {
      applyRemoteScene(scene, version, 'live')
    }, setPanelStatus)
  }, [props.currentUser.uid, props.roomClient, props.sessionId])

  useEffect(() => {
    pollTimerRef.current = window.setInterval(() => {
      void loadWhiteboardSnapshot(props.sessionId)
        .then((snapshot) => {
          applyRemoteScene(snapshot.scene, snapshot.sceneVersion, 'snapshot')
          if (snapshot.finalized) setPanelStatus('readonly', '白板已归档，只读查看')
        })
        .catch(() => undefined)
    }, 2000)
    return () => {
      if (pollTimerRef.current != null) window.clearInterval(pollTimerRef.current)
      pollTimerRef.current = null
    }
  }, [props.sessionId])

  useEffect(() => {
    const handleFlush = (event: Event) => {
      const detail = event instanceof CustomEvent ? event.detail as { promises?: Promise<unknown>[]; finalize?: boolean } : null
      const promise = persistCurrentScene(Boolean(detail?.finalize))
      detail?.promises?.push(promise)
    }
    const handleBeforeUnload = () => {
      void persistCurrentScene(false)
    }
    window.addEventListener('live-whiteboard-flush', handleFlush)
    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => {
      window.removeEventListener('live-whiteboard-flush', handleFlush)
      window.removeEventListener('beforeunload', handleBeforeUnload)
      clearTimers()
      void persistCurrentScene(false)
    }
  }, [])

  if (!initialData) {
    return createElement(
      'div',
      { className: 'whiteboard-loading', 'data-testid': 'live-whiteboard-loading' },
      createElement('div', { className: 'whiteboard-loading-orb' }),
      createElement('strong', null, message || '正在打开白板'),
      createElement('span', null, '白板会和本节课自动绑定'),
    )
  }

  return createElement(
    'div',
    { className: 'whiteboard-react-host', 'data-testid': 'live-whiteboard-react-host' },
    createElement(
      'div',
      { className: 'whiteboard-sync-chip', 'data-state': status },
      createElement('span'),
      createElement('strong', null, message || (status === 'saving' ? '正在同步' : '已同步')),
    ),
    createElement(Excalidraw as never, {
      initialData,
      excalidrawAPI: (api: ExcalidrawImperativeAPI) => {
          apiRef.current = api
          window.setTimeout(() => api.refresh(), 60)
      },
      onChange: (elements: readonly unknown[], appState: unknown, files: unknown) => {
          if (applyingRemoteRef.current) return
          const nextScene = makeScene(elements, appState as unknown as Record<string, unknown>, files as unknown as Record<string, unknown>)
          if (isWhiteboardContentEqual(sceneRef.current, nextScene)) return
          sceneRef.current = nextScene
          setPanelStatus('saving', '正在记录白板')
          scheduleSync()
      },
      langCode: 'zh-CN',
      theme: 'light',
      name: `课堂白板-${props.sessionId}`,
      isCollaborating: true,
      viewModeEnabled: props.readonly,
      UIOptions: {
        canvasActions: {
          loadScene: false,
          saveToActiveFile: false,
          export: false,
          saveAsImage: false,
        },
      },
    } as never),
  )
}
