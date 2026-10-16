<script setup lang="ts" generic="T">
import { computed, useAttrs, useId } from 'vue'
import { MOption } from './types'

/**
 * M3 outlined select (exposed dropdown menu) on a native <select>.
 * Pass `options`, or <option> elements in the default slot.
 */
defineOptions({ inheritAttrs: false })

const props = defineProps<{
  label?: string,
  options?: Array<MOption<T>>,
  placeholder?: string,
  supportingText?: string,
  error?: string | boolean,
  leadingIcon?: string,
  disabled?: boolean,
  id?: string,
}>()

const model = defineModel<T | null | undefined>()

const attrs = useAttrs()
const generatedId = useId()
const selectId = computed(() => props.id ?? generatedId)

const rootAttrs = computed(() => ({ class: attrs.class, style: attrs.style }))
const selectAttrs = computed(() => {
  const rest: Record<string, unknown> = { ...attrs }
  delete rest.class
  delete rest.style
  return rest
})

const errorText = computed(() => typeof props.error === 'string' ? props.error : '')
const hasError = computed(() => props.error === true || errorText.value.length > 0)
const supporting = computed(() => errorText.value.length > 0 ? errorText.value : props.supportingText)
</script>

<template>
  <div
    class="m3-field"
    :class="[{ 'm3-field--error': hasError, 'm3-field--disabled': props.disabled }, rootAttrs.class]"
    :style="rootAttrs.style as any"
  >
    <fieldset class="m3-field__outline">
      <legend v-if="props.label">
        <label :for="selectId">{{ props.label }}</label>
      </legend>
      <i
        v-if="props.leadingIcon"
        class="m3-field__icon mdi"
        :class="`mdi-${props.leadingIcon}`"
        aria-hidden="true"
      />
      <select
        :id="selectId"
        v-model="model"
        class="m3-field__input"
        :disabled="props.disabled"
        :aria-invalid="hasError || undefined"
        v-bind="selectAttrs"
      >
        <option
          v-if="props.placeholder"
          disabled
          :value="null"
        >
          {{ props.placeholder }}
        </option>
        <slot>
          <option
            v-for="option in props.options"
            :key="String(option.value)"
            :value="option.value"
            :disabled="option.disabled"
          >
            {{ option.label }}
          </option>
        </slot>
      </select>
      <i
        class="m3-field__icon m3-field__select-arrow mdi mdi-menu-down"
        aria-hidden="true"
      />
    </fieldset>
    <div
      v-if="supporting"
      class="m3-field__supporting"
    >
      {{ supporting }}
    </div>
  </div>
</template>
