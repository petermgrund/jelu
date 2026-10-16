<script setup lang="ts">
import { useI18n } from 'vue-i18n'

/** M3 input chip with a remove button, used for selected tags and authors. */
const props = defineProps<{
  content: string
}>();

const emit = defineEmits<{
  (e: 'closed'): void,
}>()

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})
</script>

<template>
  <span class="m3-chip m3-chip--input">
    <span>{{ props.content }}</span>
    <button
      type="button"
      class="m3-chip__remove"
      :aria-label="`${t('labels.delete')} ${props.content}`"
      @click.stop="emit('closed')"
    >
      <i
        class="mdi mdi-close"
        aria-hidden="true"
      />
    </button>
  </span>
</template>
