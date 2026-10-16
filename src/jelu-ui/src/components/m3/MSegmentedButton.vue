<script setup lang="ts" generic="T">
import { MOption } from './types'

/**
 * M3 segmented button. Single select by default (v-model is a value),
 * `multiple` turns it into a multi select (v-model is an array).
 */
const props = defineProps<{
  options: Array<MOption<T>>,
  multiple?: boolean,
  block?: boolean,
  ariaLabel?: string,
}>()

// any: the model is a value or an array of values depending on the mode
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const model = defineModel<any>()

const isSelected = (value: T): boolean => {
  if (props.multiple) {
    return Array.isArray(model.value) && model.value.includes(value)
  }
  return model.value === value
}

const select = (value: T) => {
  if (props.multiple) {
    const current = Array.isArray(model.value) ? [...model.value] : []
    const index = current.indexOf(value)
    if (index >= 0) {
      current.splice(index, 1)
    } else {
      current.push(value)
    }
    model.value = current
  } else {
    model.value = value
  }
}
</script>

<template>
  <div
    class="m3-segmented"
    :class="{ 'm3-segmented--block': props.block }"
    :role="props.multiple ? 'group' : 'radiogroup'"
    :aria-label="props.ariaLabel"
  >
    <button
      v-for="option in props.options"
      :key="String(option.value)"
      type="button"
      class="m3-segmented__btn"
      :class="{ 'is-selected': isSelected(option.value) }"
      :role="props.multiple ? 'checkbox' : 'radio'"
      :aria-checked="isSelected(option.value)"
      :disabled="option.disabled"
      @click="select(option.value)"
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
      <span class="m3-segmented__label">{{ option.label }}</span>
    </button>
  </div>
</template>
