<script setup lang="ts">
import { computed } from 'vue'
import useBookStatus, { bookStatus } from '../composables/bookStatus'
import { ReadingEventType } from '../model/ReadingEvent'

/** Tonal label for the status of a book: want to read, reading, read, stopped. */
const props = defineProps<{
  book: { toRead?: boolean | null, lastReadingEvent?: ReadingEventType | null } | null | undefined,
}>()

const { statusClass, statusIcon, statusLabel } = useBookStatus()

const status = computed(() => bookStatus(props.book))
</script>

<template>
  <span
    v-if="status"
    class="m3-status"
    :class="statusClass(status)"
  >
    <i
      class="mdi"
      :class="statusIcon(status)"
      aria-hidden="true"
    />
    <span>{{ statusLabel(status) }}</span>
  </span>
</template>
