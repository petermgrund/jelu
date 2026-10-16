<script setup lang="ts">
/** Placeholder grid shown while books are loading. */
const props = withDefaults(defineProps<{
  count?: number,
}>(), {
  count: 8,
})
</script>

<template>
  <div
    class="jl-book-grid"
    aria-hidden="true"
  >
    <div
      v-for="index in props.count"
      :key="index"
      class="flex flex-col gap-2"
    >
      <div class="m3-skeleton aspect-[2/3] w-full" />
      <div class="m3-skeleton h-4 w-4/5 rounded-xs" />
      <div class="m3-skeleton h-3 w-1/2 rounded-xs" />
    </div>
  </div>
</template>
