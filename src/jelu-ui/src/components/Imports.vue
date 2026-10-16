<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useStore } from 'vuex'
import { key } from '../store'
import { ImportSource } from "../model/ImportConfiguration";
import dataService from "../services/DataService";
import { useTitle } from '@vueuse/core'
import { useI18n } from 'vue-i18n'
import { ObjectUtils } from '../utils/ObjectUtils'
import { useOruga } from "@oruga-ui/oruga-next";
import MSegmentedButton from "./m3/MSegmentedButton.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const oruga = useOruga();

useTitle('Jelu | Imports')

const store = useStore(key)
const file = ref(new File([], "dummy"));
const importSource = ref(ImportSource.GOODREADS);
const fetchMetadata = ref(true)
const fetchCovers = ref(true)
const uploadPercentage = ref(0);
const errorMessage = ref("");
const exportErrorMessage = ref("");
const exportMessage = ref("");
const noFile = ref(true)

const handleFileUpload = (event: any) => {
  file.value = event.target.files[0];
  noFile.value = false
};

const importFile = async () => {
  if (file.value == null || file.value.name === 'dummy') {
    return
  }
  const importConfig = {
    shouldFetchMetadata: fetchMetadata.value,
    shouldFetchCovers: fetchCovers.value,
    importSource: importSource.value
    }
    if (importSource.value == ImportSource.ISBN_LIST) {
      importConfig.shouldFetchMetadata = true
    }
  dataService.importCsv(
    importConfig,
        file.value,
        (event: { loaded: number; total: number }) => {
          const percent = Math.round((100 * event.loaded) / event.total);
          console.log("percent " + percent);
          uploadPercentage.value = percent;
        })
        .then(res => {
          ObjectUtils.toast(oruga, "success", t('csv_import.import_ok'), 6000);
        })
        .catch(err => {
          console.log(err)
        })
}

const exportFile =async () => {
  console.log("export requested")
  try {
    await dataService.exportCsv()
    exportMessage.value = t('csv_import.export_ok')
  } catch (error) {
    exportErrorMessage.value = t('csv_import.export_ko')
  }
}

const fetchCoversDisabled = computed(() => {
  if (store != null && !store.getters.getMetadataFetchEnabled) {
    return true
  } else {
    return !fetchMetadata.value
  }
})

watch(file, (newVal, oldVal) => {
  console.log("file ")
  console.log(newVal)
  console.log(oldVal)
})

const sourceOptions = computed(() => [
  { value: ImportSource.GOODREADS, label: 'Goodreads', icon: 'alpha-g-circle-outline' },
  { value: ImportSource.ISBN_LIST, label: t('csv_import.isbn_list'), icon: 'barcode' },
])

</script>

