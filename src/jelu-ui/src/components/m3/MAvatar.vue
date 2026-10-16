<script setup lang="ts">
import { computed } from 'vue'

/** Initials avatar, the tonal color is derived from the name so it stays stable. */
const props = withDefaults(defineProps<{
  name: string,
  size?: 'sm' | 'md' | 'lg',
}>(), {
  size: 'md',
})

const initials = computed(() => {
  const parts = props.name.trim().split(/[\s._-]+/).filter(p => p.length > 0)
  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase()
  }
  return props.name.trim().slice(0, 2).toUpperCase()
})

const tones = [
  'bg-primary-container text-on-primary-container',
  'bg-secondary-container text-on-secondary-container',
  'bg-tertiary-container text-on-tertiary-container',
]

const tone = computed(() => {
  let hash = 0
  for (const char of props.name) {
    hash = (hash * 31 + char.charCodeAt(0)) | 0
  }
  return tones[Math.abs(hash) % tones.length]
})
</script>

<template>
  <span
    class="m3-avatar"
    :class="[tone, { 'm3-avatar--sm': props.size === 'sm', 'm3-avatar--lg': props.size === 'lg' }]"
    :title="props.name"
    aria-hidden="true"
  >{{ initials }}</span>
</template>
