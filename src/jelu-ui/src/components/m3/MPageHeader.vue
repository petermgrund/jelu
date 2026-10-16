<script setup lang="ts">
/**
 * Page title row: headline, optional supporting text (prop or #subtitle slot)
 * and trailing actions (#actions slot).
 */
const props = defineProps<{
  title?: string,
  subtitle?: string,
}>()
</script>

<template>
  <header class="m3-page-header">
    <div class="m3-page-header__titles">
      <h1 class="m3-page-header__title">
        <slot name="title">
          {{ props.title }}
        </slot>
      </h1>
      <p
        v-if="props.subtitle || $slots.subtitle"
        class="m3-page-header__subtitle"
      >
        <slot name="subtitle">
          {{ props.subtitle }}
        </slot>
      </p>
    </div>
    <div
      v-if="$slots.actions"
      class="m3-page-header__actions"
    >
      <slot name="actions" />
    </div>
  </header>
</template>
