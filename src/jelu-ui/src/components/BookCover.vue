<script setup lang="ts">
import { computed, ref, watch } from 'vue'

/**
 * Book cover with a 2:3 frame and a tonal placeholder when there is no image
 * or it fails to load. `image` is either a Jelu file name or an absolute url.
 */
const props = withDefaults(defineProps<{
  image?: string | null,
  title?: string,
  cacheKey?: string,
  rounded?: boolean,
  elevated?: boolean,
}>(), {
  image: null,
  title: '',
  cacheKey: undefined,
})

const failed = ref(false)

watch(() => props.image, () => {
  failed.value = false
})

const src = computed(() => {
  if (props.image == null || props.image.trim().length === 0) {
    return null
  }
  if (props.image.startsWith('http') || props.image.startsWith('data:') || props.image.startsWith('blob:')) {
    return props.image
  }
  const path = props.image.startsWith('/files/') ? props.image : `/files/${props.image}`
  return props.cacheKey ? `${path}?timestamp=${props.cacheKey}` : path
})
</script>

<template>
  <div
    class="relative aspect-[2/3] w-full overflow-hidden bg-surface-container-highest"
    :class="{ 'rounded-md': props.rounded, 'shadow-elevation-2': props.elevated }"
  >
    <img
      v-if="src && !failed"
      :src="src"
      :alt="props.title"
      loading="lazy"
      class="h-full w-full object-cover"
      @error="failed = true"
    >
    <div
      v-else
      class="flex h-full w-full flex-col items-center justify-center gap-3 p-4 text-center text-on-surface-variant"
    >
      <i
        class="mdi mdi-book-open-page-variant-outline text-[2.5rem] leading-none text-outline"
        aria-hidden="true"
      />
      <span class="line-clamp-4 text-title-small">{{ props.title }}</span>
    </div>
    <slot />
  </div>
</template>
