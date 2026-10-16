<script setup lang="ts" generic="T">
import { MOption } from './types'

/**
 * Set of M3 filter chips. Multi select (v-model is an array) unless
 * `single` is set, then v-model is a value.
 */
const props = defineProps<{
  options: Array<MOption<T>>,
  single?: boolean,
  ariaLabel?: string,
}>()

// any: the model is a value or an array of values depending on the mode
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const model = defineModel<any>()

const isSelected = (value: T): boolean => {
  if (props.single) {
    return model.value === value
  }
  return Array.isArray(model.value) && model.value.includes(value)
}

const toggle = (value: T) => {
  if (props.single) {
    model.value = value
    return
  }
  const current = Array.isArray(model.value) ? [...model.value] : []
  const index = current.indexOf(value)
  if (index >= 0) {
    current.splice(index, 1)
  } else {
    current.push(value)
  }
  model.value = current
}
</script>

<template>
  <div
    class="m3-chip-set"
    role="group"
    :aria-label="props.ariaLabel"
  >
    <button
      v-for="option in props.options"
      :key="String(option.value)"
      type="button"
      class="m3-chip"
      :class="{ 'is-selected': isSelected(option.value) }"
      :aria-pressed="isSelected(option.value)"
      :disabled="option.disabled"
      @click="toggle(option.value)"
    >
      <i
        v-if="isSelected(option.value)"
        class="mdi mdi-check"
        aria-hidden="true"
      />
      <i
        v-else-if="option.icon"
        class="mdi"
        :class="`mdi-${option.icon}`"
        aria-hidden="true"
      />
      <span>{{ option.label }}</span>
    </button>
  </div>
</template>
