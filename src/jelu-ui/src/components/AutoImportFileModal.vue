<script setup lang="ts">
import { computed, Ref, ref } from "vue";
import { useI18n } from 'vue-i18n';
import { DirectoryListing, Path } from "../model/DirectoryListing";
import { Metadata } from "../model/Metadata";
import dataService from "../services/DataService";
import MetadataDetail from "./MetadataDetail.vue";
import FilePickerElement from "./FilePickerElement.vue";
import MDialog from "./m3/MDialog.vue";
import MRadioGroup from "./m3/MRadioGroup.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const file: Ref<File|null> = ref(null);
const uploadPercentage = ref(0);

const handleFileUpload = (event: any) => {
  file.value = event.target.files[0];
  dataService.getMetadataFromUploadedFile(file.value,
  (event: { loaded: number; total: number }) => {
          let percent = Math.round((100 * event.loaded) / event.total);
          uploadPercentage.value = percent;
        }).then(res => {
          metadata.value = res
          displayMetadata.value = true
        })
        .catch(e => console.log(e))
};

const fromServer = ref(false);

const directoryListing:Ref<DirectoryListing|null> = ref(null)

const directories = (root: string|undefined) => {
  if (root != null) {
    dataService.getDirectoryListing(root)
    .then(res => {
      directoryListing.value = res
    })
    .catch(err => console.log(err))
  }
}

const emit = defineEmits(['close', 'metadataReceived']);

const displayMetadata: Ref<boolean> = ref(false)
const metadata: Ref<Metadata|null> = ref(null)

const displayForm: Ref<boolean> = ref(true)

const discard = () => {
    displayForm.value = true
    metadata.value = null
    displayMetadata.value = false
}

const importData = () => {
    emit('metadataReceived', metadata.value)
    emit('close')
}

const selectPath = (elem: Path) => {
  if (elem.type == 'directory') {
    directories(elem.path)
  } else {
    dataService.getMetadataFromFile(elem.path)
      .then(res => {
        metadata.value = res
        displayMetadata.value = true
      })
      .catch(err => console.log(err))
  }
}

const sourceOptions = computed(() => [
  { value: false, label: t('labels.upload_from_computer') },
  { value: true, label: t('labels.upload_from_server') },
])

directories('/')

</script>

<template>
  <m-dialog
    :headline="t('labels.import_book')"
    size="md"
    @close="emit('close')"
  >
    <metadata-detail
      v-if="displayMetadata && metadata != null"
      :metadata="metadata"
    />
    <div
      v-else
      class="flex flex-col gap-4"
    >
      <p class="text-body-medium">
        {{ t('labels.auto_fill_book') }}
      </p>
      <m-radio-group
        v-model="fromServer"
        :options="sourceOptions"
        :label="t('csv_import.import_source')"
      />
      <ul
        v-if="fromServer"
        class="m3-list -mx-6 border-t border-outline-variant"
      >
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
        <file-picker-element
          v-for="elem of directoryListing?.directories"
          :key="elem.path"
          :elem="elem"
          class="pl-6"
          @choose="selectPath"
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
      <div
        v-else
        class="flex flex-col gap-3"
      >
        <div class="flex flex-wrap items-center gap-3">
          <label class="m3-btn m3-btn--outlined cursor-pointer focus-within:outline-3 focus-within:outline-secondary">
            <i
              class="mdi mdi-paperclip"
              aria-hidden="true"
            />
            <span>{{ t('labels.choose_file') }}</span>
            <input
              type="file"
              accept="image/*,.opf,.epub,.OPF,.EPUB"
              class="sr-only"
              @change="handleFileUpload($event)"
            >
          </label>
          <span class="min-w-0 truncate text-body-medium text-on-surface-variant">
            {{ file != null ? file.name : t('labels.no_file_selected') }}
          </span>
        </div>
        <progress
          v-if="uploadPercentage > 0"
          max="100"
          :value.prop="uploadPercentage"
          class="m3-linear-progress"
        />
      </div>
    </div>
    <template #actions>
      <template v-if="displayMetadata && metadata != null">
        <button
          type="button"
          class="m3-btn m3-btn--text"
          @click="discard"
        >
          {{ t('labels.discard') }}
        </button>
        <button
          type="button"
          class="m3-btn m3-btn--filled"
          @click="importData"
        >
          <i
            class="mdi mdi-check"
            aria-hidden="true"
          />
          <span>{{ t('labels.import') }}</span>
        </button>
      </template>
      <button
        v-else
        type="button"
        class="m3-btn m3-btn--text"
        @click="emit('close')"
      >
        {{ t('labels.cancel') }}
      </button>
    </template>
  </m-dialog>
</template>
