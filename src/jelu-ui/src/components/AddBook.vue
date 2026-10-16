<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next";
import IsbnVerify from '@w0s/isbn-verify';
import { useTitle } from '@vueuse/core';
import { computed, reactive, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { Author } from "../model/Author";
import { Wrapper } from "../model/autocomplete-wrapper";
import { UserBook } from "../model/Book";
import { Path } from "../model/DirectoryListing";
import { Metadata } from "../model/Metadata";
import { SeriesOrder } from "../model/Series";
import { Tag } from "../model/Tag";
import dataService from "../services/DataService";
import { key } from '../store';
import { ObjectUtils } from "../utils/ObjectUtils";
import { StringUtils } from "../utils/StringUtils";
import AutoImportFileModalVue from "./AutoImportFileModal.vue";
import AutoImportFormModalVue from "./AutoImportFormModal.vue";
import ImagePickerModal from "./ImagePickerModal.vue";
import SeriesCompleteInput from "./SeriesCompleteInput.vue";
import ClosableBadge from "./ClosableBadge.vue";
import { Role } from "../model/Role";
import { ReadingEventType } from "../model/ReadingEvent";
import BookCover from "./BookCover.vue";
import MField from "./m3/MField.vue";
import MPageHeader from "./m3/MPageHeader.vue";
import MRadioGroup from "./m3/MRadioGroup.vue";
import MSegmentedButton from "./m3/MSegmentedButton.vue";
import MSlider from "./m3/MSlider.vue";
import MTextField from "./m3/MTextField.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

useTitle('Jelu | ' + t('nav.add_book'))

const store = useStore(key)
const router = useRouter()
const oruga = useOruga()

const datepicker = ref(null);
const publishedDate: Ref<Date | null> = ref(null)
const progress: Ref<boolean> = ref(false)
const form = reactive({
  title: "",
  summary: "",
  isbn10: "",
  isbn13: "",
  publisher: "",
  pageCount: null,
  personalNotes: "",
  owned: null,
  borrowed: null,
  toRead: null,
  percentRead: 0,
  currentPageNumber: null,
  googleId: "",
  amazonId: "",
  goodreadsId: "",
  librarythingId: "",
  isfdbId: "",
  openlibraryId: "",
  noosfereId: "",
  inventaireId: "",
  language: "",
  originalTitle: "",
  price: null
});
const eventType = ref(null);
const eventDate: Ref<Date|null> = ref(new Date());
const imageUrl = ref<string | null>(null);
const imagePath = ref<string | null>(null);
const file = ref<File | null>(null);
const uploadType = ref('web');

const uploadPercentage = ref(0);
const errorMessage = ref("");
const toReadDisplay = computed(() => {
  if (form.toRead) {
    return t('labels.book_will_be_added')
  }
  return ""
})

watch(() => [form.currentPageNumber, form.percentRead, form.pageCount],(newVal, oldVal) => {
  if (form.pageCount != null) {
    ObjectUtils.computePages(newVal, oldVal, form, form.pageCount)
  }
})

const filteredAuthors: Ref<Array<Wrapper>> = ref([]);
const authors: Ref<Array<Author>> = ref([]);

const filteredTags: Ref<Array<Wrapper>> = ref([]);
const tags: Ref<Array<Tag>> = ref([]);

const translators: Ref<Array<Author>> = ref([]);
const filteredTranslators: Ref<Array<Wrapper>> = ref([]);

const narrators: Ref<Array<Author>> = ref([]);
const filteredNarrators: Ref<Array<Wrapper>> = ref([]);

const filteredPublishers: Ref<Array<string>> = ref([])

const seriesCopy: Ref<Array<SeriesOrder>> = ref([])

const showModal: Ref<boolean> = ref(false)
const metadata: Ref<Metadata | null> = ref(null)

const showImagePickerModal: Ref<boolean> = ref(false)

const hasImage = computed(() => {
  return StringUtils.isNotBlank(metadata.value?.image)
})
const deleteImage: Ref<boolean> = ref(false)

function toggleRemoveImage() {
  deleteImage.value = !deleteImage.value
}

const importBook = async () => {
  console.log("import book");
  if (StringUtils.isNotBlank(form.title)) {
    const alreadyExisting = await dataService.checkIsbnExists(form.isbn10, form.isbn13)
    console.log('already existing')
    console.log(alreadyExisting)
    let saveBook = true
    if (alreadyExisting != null) {
      saveBook = false
      await ObjectUtils.swalYesNoMixin.fire({
        html: `<p>${t('labels.book_with_same_isbn_already_exists')}:<br>${alreadyExisting.title}<br>${t('labels.save_new_anyway')}</p>`,
        showDenyButton: false,
        confirmButtonText: t('labels.save'),
        cancelButtonText: t('labels.dont_save')
      }).then((result) => {
        if (result.isConfirmed) {
          saveBook = true
        } else if (result.isDenied) {
          ObjectUtils.baseSwalMixin.fire('', t('labels.changes_not_saved'), 'info')
        }
      })
    }
    console.log(`save book ${saveBook}`)
    if (!saveBook) {
      return
    }
    const userBook: UserBook = fillBook(form, publishedDate.value)
    authors.value.forEach((a) => {
        userBook.book.authors?.push(a)
    });
    tags.value.forEach((t) => userBook.book.tags?.push(t));
    translators.value.forEach((tr) => userBook.book.translators?.push(tr));
    narrators.value.forEach((n) => userBook.book.narrators?.push(n))
    seriesCopy.value.forEach((s) => {
      if (s.name.trim().length > 0) {
        userBook.book.series?.push(s)
      }
    })
    if (StringUtils.isNotBlank(imageUrl.value)) {
      userBook.book.image = imageUrl.value;
    }
    else if (imagePath.value != null && StringUtils.isNotBlank(imagePath.value)) {
      userBook.book.image = imagePath.value
    }
    else if (!deleteImage.value
      && metadata.value != null
      && metadata.value?.image != null
      && StringUtils.isNotBlank(metadata.value.image)) {
      userBook.book.image = metadata.value.image
    }
    if (eventType.value !== null && eventType.value !== "NONE" && eventDate.value != null) {
      console.log(
        "type " + StringUtils.readingEventTypeForValue(eventType.value)
      );
      userBook.lastReadingEvent = StringUtils.readingEventTypeForValue(eventType.value);
      userBook.lastReadingEventDate = eventDate.value?.toISOString()
    }
    try {
      console.log(`push book ` + userBook);
      console.log(userBook);
      progress.value = true
      const res: UserBook = await dataService.saveUserBookImage(
        userBook,
        file.value,
        (event: { loaded: number; total: number }) => {
          const percent = Math.round((100 * event.loaded) / event.total);
          console.log("percent " + percent);
          uploadPercentage.value = percent;
        }
      );
      progress.value = false
      console.log(`saved book ${res.book.title}`);
      ObjectUtils.toast(oruga, "success", t('labels.book_title_saved', {title : res.book.title}), 4000)
      clearForm();
      await router.push({name: 'my-books'})
    } catch (error: any) {
      progress.value = false
      ObjectUtils.toast(oruga, "danger", t('labels.error_message', {msg : error.message}), 4000)
    }
  } else {
    errorMessage.value = t('labels.provide_title');
  }
};

const fillBook = (formdata: any, publishedDate: Date | null): UserBook => {
  const userBook: UserBook = {
    book: {
      title: formdata.title,
      isbn10: formdata.isbn10,
      isbn13: formdata.isbn13,
      summary: formdata.summary,
      publisher: formdata.publisher,
      image: formdata.image,
      pageCount: formdata.pageCount,
      publishedDate: publishedDate?.toISOString(),
      series: [],
      googleId: formdata.googleId,
      amazonId: formdata.amazonId,
      goodreadsId: formdata.goodreadsId,
      librarythingId: formdata.librarythingId,
      isfdbId: formdata.isfdbId,
      openlibraryId: formdata.openlibraryId,
      noosfereId: formdata.noosfereId,
      inventaireId: formdata.inventaireId,
      language: formdata.language,
      authors: [],
      translators: [],
      narrators: [],
      tags: [],
      originalTitle: formdata.originalTitle.length > 0 ? formdata.originalTitle : null
    },
    owned: formdata.owned,
    personalNotes: formdata.personalNotes,
    toRead: formdata.toRead,
    percentRead: formdata.percentRead,
    currentPageNumber: formdata.currentPageNumber,
    borrowed: formdata.borrowed,
    price: (formdata.price == null || formdata.price < 0) ? null : formdata.price
  }
  return userBook
}

const clearForm = () => {
  clearImageField();
  errorMessage.value = "";
  eventType.value = null;
  file.value = null;
  authors.value = [];
  tags.value = [];
  translators.value = [];
  narrators.value = []
  uploadPercentage.value = 0;
  form.title = "";
  form.summary = "";
  form.isbn10 = "";
  form.isbn13 = "";
  form.publisher = "";
  form.pageCount = null;
  publishedDate.value = null
  seriesCopy.value = []
  form.language = ""
  form.owned = null
  form.personalNotes = ""
  form.amazonId = ""
  form.googleId = ""
  form.goodreadsId = ""
  form.librarythingId = ""
  form.isfdbId = ""
  form.openlibraryId = ""
  form.noosfereId = ""
  form.inventaireId = ""
  form.percentRead = 0
  form.currentPageNumber = null
  form.originalTitle = ""
  form.price= null
};

const clearDatePicker = () => {
  // close datepicker on reset
  publishedDate.value = null
};

const handleFileUpload = (event: any) => {
  file.value = event.target.files[0];
};

const clearImageField = () => {
  imageUrl.value = "";
};

function getFilteredData(text: string, target: Array<Wrapper>) {
  dataService.findAuthorByCriteria(Role.ANY, text).then((data) => {
    target.splice(0, target.length)
    data.content.forEach(a => target.push(ObjectUtils.wrapForOptions(a)))
  })
}

function getFilteredTags(text: string) {
  filteredTags.value.splice(0, filteredTags.value.length)
  dataService.findTagsByCriteria(text).then((data) => data.content.forEach(t => filteredTags.value.push(ObjectUtils.wrapForOptions(t))))
}

function getFilteredPublishers(text: string) {
  dataService.findPublisherByCriteria(text).then(data => filteredPublishers.value = data.content)
}

function beforeAdd(item: Author | string, target: Array<Author>) {
  let shouldAdd = true
  if (item instanceof Object) {
    target.forEach(author => {
      console.log(`author ${author.name}`)
      if (author.name === item.name) {
        console.log(`author ${author.name} item ${item.name}`)
        shouldAdd = false;
      }
    });
  }
  else {
    target.forEach(author => {
      console.log(`author ${author.name}`)
      if (author.name === item) {
        console.log(`author ${author.name} item ${item}`)
        shouldAdd = false;
      }
    });
  }
  return shouldAdd
}

function beforeAddTag(item: Tag | string) {
  let shouldAdd = true
  if (item instanceof Object) {
    tags.value.forEach(tag => {
      console.log(`tag ${tag.name}`)
      if (tag.name === item.name) {
        console.log(`tag ${tag.name} item ${item.name}`)
        shouldAdd = false;
      }
    });
  }
  else {
    tags.value.forEach(tag => {
      console.log(`tag ${tag.name}`)
      if (tag.name === item) {
        console.log(`tag ${tag.name} item ${item}`)
        shouldAdd = false;
      }
    });
  }
  return shouldAdd
}

function selectPublisher(publisher: string) {
  // we receive from oruga weird events while nothing is selected
  // so try to get rid of those null data we receive
  if (publisher != null) {
    form.publisher = publisher
  }
}

const toggleModal = (file: boolean) => {
  showModal.value = !showModal.value
  oruga.modal.open({
    parent: this,
    component: file ? AutoImportFileModalVue : AutoImportFormModalVue,
    trapFocus: true,
    active: true,
    canCancel: ['x', 'button', 'outside'],
    scroll: 'keep',
    props: {
        "book": undefined,
      },
    events: {
      metadataReceived: (modalMetadata: Metadata) => {
        console.log("received metadata")
        console.log(modalMetadata)
        metadata.value = modalMetadata
        mergeMetadata()
      }
    },
    onClose: modalClosed
  });
}

const toggleImagePickerModal = () => {
  showImagePickerModal.value = !showImagePickerModal.value
  oruga.modal.open({
    parent: this,
    component: ImagePickerModal,
    trapFocus: true,
    active: true,
    canCancel: ['x', 'button', 'outside'],
    scroll: 'keep',
    events: {
      choose: (path: Path) => {
        console.log("received path")
        console.log(path)
        imagePath.value = path.path
      }
    },
    onClose: modalClosed
  });
}

function modalClosed() {
  console.log("modal closed")
}

const mergeMetadata = () => {
  for (const key in metadata.value) {
    console.log("key")
    console.log(key)
    if (key in form) {
      const castKey = key as (keyof typeof metadata.value & keyof typeof form);
      (form[castKey] as any) = metadata.value[castKey];
    }
  }
  if (metadata.value?.authors != null && metadata.value.authors.length > 0) {
    const auths: Array<Author> = []
    metadata.value.authors.forEach(a => auths.push(ObjectUtils.createNamedItem(a)))
    authors.value = auths
  }
  if (metadata.value?.tags != null && metadata.value.tags.length > 0) {
    const importedTags: Array<Tag> = []
    metadata.value.tags.forEach(t => importedTags.push(ObjectUtils.createNamedItem(t)))
    tags.value = importedTags
  }
  if (metadata.value?.publishedDate && StringUtils.isNotBlank(metadata.value?.publishedDate)) {
    publishedDate.value = new Date(metadata.value?.publishedDate)
  }
  if (metadata.value?.series != null && metadata.value?.series?.length > 0) {
    seriesCopy.value.push({
      "name": metadata.value?.series,
      "numberInSeries" : metadata.value.numberInSeries
    })
  }
  if (metadata.value?.publisher) {
    selectPublisher(metadata.value.publisher)
  }
}

const isbn10ValidationMessage = ref("")

const isbn13ValidationMessage = ref("")

const validateIsbn10 = (isbn: string) => {
  if (StringUtils.isNotBlank(isbn)) {
    const isbnVerify = new IsbnVerify(isbn);
    if (!isbnVerify.isIsbn10()) {
      isbn10ValidationMessage.value = t('labels.invalid_isbn10')
    }
    else {


      isbn10ValidationMessage.value = ""
    }
  }
  else {
    isbn10ValidationMessage.value = ""
  }
}

const validateIsbn13 = (isbn: string) => {
  if (StringUtils.isNotBlank(isbn)) {
    const isbnVerify = new IsbnVerify(isbn);
    if (!isbnVerify.isIsbn13()) {
      isbn13ValidationMessage.value = t('labels.invalid_isbn13')
    }
    else {
      isbn13ValidationMessage.value = ""
    }
  }
  else {
    isbn13ValidationMessage.value = ""
  }
}

const displayDatepicker = computed(() => {
  return eventType.value !== null && eventType.value !== "NONE"
})

const eventTypeOptions = computed(() => [
  { value: ReadingEventType.FINISHED, label: t('reading_events.finished') },
  { value: ReadingEventType.CURRENTLY_READING, label: t('reading_events.currently_reading') },
  { value: ReadingEventType.DROPPED, label: t('reading_events.dropped') },
  { value: ReadingEventType.NONE, label: t('reading_events.none') },
])

const uploadTypeOptions = computed(() => [
  { value: 'web', label: t('labels.upload_from_web'), icon: 'web' },
  { value: 'computer', label: t('labels.upload_from_computer'), icon: 'laptop' },
  { value: 'server', label: t('labels.upload_from_server'), icon: 'server' },
])

const metadataFetchDisabled = computed(() => store != null && !store.getters.getMetadataFetchEnabled)

const currency = localStorage.getItem("JL_CURRENCY") ?? "EUR"

const imageUrlError = computed(() => StringUtils.isNotBlank(imageUrl.value) && !/^https?:\/\//i.test(imageUrl.value ?? '') ? t('labels.url_must_start') : false)

</script>

<template>
  <div class="m3-page m3-page--medium">
    <m-page-header :title="t('nav.add_book')">
      <template #actions>
        <button
          v-tooltip="t('labels.auto_fill_doc')"
          type="button"
          class="m3-btn m3-btn--tonal"
          :disabled="metadataFetchDisabled"
          @click="toggleModal(false)"
        >
          <i
            class="mdi mdi-cloud-download-outline"
            aria-hidden="true"
          />
          <span>{{ t('labels.auto_fill_from_web') }}</span>
        </button>
        <button
          v-tooltip="t('labels.auto_fill_book')"
          type="button"
          class="m3-btn m3-btn--outlined"
          @click="toggleModal(true)"
        >
          <i
            class="mdi mdi-file-upload-outline"
            aria-hidden="true"
          />
          <span>{{ t('labels.auto_fill_from_file') }}</span>
        </button>
      </template>
    </m-page-header>

    <div class="flex flex-col gap-6">
      <p
        v-if="metadataFetchDisabled"
        class="flex items-center gap-3 rounded-md bg-tertiary-container p-4 text-body-medium text-on-tertiary-container"
        role="note"
      >
        <i
          class="mdi mdi-information-outline text-[1.25rem]"
          aria-hidden="true"
        />
        <span>{{ t('labels.auto_import_disabled') }}</span>
      </p>

      <section class="m3-card m3-card--outlined">
        <div class="m3-card__content gap-4 p-6">
          <div class="flex items-center gap-3">
            <i
              class="mdi mdi-book-open-page-variant-outline text-[1.5rem] text-on-surface-variant"
              aria-hidden="true"
            />
            <h2 class="text-title-large">
              {{ t('book.section_book') }}
            </h2>
          </div>
          <div class="grid gap-4 md:grid-cols-2">
            <m-text-field
              v-model="form.title"
              :label="t('book.title')"
              required
            />
            <m-text-field
              v-model="form.originalTitle"
              :label="t('book.original_title')"
            />
          </div>
          <m-field :label="t('book.author', 2)">
            <o-taginput
              v-model="authors"
              :allow-autocomplete="true"
              autocomplete="off"
              :allow-new="true"
              :allow-duplicates="false"
              :open-on-focus="true"
              :options="filteredAuthors"
              :validate-item="(item: Author|string) => beforeAdd(item, authors)"
              :create-item="ObjectUtils.createNamedItem"
              icon-pack="mdi"
              icon="account-plus"
              :placeholder="t('labels.add_author')"
              @input="(v: string) => getFilteredData(v, filteredAuthors)"
            >
              <template #default="{ value }">
                <div class="jl-taginput-item">
                  {{ value.name }}
                </div>
              </template>
              <template #selected="{ removeItem, items }">
                <ClosableBadge
                  v-for="(item, index) in items"
                  :key="item.name"
                  :content="item.name"
                  @closed="removeItem(index, $event)"
                />
              </template>
            </o-taginput>
          </m-field>
          <div class="grid gap-4 md:grid-cols-2">
            <m-field :label="t('book.translator', 2)">
              <o-taginput
                v-model="translators"
                :options="filteredTranslators"
                :allow-autocomplete="true"
                autocomplete="off"
                :allow-new="true"
                :allow-duplicates="false"
                :open-on-focus="true"
                :validate-item="(item: Author) => beforeAdd(item, translators)"
                :create-item="ObjectUtils.createNamedItem"
                icon-pack="mdi"
                icon="account-plus"
                field="name"
                :placeholder="t('labels.add_translator')"
                @input="(v: string) => getFilteredData(v, filteredTranslators)"
              >
                <template #default="{ value }">
                  <div class="jl-taginput-item">
                    {{ value.name }}
                  </div>
                </template>
                <template #selected="{ removeItem, items }">
                  <ClosableBadge
                    v-for="(item, index) in items"
                    :key="item.name"
                    :content="item.name"
                    @closed="removeItem(index, $event)"
                  />
                </template>
              </o-taginput>
            </m-field>
            <m-field :label="t('book.narrator', 2)">
              <o-taginput
                v-model="narrators"
                :options="filteredNarrators"
                :allow-autocomplete="true"
                autocomplete="off"
                :allow-new="true"
                :allow-duplicates="false"
                :open-on-focus="true"
                :validate-item="(item: Author) => beforeAdd(item, narrators)"
                :create-item="ObjectUtils.createNamedItem"
                icon-pack="mdi"
                icon="account-plus"
                field="name"
                :placeholder="t('labels.add_narrator')"
                @input="(v: string) => getFilteredData(v, filteredNarrators)"
              >
                <template #default="{ value }">
                  <div class="jl-taginput-item">
                    {{ value.name }}
                  </div>
                </template>
                <template #selected="{ removeItem, items }">
                  <ClosableBadge
                    v-for="(item, index) in items"
                    :key="item.name"
                    :content="item.name"
                    @closed="removeItem(index, $event)"
                  />
                </template>
              </o-taginput>
            </m-field>
          </div>
          <m-field :label="t('book.tag', 2)">
            <o-taginput
              v-model="tags"
              :options="filteredTags"
              :allow-autocomplete="true"
              autocomplete="off"
              :allow-new="true"
              :allow-duplicates="false"
              :open-on-focus="true"
              :validate-item="beforeAddTag"
              :create-item="ObjectUtils.createNamedItem"
              icon-pack="mdi"
              icon="tag-plus"
              field="name"
              :placeholder="t('labels.add_tag')"
              @input="getFilteredTags"
            >
              <template #default="{ value }">
                <div class="jl-taginput-item">
                  {{ value.name }}
                </div>
              </template>
              <template #selected="{ removeItem, items }">
                <ClosableBadge
                  v-for="(item, index) in items"
                  :key="item.name"
                  :content="item.name"
                  @closed="removeItem(index, $event)"
                />
              </template>
            </o-taginput>
          </m-field>
          <SeriesCompleteInput v-model="seriesCopy" />
          <m-text-field
            v-model="form.summary"
            :label="t('book.summary')"
            multiline
            :rows="6"
            maxlength="50000"
          />
        </div>
      </section>

      <section class="m3-card m3-card--outlined">
        <div class="m3-card__content gap-4 p-6">
          <div class="flex items-center gap-3">
            <i
              class="mdi mdi-domain text-[1.5rem] text-on-surface-variant"
              aria-hidden="true"
            />
            <h2 class="text-title-large">
              {{ t('book.section_publication') }}
            </h2>
          </div>
          <div class="grid gap-4 sm:grid-cols-2">
            <m-text-field
              v-model="form.publisher"
              :label="t('book.publisher')"
              list="publishers-list"
              @input="getFilteredPublishers(($event.target as HTMLInputElement).value)"
            />
            <m-field :label="t('book.published_date')">
              <o-datepicker
                ref="datepicker"
                v-model="publishedDate"
                :show-week-number="false"
                :locale="undefined"
                :placeholder="t('labels.click_to_select')"
                icon="calendar"
                trap-focus
                expanded
              />
              <template #trailing>
                <button
                  v-if="publishedDate != null"
                  v-tooltip="t('labels.clear')"
                  type="button"
                  class="m3-icon-btn"
                  :aria-label="t('labels.clear')"
                  @click="clearDatePicker"
                >
                  <i
                    class="mdi mdi-close-circle-outline"
                    aria-hidden="true"
                  />
                </button>
              </template>
            </m-field>
            <m-text-field
              v-model="form.pageCount"
              type="number"
              :label="t('book.page_count')"
              min="0"
              clearable
              @clear="form.currentPageNumber = null"
            />
            <m-text-field
              v-model="form.language"
              :label="t('book.language')"
            />
          </div>
          <datalist id="publishers-list">
            <option
              v-for="pub in filteredPublishers"
              :key="pub"
              :value="pub"
            />
          </datalist>
        </div>
      </section>

      <section class="m3-card m3-card--outlined">
        <div class="m3-card__content gap-4 p-6">
          <div class="flex items-center gap-3">
            <i
              class="mdi mdi-barcode text-[1.5rem] text-on-surface-variant"
              aria-hidden="true"
            />
            <h2 class="text-title-large">
              {{ t('book.identifiers') }}
            </h2>
          </div>
          <div class="grid gap-4 sm:grid-cols-2">
            <m-text-field
              v-model="form.isbn10"
              :label="t('book.isbn10')"
              name="isbn10"
              :error="isbn10ValidationMessage"
              @blur="validateIsbn10(($event.target as HTMLInputElement).value)"
            />
            <m-text-field
              v-model="form.isbn13"
              :label="t('book.isbn13')"
              name="isbn13"
              :error="isbn13ValidationMessage"
              @blur="validateIsbn13(($event.target as HTMLInputElement).value)"
            />
          </div>
          <div class="grid gap-4 sm:grid-cols-2 md:grid-cols-3">
            <m-text-field
              v-model="form.goodreadsId"
              :label="t('book.goodreads_id')"
              name="goodreadsId"
            />
            <m-text-field
              v-model="form.googleId"
              :label="t('book.google_id')"
              name="googleId"
            />
            <m-text-field
              v-model="form.amazonId"
              :label="t('book.amazon_id')"
              name="amazonId"
            />
            <m-text-field
              v-model="form.librarythingId"
              :label="t('book.librarything_id')"
              name="librarythingId"
            />
            <m-text-field
              v-model="form.isfdbId"
              :label="t('book.isfdb_id')"
              name="isfdbId"
            />
            <m-text-field
              v-model="form.openlibraryId"
              :label="t('book.openlibrary_id')"
              name="openlibraryId"
            />
            <m-text-field
              v-model="form.noosfereId"
              :label="t('book.noosfere_id')"
              name="noosfereId"
            />
            <m-text-field
              v-model="form.inventaireId"
              :label="t('book.inventaire_id')"
              name="inventaireId"
            />
          </div>
        </div>
      </section>

      <section class="m3-card m3-card--outlined">
        <div class="m3-card__content gap-4 p-6">
          <div class="flex items-center gap-3">
            <i
              class="mdi mdi-glasses text-[1.5rem] text-on-surface-variant"
              aria-hidden="true"
            />
            <h2 class="text-title-large">
              {{ t('book.section_reading') }}
            </h2>
          </div>
          <div class="flex flex-col gap-1">
            <h3 class="text-title-small text-on-surface-variant">
              {{ t('book.status') }}
            </h3>
            <m-radio-group
              v-model="eventType"
              name="add-book-status"
              :options="eventTypeOptions"
              :label="t('book.status')"
              class="sm:flex-row sm:flex-wrap sm:gap-x-6"
            />
          </div>
          <m-field
            v-if="displayDatepicker"
            :label="t('labels.event_date')"
          >
            <o-datepicker
              v-model="eventDate"
              :show-week-number="false"
              :locale="undefined"
              :placeholder="t('labels.click_to_select')"
              :expanded="true"
              icon="calendar"
              mobile-native="false"
              mobile-modal="false"
              trap-focus
            />
            <template #trailing>
              <button
                v-if="eventDate != null"
                v-tooltip="t('labels.clear')"
                type="button"
                class="m3-icon-btn"
                :aria-label="t('labels.clear')"
                @click="eventDate = null"
              >
                <i
                  class="mdi mdi-close-circle-outline"
                  aria-hidden="true"
                />
              </button>
            </template>
          </m-field>
          <div class="grid max-w-3xl gap-x-8 sm:grid-cols-3">
            <label class="m3-choice m3-choice--between">
              <span>{{ t('book.owned') }}</span>
              <input
                v-model="form.owned"
                type="checkbox"
                role="switch"
                class="m3-switch"
              >
            </label>
            <label class="m3-choice m3-choice--between">
              <span class="flex flex-col">
                <span>{{ t('book.to_read') }}</span>
                <span
                  v-if="toReadDisplay"
                  class="text-body-small text-on-surface-variant"
                >{{ toReadDisplay }}</span>
              </span>
              <input
                v-model="form.toRead"
                type="checkbox"
                role="switch"
                class="m3-switch"
              >
            </label>
            <label class="m3-choice m3-choice--between">
              <span>{{ t('book.borrowed') }}</span>
              <input
                v-model="form.borrowed"
                type="checkbox"
                role="switch"
                class="m3-switch"
              >
            </label>
          </div>
          <div class="grid gap-4 sm:grid-cols-2">
            <m-text-field
              v-model="form.price"
              type="number"
              :label="t('book.price')"
              :suffix="currency"
              step="0.01"
              min="0"
              clearable
              :error="form.price != null && form.price < 0 ? t('errors.positive_only') : false"
            />
            <m-text-field
              v-model="form.currentPageNumber"
              type="number"
              :label="t('book.current_page_number')"
              min="0"
              :max="form.pageCount ?? undefined"
              :disabled="form.pageCount == null"
              clearable
            />
          </div>
          <m-slider
            v-model="form.percentRead"
            :label="t('book.percent_read')"
            show-value
            unit="%"
          />
          <m-text-field
            v-model="form.personalNotes"
            :label="t('book.personal_notes')"
            multiline
            maxlength="5000"
          />
        </div>
      </section>

      <section class="m3-card m3-card--outlined">
        <div class="m3-card__content gap-4 p-6">
          <div class="flex items-center gap-3">
            <i
              class="mdi mdi-image-outline text-[1.5rem] text-on-surface-variant"
              aria-hidden="true"
            />
            <h2 class="text-title-large">
              {{ t('book.section_cover') }}
            </h2>
          </div>
          <div class="flex flex-col gap-6 sm:flex-row sm:items-start">
            <div
              v-if="hasImage"
              class="flex shrink-0 flex-col gap-2"
            >
              <h3 class="text-title-small text-on-surface-variant">
                {{ t('labels.actual_cover') }}
              </h3>
              <div class="w-32">
                <book-cover
                  :image="metadata?.image"
                  :title="form.title"
                  rounded
                >
                  <div
                    v-if="deleteImage"
                    class="absolute inset-0 bg-surface/70"
                  />
                  <button
                    v-tooltip="deleteImage ? t('labels.refresh_to_restore') : t('labels.click_bin_to_remove')"
                    type="button"
                    class="m3-icon-btn m3-icon-btn--tonal absolute right-2 bottom-2"
                    :aria-label="deleteImage ? t('labels.refresh_to_restore') : t('labels.click_bin_to_remove')"
                    :aria-pressed="deleteImage"
                    @click="toggleRemoveImage"
                  >
                    <i
                      class="mdi"
                      :class="deleteImage ? 'mdi-restore' : 'mdi-delete-outline'"
                      aria-hidden="true"
                    />
                  </button>
                </book-cover>
              </div>
            </div>
            <div
              v-if="!hasImage || deleteImage"
              class="flex min-w-0 flex-1 flex-col gap-4"
            >
              <h3 class="text-title-small text-on-surface-variant">
                {{ t('labels.upload_cover') }}
              </h3>
              <m-segmented-button
                v-model="uploadType"
                :options="uploadTypeOptions"
                :aria-label="t('labels.upload_cover')"
                class="hidden lg:flex"
                block
              />
              <m-radio-group
                v-model="uploadType"
                name="add-book-cover-source"
                :options="uploadTypeOptions"
                :label="t('labels.upload_cover')"
                class="-mt-2 sm:flex-row sm:flex-wrap sm:gap-x-6 lg:hidden"
              />
              <m-text-field
                v-if="uploadType == 'web'"
                v-model="imageUrl"
                type="url"
                :label="t('labels.enter_image_address')"
                leading-icon="link-variant"
                placeholder="https://"
                pattern="https?://.*"
                clearable
                :error="imageUrlError"
                :supporting-text="t('labels.url_must_start')"
                @clear="clearImageField"
              />
              <div
                v-else-if="uploadType == 'computer'"
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
                      accept="image/*"
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
              <div
                v-else
                class="flex flex-wrap items-center gap-3"
              >
                <button
                  type="button"
                  class="m3-btn m3-btn--outlined"
                  @click="toggleImagePickerModal()"
                >
                  <i
                    class="mdi mdi-folder-search-outline"
                    aria-hidden="true"
                  />
                  <span>{{ t('labels.choose_file') }}</span>
                </button>
                <span class="min-w-0 truncate text-body-medium text-on-surface-variant">
                  {{ imagePath ? imagePath : t('labels.no_file_selected') }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <div class="sticky bottom-[calc(5rem+env(safe-area-inset-bottom))] z-10 mt-6 flex flex-col gap-3 border-t border-outline-variant bg-surface py-4 sm:bottom-0">
      <p
        v-if="errorMessage"
        class="flex items-center gap-3 rounded-md bg-error-container p-4 text-body-medium text-on-error-container"
        role="alert"
      >
        <i
          class="mdi mdi-alert-circle-outline text-[1.25rem]"
          aria-hidden="true"
        />
        <span>{{ errorMessage }}</span>
      </p>
      <div class="flex justify-end">
        <button
          type="button"
          class="m3-btn m3-btn--filled w-full sm:w-auto"
          :disabled="progress || !StringUtils.isNotBlank(form.title)"
          @click="importBook"
        >
          <span
            v-if="progress"
            class="m3-spinner"
          />
          <i
            v-else
            class="mdi mdi-book-plus-outline"
            aria-hidden="true"
          />
          <span>{{ t('labels.import_book') }}</span>
        </button>
      </div>
    </div>
  </div>
</template>