<template>
  <div class="flex max-w-3xl flex-col gap-6">
    <section class="m3-card m3-card--outlined">
      <div class="m3-card__content gap-6 p-6">
        <div class="flex items-start gap-3">
          <i
            class="mdi mdi-file-import-outline text-[1.5rem] text-on-surface-variant"
            aria-hidden="true"
          />
          <div class="flex flex-1 flex-col gap-1">
            <h2 class="text-title-large">
              {{ t('csv_import.import_csv') }}
            </h2>
            <p class="text-body-medium text-on-surface-variant">
              {{ t('csv_import.import_help') }}
            </p>
          </div>
        </div>
        <div
          v-if="store != null && !store.getters.getMetadataFetchEnabled"
          class="flex items-center gap-3 rounded-md bg-tertiary-container p-4 text-body-medium text-on-tertiary-container"
          role="note"
        >
          <i
            class="mdi mdi-information-outline text-[1.25rem]"
            aria-hidden="true"
          />
          <span>{{ t('labels.auto_import_disabled') }}</span>
        </div>
        <div class="flex flex-col gap-3">
          <h3 class="text-title-small text-on-surface-variant">
            {{ t('csv_import.import_source') }}
          </h3>
          <m-segmented-button
            v-model="importSource"
            :options="sourceOptions"
            :aria-label="t('csv_import.import_source')"
            class="max-w-lg"
            block
          />
        </div>
        <div class="flex flex-col">
          <label
            v-if="importSource != ImportSource.ISBN_LIST"
            class="m3-choice m3-choice--between"
          >
            <span>{{ t('csv_import.auto_fetch_online') }}</span>
            <input
              v-model="fetchMetadata"
              type="checkbox"
              role="switch"
              class="m3-switch"
              :disabled="store != null && !store.getters.getMetadataFetchEnabled"
            >
          </label>
          <label class="m3-choice m3-choice--between">
            <span>{{ t('csv_import.fetch_covers') }}</span>
            <input
              v-model="fetchCovers"
              type="checkbox"
              role="switch"
              class="m3-switch"
              :disabled="fetchCoversDisabled"
            >
          </label>
        </div>
        <div class="flex flex-col gap-3">
          <h3 class="text-title-small text-on-surface-variant">
            {{ t('csv_import.choose_file') }}
          </h3>
          <div class="flex flex-wrap items-center gap-3">
            <label class="m3-btn m3-btn--outlined cursor-pointer focus-within:outline-3 focus-within:outline-secondary">
              <i
                class="mdi mdi-paperclip"
                aria-hidden="true"
              />
              <span>{{ t('labels.choose_file') }}</span>
              <input
                id="file"
                type="file"
                accept=".csv,.tsv,.txt"
                class="sr-only"
                @change="handleFileUpload($event)"
              >
            </label>
            <span class="min-w-0 truncate text-body-medium text-on-surface-variant">
              {{ noFile ? t('labels.no_file_selected') : file.name }}
            </span>
          </div>
          <progress
            v-if="uploadPercentage > 0"
            max="100"
            :value.prop="uploadPercentage"
            class="m3-linear-progress"
          />
        </div>
        <p
          v-if="errorMessage"
          class="text-body-medium text-error"
        >
          {{ errorMessage }}
        </p>
        <div class="flex justify-end">
          <button
            type="button"
            class="m3-btn m3-btn--filled"
            :disabled="noFile"
            @click="importFile"
          >
            <i
              class="mdi mdi-upload"
              aria-hidden="true"
            />
            <span>{{ t('csv_import.import_file') }}</span>
          </button>
        </div>
      </div>
    </section>

    <section class="m3-card m3-card--outlined">
      <div class="m3-card__content gap-6 p-6">
        <div class="flex items-start gap-3">
          <i
            class="mdi mdi-file-export-outline text-[1.5rem] text-on-surface-variant"
            aria-hidden="true"
          />
          <div class="flex flex-1 flex-col gap-1">
            <h2 class="text-title-large">
              {{ t('csv_import.export') }}
            </h2>
            <p class="text-body-medium text-on-surface-variant">
              {{ t('csv_import.export_message') }}
            </p>
          </div>
        </div>
        <p
          v-if="exportMessage"
          class="flex items-center gap-3 rounded-md bg-secondary-container p-4 text-body-medium text-on-secondary-container"
          role="status"
        >
          <i
            class="mdi mdi-check-circle-outline text-[1.25rem]"
            aria-hidden="true"
          />
          <span>{{ exportMessage }}</span>
        </p>
        <p
          v-if="exportErrorMessage"
          class="flex items-center gap-3 rounded-md bg-error-container p-4 text-body-medium text-on-error-container"
          role="alert"
        >
          <i
            class="mdi mdi-alert-circle-outline text-[1.25rem]"
            aria-hidden="true"
          />
          <span>{{ exportErrorMessage }}</span>
        </p>
        <div class="flex justify-end">
          <button
            type="button"
            class="m3-btn m3-btn--tonal"
            @click="exportFile"
          >
            <i
              class="mdi mdi-download"
              aria-hidden="true"
            />
            <span>{{ t('csv_import.export_file') }}</span>
          </button>
        </div>
      </div>
    </section>
  </div>
</template>
