<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import useBookStatus, { BOOK_STATUSES, NO_STATUS, StatusFilter } from '../composables/bookStatus'
import MChipSet from './m3/MChipSet.vue'

/**
 * Single choice filter on the status of the books. The value is kept as the
 * route query string: ALL, a status, or NONE for books without status.
 */
const props = defineProps<{
  exclude?: Array<StatusFilter>,
}>()

const model = defineModel<string | null>()

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const { statusLabel, statusIcon } = useBookStatus()

const statuses: Array<StatusFilter> = [...BOOK_STATUSES, NO_STATUS]

const options = computed(() => [
  { value: 'ALL', label: t('filtering.any') },
  ...statuses
    .filter(status => !(props.exclude ?? []).includes(status))
    .map(status => ({ value: status as string, label: statusLabel(status), icon: statusIcon(status).replace('mdi-', '') })),
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
      {{ t('status.status') }}
    </h3>
    <m-chip-set
      v-model="value"
      :options="options"
      :aria-label="t('status.status')"
      single
    />
  </section>
</template>
