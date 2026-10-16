<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next";
import { ComputedRef, Ref, computed, reactive, ref } from "vue";
import { useI18n } from 'vue-i18n';
import { useStore } from 'vuex';
import { Book } from "../model/Book";
import { Metadata } from "../model/Metadata";
import { PluginInfo } from "../model/PluginInfo";
import { ServerSettings } from "../model/ServerSettings";
import dataService from "../services/DataService";
import { key } from '../store';
import { StringUtils } from "../utils/StringUtils";
import MetadataDetail from "./MetadataDetail.vue";
import MetadataPluginsModal from "./MetadataPluginsModal.vue";
import ScanModal from "./ScanModal.vue";
import MDialog from "./m3/MDialog.vue";
import MTextField from "./m3/MTextField.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })
const store = useStore(key)

const oruga = useOruga();

const props = defineProps<{
  book: Book|undefined,
}>()

const form = reactive({
  title: props.book?.title,
  isbn: props.book?.isbn10?.length != undefined && props.book?.isbn10?.length > 0 ? props.book?.isbn10 : props.book?.isbn13,
  authors: props.book?.authors?.map(a => a.name).join(','),
});

const serverSettings: ComputedRef<ServerSettings> = computed(() => {
  return store != undefined && store.getters.getSettings
})

const emit = defineEmits(['close', 'metadataReceived']);

const metadata: Ref<Metadata|null> = ref(null)

const errorMessage = ref("");

const displayForm: Ref<boolean> = ref(true)

const progress: Ref<boolean> = ref(false)

let plugins: Array<PluginInfo> = []

const fetchMetadata = async () => {
    console.log("fetch metadata")
    progress.value = true
    dataService.fetchMetadataWithPlugins({isbn: form.isbn, title: form.title, authors: form.authors, plugins: plugins})
    .then(res => {
        console.log(res)
        progress.value = false
        metadata.value = res
        displayForm.value = false
        }
    )
}

const discard = () => {
    displayForm.value = true
    metadata.value = null
}

const importData = () => {
    emit('metadataReceived', metadata.value)
    emit('close')
}

const isValid = computed(() => StringUtils.isNotBlank(form.title)
|| StringUtils.isNotBlank(form.isbn)
|| StringUtils.isNotBlank(form.authors))

const formatErrorMessage = (message: string) => {
  return message.replace(/\n/g, '<br>') || ''
}

let barcodeReader: any = null

function toggleScanModal() {
    oruga.modal.open({
      component: ScanModal,
      trapFocus: true,
      active: true,
      canCancel: ['x', 'button', 'outside'],
      scroll: 'keep',
      props: {
      },
      events: {
        decoded: (barcode: string|null) => {
          console.log("barcode " + barcode)
          if (barcode != null) {
            form.isbn = barcode
          }
      },
      barcodeLoaded: (reader: any) => {
        barcodeReader = reader
      }
    },
      onClose: scanModalClosed
    });
}

function togglePluginsModal() {
    oruga.modal.open({
      component: MetadataPluginsModal,
      trapFocus: true,
      active: true,
      canCancel: ['x', 'button', 'outside'],
      scroll: 'keep',
      props: {
      },
      events: {
        plugins: (received: Array<PluginInfo>) => {
          console.log("plugins ")
          console.log(received)
          plugins = received
      }
    },
      onClose: pluginsModalClosed
    });
}

function scanModalClosed() {
  console.log("scan modal closed")
}

function pluginsModalClosed() {
  console.log("plugins modal closed")
}
</script>

<template>
  <m-dialog
    :headline="t('labels.import_book')"
    size="md"
    @close="emit('close')"
  >
    <div
      v-if="displayForm"
      class="flex flex-col gap-4"
    >
      <p class="text-body-medium">
        {{ t('labels.auto_fill_doc') }}
      </p>
      <m-text-field
        v-model="form.isbn"
        :label="t('book.isbn')"
        @keyup.enter="fetchMetadata"
      >
        <template #trailing>
          <button
            v-tooltip="t('labels.scan_isbn')"
            type="button"
            class="m3-icon-btn"
            :aria-label="t('labels.scan_isbn')"
            :disabled="progress"
            @click="toggleScanModal"
          >
            <i
              class="mdi mdi-barcode-scan"
              aria-hidden="true"
            />
          </button>
        </template>
      </m-text-field>
      <m-text-field
        v-model="form.title"
        :label="t('book.title')"
        @keyup.enter="fetchMetadata"
      />
      <m-text-field
        v-model="form.authors"
        :label="t('book.author', 2)"
        @keyup.enter="fetchMetadata"
      />
      <div v-if="serverSettings.metadataFetchEnabled && serverSettings.metadataPlugins.length > 1">
        <button
          type="button"
          class="m3-btn m3-btn--text"
          :disabled="progress"
          @click="togglePluginsModal"
        >
          <i
            class="mdi mdi-sort-variant"
            aria-hidden="true"
          />
          <span>{{ t('metadata.reorder_plugins') }}</span>
        </button>
      </div>
      <p
        v-if="errorMessage"
        class="text-body-small text-error"
      >
        {{ errorMessage }}
      </p>
      <progress
        v-if="progress"
        class="m3-linear-progress"
      />
    </div>
    <div
      v-else
      class="flex flex-col gap-4"
    >
      <template v-if="metadata != null && metadata.errors != null && metadata.errors.length > 0">
        <div
          v-for="error in metadata.errors"
          :key="error.sourcePlugin"
          class="flex flex-col gap-2 rounded-md bg-error-container p-4 text-body-medium text-on-error-container"
          role="alert"
        >
          <p class="flex items-center gap-3">
            <i
              class="mdi mdi-alert-circle-outline text-[1.25rem]"
              aria-hidden="true"
            />
            <span>{{ t('errors.metadata.' + error.errorType) }}</span>
          </p>
          <details
            v-if="error.pluginErrorMessage"
            class="pl-8"
          >
            <summary class="cursor-pointer text-label-large">
              {{ t('errors.details') }}
            </summary>
            <p
              class="mt-2 max-h-48 overflow-y-auto font-mono text-body-small break-words whitespace-pre-line"
              v-html="formatErrorMessage(error.pluginErrorMessage)"
            />
          </details>
        </div>
      </template>
      <metadata-detail
        v-else-if="metadata != null"
        :metadata="metadata"
      />
    </div>
    <template #actions>
      <template v-if="displayForm">
        <button
          type="button"
          class="m3-btn m3-btn--text"
          @click="emit('close')"
        >
          {{ t('labels.cancel') }}
        </button>
        <button
          type="button"
          class="m3-btn m3-btn--filled"
          :disabled="!isValid || progress"
          @click="fetchMetadata"
        >
          <span
            v-if="progress"
            class="m3-spinner"
          />
          <i
            v-else
            class="mdi mdi-cloud-download-outline"
            aria-hidden="true"
          />
          <span>{{ t('labels.fetch_book') }}</span>
        </button>
      </template>
      <template v-else>
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
    </template>
  </m-dialog>
</template>
