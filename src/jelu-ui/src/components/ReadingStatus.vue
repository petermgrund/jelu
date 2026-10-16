<script setup lang="ts">
import useEvents from '../composables/events'
import { ReadingEventType } from '../model/ReadingEvent'

/** Tonal label for a reading status: reading, finished, dropped. */
const props = defineProps<{
  type: ReadingEventType | null | undefined,
  label?: string,
}>()

const { eventClass, eventIcon, eventLabel } = useEvents()
</script>

<template>
  <span
    v-if="props.type"
    class="m3-status"
    :class="eventClass(props.type)"
  >
    <i
      class="mdi"
      :class="eventIcon(props.type)"
      aria-hidden="true"
    />
    <span>{{ props.label ?? eventLabel(props.type) }}</span>
  </span>
</template>
