<script setup lang="ts">
import { Ref, ref } from "vue";
import { useI18n } from 'vue-i18n';
import { DirectoryListing, Path } from "../model/DirectoryListing";
import dataService from "../services/DataService";
import FilePickerElement from "./FilePickerElement.vue";
import MDialog from "./m3/MDialog.vue";
import MTextField from "./m3/MTextField.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const directoryListing:Ref<DirectoryListing|null> = ref(null)
const pattern = ref("")

const directories = (root: string|undefined) => {
  if (root != null) {
    dataService.getDirectoryListing(root, 'pictures')
    .then(res => {
      directoryListing.value = res
    })
    .catch(err => console.log(err))
  }
}

const selectPath = (elem: Path) => {
  if (elem.type == 'directory') {
    directories(elem.path)
  } else {
    emit('choose', elem)
    emit('close')
  }
}

const emit = defineEmits<{
  (e: 'choose', targetElement: Path): void,
  (e: 'close'): void
}>();

directories('/')

</script>

<template>
  <m-dialog
    :headline="t('labels.choose_file')"
    size="md"
    @close="emit('close')"
  >
    <div class="sticky -top-4 z-10 -mt-4 bg-surface-container-high pt-4 pb-2">
      <m-text-field
        id="image-picker-filter"
        v-model="pattern"
        :label="t('labels.filter_pattern')"
        leading-icon="filter-outline"
        clearable
      />
    </div>
    <ul class="m3-list -mx-6 min-h-[min(24rem,50dvh)]">
      <li v-if="directoryListing?.parent != null">
        <button
          type="button"
          class="m3-list-item pl-6"
          @click="directories(directoryListing?.parent)"
        >
          <span class="m3-list-item__leading">
            <i
              class="mdi mdi-arrow-up"
              aria-hidden="true"
            />
          </span>
          <span class="m3-list-item__content">
            <span class="m3-list-item__headline">{{ t('labels.back') }}</span>
            <span
              v-if="directoryListing.parent"
              class="m3-list-item__supporting truncate"
            >{{ directoryListing.parent }}</span>
          </span>
        </button>
      </li>
      <FilePickerElement
        v-for="elem of directoryListing?.directories.filter(d => {
          if (pattern != null && pattern.trim().length > 0) {
            return d.name.toLowerCase().includes(pattern.toLowerCase())
          }
          return d
        })"
        :key="elem.path"
        :elem="elem"
        class="pl-6"
        @choose="elem => selectPath(elem)"
      />
      <li
        v-if="directoryListing?.directories &&
          directoryListing?.directories.length < 1"
        class="m3-list-item pl-6"
      >
        <span class="m3-list-item__leading">
          <i
            class="mdi mdi-folder-off-outline"
            aria-hidden="true"
          />
        </span>
        <span class="m3-list-item__content">
          <span class="m3-list-item__supporting">{{ t('labels.no_supported_file') }}</span>
        </span>
      </li>
    </ul>
    <template #actions>
      <button
        type="button"
        class="m3-btn m3-btn--text"
        @click="emit('close')"
      >
        {{ t('labels.cancel') }}
      </button>
    </template>
  </m-dialog>
</template>
