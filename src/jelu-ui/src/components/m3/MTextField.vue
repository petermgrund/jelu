<script setup lang="ts">
import { computed, useAttrs, useTemplateRef } from 'vue'
import { uniqueId } from './uid'
import { useI18n } from 'vue-i18n'

/**
 * M3 outlined text field. Attributes that are not props (name, min, max,
 * step, maxlength, autocomplete, list, inputmode, listeners...) are passed
 * to the native input, class and style stay on the field container.
 */
defineOptions({ inheritAttrs: false })

const props = withDefaults(defineProps<{
  label?: string,
  type?: string,
  placeholder?: string,
  supportingText?: string,
  error?: string | boolean,
  leadingIcon?: string,
  trailingIcon?: string,
  prefix?: string,
  suffix?: string,
  clearable?: boolean,
  multiline?: boolean,
  rows?: number,
  disabled?: boolean,
  readonly?: boolean,
  required?: boolean,
  id?: string,
}>(), {
  label: undefined,
  type: 'text',
  placeholder: undefined,
  supportingText: undefined,
  error: undefined,
  leadingIcon: undefined,
  trailingIcon: undefined,
  prefix: undefined,
  suffix: undefined,
  rows: 4,
  id: undefined,
})

const model = defineModel<string | number | null | undefined>()

const emit = defineEmits<{
  (e: 'clear'): void,
}>()

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const attrs = useAttrs()
const generatedId = uniqueId()
const inputId = computed(() => props.id ?? generatedId)
const supportingId = computed(() => `${inputId.value}-supporting`)
const input = useTemplateRef<HTMLInputElement | HTMLTextAreaElement>('input')

const rootAttrs = computed(() => ({ class: attrs.class, style: attrs.style }))
const inputAttrs = computed(() => {
  const rest: Record<string, unknown> = { ...attrs }
  delete rest.class
  delete rest.style
  return rest
})

const errorText = computed(() => typeof props.error === 'string' ? props.error : '')
const hasError = computed(() => props.error === true || errorText.value.length > 0)
const supporting = computed(() => errorText.value.length > 0 ? errorText.value : props.supportingText)
const hasValue = computed(() => model.value != null && `${model.value}`.length > 0)

const onInput = (event: Event) => {
  const target = event.target as HTMLInputElement | HTMLTextAreaElement
  if (props.type === 'number' && !props.multiline) {
    model.value = target.value === '' ? null : Number(target.value)
  } else {
    model.value = target.value
  }
}

const clear = () => {
  model.value = props.type === 'number' ? null : ''
  emit('clear')
  input.value?.focus()
}

defineExpose({
  focus: () => input.value?.focus(),
})
</script>

<template>
  <div
    class="m3-field"
    :class="[{ 'm3-field--error': hasError, 'm3-field--disabled': props.disabled }, rootAttrs.class]"
    :style="rootAttrs.style as any"
  >
    <fieldset class="m3-field__outline">
      <legend v-if="props.label">
        <label :for="inputId">{{ props.label }}<span
          v-if="props.required"
          aria-hidden="true"
        >&nbsp;*</span></label>
      </legend>
      <slot name="leading">
        <i
          v-if="props.leadingIcon"
          class="m3-field__icon mdi"
          :class="`mdi-${props.leadingIcon}`"
          aria-hidden="true"
        />
      </slot>
      <span
        v-if="props.prefix"
        class="m3-field__affix"
      >{{ props.prefix }}</span>
      <textarea
        v-if="props.multiline"
        :id="inputId"
        ref="input"
        class="m3-field__input"
        :value="model ?? ''"
        :rows="props.rows"
        :placeholder="props.placeholder"
        :disabled="props.disabled"
        :readonly="props.readonly"
        :required="props.required"
        :aria-invalid="hasError || undefined"
        :aria-describedby="supporting ? supportingId : undefined"
        v-bind="inputAttrs"
        @input="onInput"
      />
      <input
        v-else
        :id="inputId"
        ref="input"
        class="m3-field__input"
        :type="props.type"
        :value="model ?? ''"
        :placeholder="props.placeholder"
        :disabled="props.disabled"
        :readonly="props.readonly"
        :required="props.required"
        :aria-invalid="hasError || undefined"
        :aria-describedby="supporting ? supportingId : undefined"
        v-bind="inputAttrs"
        @input="onInput"
      >
      <span
        v-if="props.suffix"
        class="m3-field__affix"
      >{{ props.suffix }}</span>
      <button
        v-if="props.clearable && hasValue && !props.disabled && !props.readonly"
        v-tooltip="t('labels.clear')"
        type="button"
        class="m3-icon-btn"
        :aria-label="t('labels.clear')"
        @click="clear"
      >
        <i
          class="mdi mdi-close-circle-outline"
          aria-hidden="true"
        />
      </button>
      <slot name="trailing">
        <i
          v-if="props.trailingIcon"
          class="m3-field__icon mdi"
          :class="`mdi-${props.trailingIcon}`"
          aria-hidden="true"
        />
      </slot>
    </fieldset>
    <div
      v-if="supporting"
      :id="supportingId"
      class="m3-field__supporting"
    >
      {{ supporting }}
    </div>
  </div>
</template>
