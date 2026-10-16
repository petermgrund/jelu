<script setup lang="ts">
import { useMagicKeys } from '@vueuse/core';
import { computed, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import MSegmentedButton from './m3/MSegmentedButton.vue';
import MSideSheet from './m3/MSideSheet.vue';

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const keys = useMagicKeys()
const shiftF = keys['Shift+F']

const props = defineProps<{
  order: string
  open: boolean
}>()

const emit = defineEmits<{
  (e: 'update:sortOrder', newval: string): void
  (e: 'update:open', open: boolean): void
}>()

const sortOrder = ref(props.order)

watch(sortOrder, (newVal, oldVal) => {
  if (newVal !== oldVal) {
      emit("update:sortOrder", newVal)
  }
})

watch(shiftF, (v) => {
  if (v) {
    emit('update:open', !props.open)
  }
})

const sheetOpen = computed({
  get: () => props.open,
  set: (value: boolean) => emit('update:open', value),
})

const orderOptions = computed(() => [
  { value: 'desc', label: t('sorting.descending'), icon: 'sort-descending' },
  { value: 'asc', label: t('sorting.ascending'), icon: 'sort-ascending' },
])
</script>

<template>
  <m-side-sheet
    v-model="sheetOpen"
    :headline="t('labels.sort_and_filter')"
  >
    <section class="m3-sheet__section">
      <h3 class="m3-sheet__section-title">
        {{ t('sorting.sort_order') }}
      </h3>
      <m-segmented-button
        v-model="sortOrder"
        :options="orderOptions"
        :aria-label="t('sorting.sort_order')"
        block
      />
    </section>
    <section
      v-if="$slots['sort-fields']"
      class="m3-sheet__section"
    >
      <h3 class="m3-sheet__section-title">
        {{ t('sorting.sort_by') }}
      </h3>
      <slot name="sort-fields" />
    </section>
    <slot name="filters" />
  </m-side-sheet>
</template>
