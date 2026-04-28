<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

import type { WhiteboardRoomClient, WhiteboardSaveState, WhiteboardUser } from '@/modules/whiteboard/whiteboardTypes'

const props = defineProps<{
  sessionId: number
  currentUser: WhiteboardUser
  readonly?: boolean
  roomClient?: WhiteboardRoomClient | null
}>()

const emit = defineEmits<{
  statusChange: [state: WhiteboardSaveState, message?: string]
}>()

const hostRef = ref<HTMLElement | null>(null)
type WhiteboardMount = Awaited<typeof import('@/modules/whiteboard/mountExcalidrawWhiteboard')>['mountExcalidrawWhiteboard']
let whiteboard: ReturnType<WhiteboardMount> | null = null
let disposed = false
const whiteboardReady = ref(false)

const hostProps = computed(() => ({
  sessionId: props.sessionId,
  currentUser: props.currentUser,
  readonly: props.readonly,
  roomClient: props.roomClient || null,
  onStatusChange: (state: WhiteboardSaveState, message?: string) => emit('statusChange', state, message),
}))

onMounted(async () => {
  if (!hostRef.value) return
  const { mountExcalidrawWhiteboard } = await import('@/modules/whiteboard/mountExcalidrawWhiteboard')
  if (disposed || !hostRef.value) return
  whiteboard = mountExcalidrawWhiteboard(hostRef.value, hostProps.value)
  whiteboardReady.value = true
})

watch(hostProps, (nextProps) => {
  whiteboard?.update(nextProps)
})

onBeforeUnmount(() => {
  disposed = true
  whiteboard?.unmount()
  whiteboard = null
  whiteboardReady.value = false
})
</script>

<template>
  <div ref="hostRef" class="live-whiteboard-panel" data-testid="live-whiteboard-panel">
    <div v-if="!whiteboardReady" class="whiteboard-module-loading">正在加载白板工具...</div>
  </div>
</template>

<style scoped>
.live-whiteboard-panel {
  width: 100%;
  height: 100%;
  min-height: 0;
}

.whiteboard-module-loading {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: #0f766e;
  font-weight: 900;
}
</style>
