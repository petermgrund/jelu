<script setup lang="ts" generic="T">
import { useId } from 'vue'
import { MOption } from './types'

/** Vertical list of M3 radio buttons. */
const props = defineProps<{
  options: Array<MOption<T>>,
  label?: string,
  name?: string,
}>()

const model = defineModel<T | null | undefined>()

const groupName = props.name ?? useId()
</script>

<template>
  <div
    class="flex flex-col"
    role="radiogroup"
    :aria-label="props.label"
  >
    <label
      v-for="option in props.options"
      :key="String(option.value)"
      class="m3-choice"
    >
      <input
        v-model="model"
        type="radio"
        class="m3-radio"
        :name="groupName"
        :value="option.value"
        :disabled="option.disabled"
      >
      <span>{{ option.label }}</span>
    </label>
  </div>
</template>
