<script setup lang="ts">
import { useI18n } from 'vue-i18n'

/** Contextual bar shown while books are being selected for a bulk edit. */
const props = defineProps<{
  count: number,
}>()

const active = defineModel<boolean>('active', { default: false })
const selectAll = defineModel<boolean>('selectAll', { default: false })

const emit = defineEmits<{
  (e: 'edit'): void,
}>()

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})
</script>

<template>
  <Transition name="jl-fade">
    <div
      v-if="active"
      class="sticky top-[4.5rem] z-20 mb-4 flex flex-wrap items-center gap-1 rounded-xl bg-secondary-container py-1 pr-2 pl-1 text-on-secondary-container shadow-elevation-2"
      role="toolbar"
      :aria-label="t('bulk.toggle')"
    >
      <button
        v-tooltip="t('labels.close')"
        type="button"
        class="m3-icon-btn"
        :aria-label="t('labels.close')"
        @click="active = false"
      >
        <i
          class="mdi mdi-close"
          aria-hidden="true"
        />
      </button>
      <span class="flex-1 px-1 text-title-small">{{ t('labels.selected_count', { count: props.count }) }}</span>
      <button
        type="button"
        class="m3-btn m3-btn--text"
        @click="selectAll = !selectAll"
      >
        <i
          class="mdi mdi-checkbox-multiple-marked-outline"
          aria-hidden="true"
        />
        <span>{{ t('bulk.select_all') }}</span>
      </button>
      <button
        type="button"
        class="m3-btn m3-btn--filled"
        :disabled="props.count < 1"
        @click="emit('edit')"
      >
        <i
          class="mdi mdi-pencil-outline"
          aria-hidden="true"
        />
        <span>{{ t('bulk.edit') }}</span>
      </button>
    </div>
  </Transition>
</template>
