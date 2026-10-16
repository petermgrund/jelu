<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from 'vue-i18n';
import { Path } from "../model/DirectoryListing";

const props = defineProps<{
  elem: Path
}>()

const emit = defineEmits<{
  (e: 'choose', targetElement: Path): void
}>();

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const isDirectory = computed(() => props.elem.type == 'directory')

const icon = computed(() => {
  if (isDirectory.value) {
    return 'mdi-folder-outline'
  }
  return /\.(avif|bmp|gif|ico|jpe?g|png|svg|tiff?|webp)$/i.test(props.elem.name) ? 'mdi-file-image-outline' : 'mdi-file-document-outline'
})

const actionLabel = computed(() => isDirectory.value ? t('labels.open_folder') : t('labels.choose_file'))
</script>

<template>
  <li
    class="m3-list-item m3-list-item--interactive"
    @click="emit('choose', props.elem)"
  >
    <span class="m3-list-item__leading">
      <i
        class="mdi"
        :class="icon"
        aria-hidden="true"
      />
    </span>
    <span class="m3-list-item__content">
      <span class="m3-list-item__headline">{{ props.elem.name }}</span>
    </span>
    <span class="m3-list-item__trailing">
      <button
        v-tooltip="actionLabel"
        type="button"
        class="m3-icon-btn"
        :aria-label="`${actionLabel} ${props.elem.name}`"
        @click.stop="emit('choose', props.elem)"
      >
        <i
          class="mdi"
          :class="isDirectory ? 'mdi-chevron-right' : 'mdi-check'"
          aria-hidden="true"
        />
      </button>
    </span>
  </li>
</template>
