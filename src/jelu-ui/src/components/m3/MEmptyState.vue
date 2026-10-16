<script setup lang="ts">
/** Centered empty state: tonal icon, title, supporting text and optional actions. */
const props = withDefaults(defineProps<{
  icon?: string,
  title: string,
  text?: string,
}>(), {
  icon: 'book-open-page-variant-outline',
})
</script>

<template>
  <div class="m3-empty">
    <div class="m3-empty__icon">
      <i
        class="mdi"
        :class="`mdi-${props.icon}`"
        aria-hidden="true"
      />
    </div>
    <h2 class="m3-empty__title">
      {{ props.title }}
    </h2>
    <p
      v-if="props.text"
      class="m3-empty__text"
    >
      {{ props.text }}
    </p>
    <div
      v-if="$slots.default"
      class="mt-4 flex flex-wrap justify-center gap-2"
    >
      <slot />
    </div>
  </div>
</template>
