<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import useOwnership, { OWNERSHIP_STATUSES } from '../composables/ownership'
import MChipSet from './m3/MChipSet.vue'

/**
 * Single choice filter on the ownership of the books. The value is kept as the
 * route query string: ALL or an ownership state.
 */
const model = defineModel<string | null>()

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const { ownershipLabel, ownershipIcon } = useOwnership()

const options = computed(() => [
  { value: 'ALL', label: t('filtering.any') },
  ...OWNERSHIP_STATUSES.map(status => ({ value: status as string, label: ownershipLabel(status), icon: ownershipIcon(status).replace('mdi-', '') })),
])

const value = computed({
  get: () => model.value ?? 'ALL',
  set: (newValue: string) => {
    model.value = newValue
  },
})
</script>

<template>
  <section class="m3-sheet__section">
    <h3 class="m3-sheet__section-title">
      {{ t('ownership.ownership') }}
    </h3>
    <m-chip-set
      v-model="value"
      :options="options"
      :aria-label="t('ownership.ownership')"
      single
    />
  </section>
</template>
