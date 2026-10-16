<script setup lang="ts">
import { computed } from 'vue'

/**
 * M3 outlined field container for controls that render their own input,
 * ie Oruga taginput, autocomplete and datepicker.
 */
const props = defineProps<{
  label?: string,
  supportingText?: string,
  error?: string | boolean,
  leadingIcon?: string,
  disabled?: boolean,
}>()

const errorText = computed(() => typeof props.error === 'string' ? props.error : '')
const hasError = computed(() => props.error === true || errorText.value.length > 0)
const supporting = computed(() => errorText.value.length > 0 ? errorText.value : props.supportingText)
</script>

<template>
  <div
    class="m3-field"
    :class="{ 'm3-field--error': hasError, 'm3-field--disabled': props.disabled }"
  >
    <fieldset
      class="m3-field__outline"
      :disabled="props.disabled"
    >
      <legend v-if="props.label">
        {{ props.label }}
      </legend>
      <i
        v-if="props.leadingIcon"
        class="m3-field__icon mdi"
        :class="`mdi-${props.leadingIcon}`"
        aria-hidden="true"
      />
      <slot />
      <slot name="trailing" />
    </fieldset>
    <div
      v-if="supporting"
      class="m3-field__supporting"
    >
      {{ supporting }}
    </div>
  </div>
</template>
