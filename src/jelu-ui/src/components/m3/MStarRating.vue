<script setup lang="ts">
import { computed, ref } from 'vue'

/**
 * Star rating, read only or editable (keyboard: arrows, home, end).
 * The value goes from 0 to `max` with `step` increments, one star per point.
 */
const props = withDefaults(defineProps<{
  max?: number,
  step?: number,
  readonly?: boolean,
  size?: 'sm' | 'md' | 'lg',
  showValue?: boolean,
  label?: string,
}>(), {
  max: 10,
  step: 0.5,
  size: 'md',
  label: undefined,
})

const model = defineModel<number | null | undefined>()

const hover = ref<number | null>(null)

const shown = computed(() => hover.value ?? model.value ?? 0)

const iconFor = (index: number) => {
  if (shown.value >= index) {
    return 'mdi-star'
  }
  if (shown.value >= index - 0.5) {
    return 'mdi-star-half-full'
  }
  return 'mdi-star-outline'
}

const clamp = (value: number) => Math.min(props.max, Math.max(0, Math.round(value / props.step) * props.step))

const valueAt = (event: MouseEvent, index: number) => {
  const target = event.currentTarget as HTMLElement
  const half = props.step <= 0.5 && event.offsetX < target.clientWidth / 2
  return clamp(half ? index - 0.5 : index)
}

const onMove = (event: MouseEvent, index: number) => {
  if (!props.readonly) {
    hover.value = valueAt(event, index)
  }
}

const onClick = (event: MouseEvent, index: number) => {
  if (!props.readonly) {
    model.value = valueAt(event, index)
  }
}

const onKey = (event: KeyboardEvent) => {
  if (props.readonly) {
    return
  }
  const current = model.value ?? 0
  const keys: Record<string, number> = {
    ArrowRight: current + props.step,
    ArrowUp: current + props.step,
    ArrowLeft: current - props.step,
    ArrowDown: current - props.step,
    Home: 0,
    End: props.max,
  }
  if (event.key in keys) {
    event.preventDefault()
    model.value = clamp(keys[event.key])
  }
}

const sizes: Record<string, string> = {
  sm: 'text-[1rem]',
  md: 'text-[1.5rem]',
  lg: 'text-[2rem]',
}
</script>

<template>
  <div class="inline-flex items-center gap-2">
    <div
      class="inline-flex items-center rounded-full"
      :class="[sizes[props.size], props.readonly ? '' : 'cursor-pointer']"
      :role="props.readonly ? 'img' : 'slider'"
      :tabindex="props.readonly ? undefined : 0"
      :aria-label="props.label ?? `${model ?? 0} / ${props.max}`"
      :aria-valuemin="props.readonly ? undefined : 0"
      :aria-valuemax="props.readonly ? undefined : props.max"
      :aria-valuenow="props.readonly ? undefined : (model ?? 0)"
      @keydown="onKey"
      @mouseleave="hover = null"
    >
      <i
        v-for="index in props.max"
        :key="index"
        class="mdi leading-none"
        :class="[iconFor(index), shown >= index - 0.5 ? 'text-primary' : 'text-outline']"
        aria-hidden="true"
        @mousemove="onMove($event, index)"
        @click="onClick($event, index)"
      />
    </div>
    <span
      v-if="props.showValue"
      class="text-label-large text-on-surface-variant tabular-nums"
    >{{ model ?? 0 }}/{{ props.max }}</span>
  </div>
</template>
