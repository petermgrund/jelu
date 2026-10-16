<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import MSegmentedButton from './m3/MSegmentedButton.vue'

/**
 * Filter that can be unset, false or true. The value is kept as the string
 * stored in the route query: "null", "false" or "true".
 */
const props = defineProps<{
  label: string,
}>()

const model = defineModel<string | null>()

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const options = computed(() => [
  { value: 'null', label: t('filtering.any') },
  { value: 'false', label: t('labels.no') },
  { value: 'true', label: t('labels.yes') },
])

const value = computed({
  get: () => model.value == null ? 'null' : model.value.toLowerCase(),
  set: (newValue) => {
    model.value = newValue
  },
})
</script>

<template>
  <section class="m3-sheet__section">
    <h3 class="m3-sheet__section-title">
      {{ props.label }}
    </h3>
    <m-segmented-button
      v-model="value"
      :options="options"
      :aria-label="props.label"
      block
    />
  </section>
</template>
