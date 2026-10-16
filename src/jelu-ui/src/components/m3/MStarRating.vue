<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MAX_STARS } from '../../utils/rating'

/**
 * Star rating, read only or editable (keyboard: arrows, home, end).
 * The model is the stored rating, from 0 to `scale` (10), shown as `stars`
 * stars (5) with half stars, so every stored point is half a star.
 */
const props = withDefaults(defineProps<{
  stars?: number,
  scale?: number,
  readonly?: boolean,
  size?: 'sm' | 'md' | 'lg',
  showValue?: boolean,
  label?: string,
}>(), {
  stars: MAX_STARS,
  scale: 10,
  size: 'md',
  label: undefined,
})

const model = defineModel<number | null | undefined>()

const { locale } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const STEP = 0.5

const hover = ref<number | null>(null)

const value = computed(() => model.value == null ? 0 : model.value * props.stars / props.scale)

const shown = computed(() => hover.value ?? value.value)

const formatted = (stars: number) => new Intl.NumberFormat(locale.value, { maximumFractionDigits: 1 }).format(stars)

const iconFor = (index: number) => {
  if (shown.value >= index) {
    return 'mdi-star'
  }
  if (shown.value >= index - 0.5) {
    return 'mdi-star-half-full'
  }
  return 'mdi-star-outline'
}

const clamp = (stars: number) => Math.min(props.stars, Math.max(0, Math.round(stars / STEP) * STEP))

const setStars = (stars: number) => {
  model.value = clamp(stars) * props.scale / props.stars
}

const valueAt = (event: MouseEvent, index: number) => {
  const target = event.currentTarget as HTMLElement
  const half = event.offsetX < target.clientWidth / 2
  return clamp(half ? index - 0.5 : index)
}

const onMove = (event: MouseEvent, index: number) => {
  if (!props.readonly) {
    hover.value = valueAt(event, index)
  }
}

const onClick = (event: MouseEvent, index: number) => {
  if (!props.readonly) {
    setStars(valueAt(event, index))
  }
}

const onKey = (event: KeyboardEvent) => {
  if (props.readonly) {
    return
  }
  const keys: Record<string, number> = {
    ArrowRight: value.value + STEP,
    ArrowUp: value.value + STEP,
    ArrowLeft: value.value - STEP,
    ArrowDown: value.value - STEP,
    Home: 0,
    End: props.stars,
  }
  if (event.key in keys) {
    event.preventDefault()
    setStars(keys[event.key])
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
      :aria-label="props.label ?? `${formatted(value)} / ${props.stars}`"
      :aria-valuemin="props.readonly ? undefined : 0"
      :aria-valuemax="props.readonly ? undefined : props.stars"
      :aria-valuenow="props.readonly ? undefined : value"
      :aria-valuetext="props.readonly ? undefined : `${formatted(value)} / ${props.stars}`"
      @keydown="onKey"
      @mouseleave="hover = null"
    >
      <i
        v-for="index in props.stars"
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
    >{{ formatted(shown) }}/{{ props.stars }}</span>
  </div>
</template>
