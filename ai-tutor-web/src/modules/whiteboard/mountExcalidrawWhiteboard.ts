import { createElement } from 'react'
import { createRoot, type Root } from 'react-dom/client'
import ExcalidrawWhiteboardHost from './ExcalidrawWhiteboardHost'
import type { WhiteboardHostProps } from './whiteboardTypes'

export function mountExcalidrawWhiteboard(el: HTMLElement, props: WhiteboardHostProps) {
  const root: Root = createRoot(el)
  root.render(createElement(ExcalidrawWhiteboardHost, props))
  return {
    update(nextProps: WhiteboardHostProps) {
      root.render(createElement(ExcalidrawWhiteboardHost, nextProps))
    },
    unmount() {
      root.unmount()
    },
  }
}
