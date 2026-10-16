<script setup lang="ts">
import { useI18n } from 'vue-i18n'

/** M3 modal side sheet on the end edge (Oruga sidebar underneath). */
const props = defineProps<{
  headline: string,
}>()

const open = defineModel<boolean>({ default: false })

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})
</script>

<template>
  <o-sidebar
    v-model:active="open"
    position="right"
    :fullheight="true"
    :overlay="true"
    :teleport="true"
    :cancelable="['escape', 'outside']"
    @close="open = false"
  >
    <div class="m3-sheet">
      <div class="m3-sheet__header">
        <h2 class="m3-sheet__headline">
          {{ props.headline }}
        </h2>
        <button
          v-tooltip="t('labels.close')"
          type="button"
          class="m3-icon-btn"
          :aria-label="t('labels.close')"
          @click="open = false"
        >
          <i
            class="mdi mdi-close"
            aria-hidden="true"
          />
        </button>
      </div>
      <div class="m3-sheet__content">
        <slot />
      </div>
    </div>
  </o-sidebar>
</template>
