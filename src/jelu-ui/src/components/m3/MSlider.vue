<script setup lang="ts">
import { computed, useId } from 'vue'

/** M3 slider on a native range input, with an optional label and value. */
const props = withDefaults(defineProps<{
  min?: number,
  max?: number,
  step?: number,
  label?: string,
  disabled?: boolean,
  showValue?: boolean,
  unit?: string,
}>(), {
  min: 0,
  max: 100,
  step: 1,
  unit: '',
})

const model = defineModel<number | null | undefined>()

const id = useId()

const percent = computed(() => {
  const value = model.value ?? props.min
  const range = props.max - props.min
  return range > 0 ? ((value - props.min) * 100) / range : 0
})

const onInput = (event: Event) => {
  model.value = Number((event.target as HTMLInputElement).value)
}
</script>

<template>
  <div class="flex flex-col gap-1">
    <div
      v-if="props.label || props.showValue"
      class="flex items-center justify-between gap-4"
    >
      <label
        v-if="props.label"
        :for="id"
        class="text-body-medium text-on-surface-variant"
      >{{ props.label }}</label>
      <span
        v-if="props.showValue"
        class="text-label-large text-on-surface tabular-nums"
      >{{ Math.round(model ?? props.min) }}{{ props.unit }}</span>
    </div>
    <input
      :id="id"
      type="range"
      class="m3-slider"
      :min="props.min"
      :max="props.max"
      :step="props.step"
      :value="model ?? props.min"
      :disabled="props.disabled"
      :style="{ '--m3-slider-value': `${percent}%` }"
      @input="onInput"
    >
  </div>
</template>
