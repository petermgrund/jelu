<script setup lang="ts">

import { useOruga } from "@oruga-ui/oruga-next";
import dayjs from "dayjs";
import { computed, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import { Author } from "../model/Author";
import { Wrapper } from "../model/autocomplete-wrapper";
import { UserBook } from "../model/Book";
import { Path } from "../model/DirectoryListing";
import { SeriesOrder } from "../model/Series";
import { Tag } from "../model/Tag";
import dataService from "../services/DataService";
import { ObjectUtils } from "../utils/ObjectUtils";
import { StringUtils } from "../utils/StringUtils";
import ImagePickerModal from "./ImagePickerModal.vue";
import SeriesCompleteInput from "./SeriesCompleteInput.vue";
import ClosableBadge from "./ClosableBadge.vue";
import { Role } from "../model/Role";
import useBookStatus, { BOOK_STATUSES, BookStatus, bookStatus, statusEventType } from "../composables/bookStatus";
import useOwnership, { OWNERSHIP_STATUSES, ownershipOf } from "../composables/ownership";
import { OwnershipStatus } from "../model/Ownership";
import BookCover from "./BookCover.vue";
import MDialog from "./m3/MDialog.vue";
import MField from "./m3/MField.vue";
import MRadioGroup from "./m3/MRadioGroup.vue";
import MSegmentedButton from "./m3/MSegmentedButton.vue";
import MSlider from "./m3/MSlider.vue";
import MTextField from "./m3/MTextField.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const props = defineProps<{ bookId: string, book: UserBook | null, canAddEvent: boolean }>()
const oruga = useOruga()
const emit = defineEmits(['close']);

const filteredAuthors: Ref<Array<Wrapper>> = ref([]);
const filteredTags: Ref<Array<Wrapper>> = ref([]);
const filteredTranslators: Ref<Array<Wrapper>> = ref([]);
const filteredNarrators: Ref<Array<Wrapper>> = ref([]);
const filteredPublishers: Ref<Array<string>> = ref([])
const userbook: Ref<UserBook> = ref(copyInput(props.book))
const hasImage: Ref<boolean> = ref(userbook.value.book.image != null)
const deleteImage: Ref<boolean> = ref(false)

const progressTouched: Ref<boolean> = ref(false)

const progress: Ref<boolean> = ref(false)

const publishedDate = ref(userbook.value.book.publishedDate ? new Date(userbook.value.book.publishedDate) : null)

function copyInput(book: UserBook | null): any {
  if (book == null) {
    return {}
  }
  const b = ObjectUtils.deepCopy(book)
  console.log("copy")
  console.log(b)
  return b
}

const handleFileUpload = (event: any) => {
  file.value = event.target.files[0];
};

const imageUrl = ref<string | null>(null);
const imagePath = ref<string | null>(null);
const uploadType = ref('web');

const clearImageField = () => {
  imageUrl.value = "";
};

const showImagePickerModal: Ref<boolean> = ref(false)

const file = ref<File | null>(null);

const uploadPercentage = ref(0);
const errorMessage = ref("");
// status and ownership are only chosen here when the book is added to the library,
// afterwards the book page changes them and keeps their history
const NO_STATUS = 'NONE'
const status: Ref<BookStatus | typeof NO_STATUS> = ref(bookStatus(userbook.value) ?? NO_STATUS)
const ownership: Ref<OwnershipStatus> = ref(ownershipOf(userbook.value))

const clearDatePicker = () => {
  // close datepicker on reset
  publishedDate.value = null
};

const seriesCopy: Array<SeriesOrder> = userbook.value.book.series ?? []

const importBook = () => {
  userbook.value.book.series = seriesCopy.filter(s => s.name != null && s.name.trim().length > 0)
  console.log("import")
  console.log(userbook)
  if (props.canAddEvent) {
    userbook.value.toRead = status.value === BookStatus.WANT_TO_READ ? true : null
    userbook.value.lastReadingEvent = status.value === NO_STATUS || status.value === BookStatus.WANT_TO_READ
      ? null
      : statusEventType(status.value)
    userbook.value.owned = ownership.value === OwnershipStatus.OWNED ? true : null
    userbook.value.borrowed = ownership.value === OwnershipStatus.BORROWED ? true : null
  } else {
    userbook.value.lastReadingEvent = null
  }
  if (StringUtils.isNotBlank(imageUrl.value)) {
    userbook.value.book.image = imageUrl.value
  } else if (imagePath.value != null && StringUtils.isNotBlank(imagePath.value)) {
      userbook.value.book.image = imagePath.value
  } else if (deleteImage.value) {
    userbook.value.book.image = null
  }
  if (userbook.value.price != null) {
    if (userbook.value.price <= 0) {
      userbook.value.price = null
    }
  }
  // otherwise progress is filled and backend creates a new event
  if (progressTouched.value == false) {
    userbook.value.percentRead = null
  }

  console.log(`push book ` + userbook.value);
  console.log(userbook.value);
  let promise: Promise<UserBook>
  progress.value = true
  // no id on userbook -> we have a book and save the userbook
  if (StringUtils.isBlank(userbook.value.id)) {
    promise = dataService.saveUserBookImage(
      userbook.value,
      file.value,
      (event: { loaded: number; total: number }) => {
        const percent = Math.round((100 * event.loaded) / event.total);
        console.log("percent " + percent);
        uploadPercentage.value = percent;
      }
    )
  }
  // just update the existing userbook
  else {
    promise = dataService.updateUserBookImage(
      userbook.value,
      file.value,
      (event: { loaded: number; total: number }) => {
        const percent = Math.round((100 * event.loaded) / event.total);
        console.log("percent " + percent);
        uploadPercentage.value = percent;
      }
    )
  }
  promise
    .then(res => {
      console.log(`update book ${res.book.title}`);
      progress.value = false
      ObjectUtils.toast(oruga, "success", t('labels.book_title_updated', { title : res.book.title}), 4000);
      emit('close')
    })
    .catch(err => {
      progress.value = false
      ObjectUtils.toast(oruga, "danger", t('labels.error_message', {msg : err.message}), 4000);
    })

}

function getFilteredData(text: string, target: Array<Wrapper>) {
  dataService.findAuthorByCriteria(Role.ANY, text).then((data) => {
    target.splice(0, target.length)
    data.content.forEach(a => target.push(ObjectUtils.wrapForOptions(a)))
  })
}

function getFilteredTags(text: string) {
  dataService.findTagsByCriteria(text).then((data) => {
    filteredTags.value.splice(0, filteredTags.value.length)
    data.content.forEach(t => filteredTags.value.push(ObjectUtils.wrapForOptions(t)))
  })
}

function getFilteredPublishers(text: string) {
  userbook.value.book.publisher = text
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
    userbook.value.book?.tags?.forEach(tag => {
      console.log(`tag ${tag.name}`)
      if (tag.name === item.name) {
        console.log(`tag ${tag.name} item ${item.name}`)
        shouldAdd = false;
      }
    });
  }
  else {
    userbook.value.book?.tags?.forEach(tag => {
      console.log(`tag ${tag.name}`)
      if (tag.name === item) {
        console.log(`tag ${tag.name} item ${item}`)
        shouldAdd = false;
      }
    });
  }
  return shouldAdd
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

function toggleRemoveImage() {
  deleteImage.value = !deleteImage.value
}

watch(() => [userbook.value.currentPageNumber, userbook.value.percentRead, userbook.value.book.pageCount],(newVal, oldVal) => {
  if (userbook.value.book.pageCount != null) {
    ObjectUtils.computePages(newVal, oldVal, userbook.value, userbook.value.book.pageCount)
  }
})

watch(() => userbook.value.percentRead,(newVal, oldVal) => {
  if (newVal != null) {
    progressTouched.value = true
  }
})

watch(() => publishedDate.value, (newVal, oldVal) => {
    if (newVal == null) {
        userbook.value.book.publishedDate = null
    } else {
        const formatted = dayjs(newVal).format('YYYY-MM-DD')
        userbook.value.book.publishedDate = formatted
    }
})

if (userbook.value.book.publisher != null) {
  filteredPublishers.value.push(userbook.value.book.publisher as string) // prefill editor autocomplete. oruga workaround
}

// fixed id: this dialog is mounted as a separate app by oruga, useId() would collide with page ids
const publishersListId = "edit-book-publishers-list"

const { statusLabel } = useBookStatus()
const { ownershipLabel } = useOwnership()

const statusOptions = computed(() => [
  { value: NO_STATUS, label: t('status.none') },
  ...BOOK_STATUSES.map(value => ({ value, label: statusLabel(value) })),
])

const ownershipOptions = computed(() => OWNERSHIP_STATUSES.map(value => ({ value, label: ownershipLabel(value) })))

const uploadTypeOptions = computed(() => [
  { value: 'web', label: t('labels.upload_from_web'), icon: 'web' },
  { value: 'computer', label: t('labels.upload_from_computer'), icon: 'laptop' },
  { value: 'server', label: t('labels.upload_from_server'), icon: 'server' },
])

const currency = localStorage.getItem("JL_CURRENCY") ?? "EUR"

const imageUrlError = computed(() => StringUtils.isNotBlank(imageUrl.value) && !/^https?:\/\//i.test(imageUrl.value ?? '') ? t('labels.url_must_start') : false)
</script>

<template>
  <m-dialog
    :headline="t('labels.edit_book')"
    size="xl"
    fullscreen
    @close="emit('close')"
  >
    <div class="flex flex-col gap-6">
      <section class="flex flex-col gap-4">
        <h3 class="flex items-center gap-3 text-title-medium text-on-surface">
          <i
            class="mdi mdi-book-open-page-variant-outline text-[1.25rem] text-on-surface-variant"
            aria-hidden="true"
          />
          {{ t('book.section_book') }}
        </h3>
        <div class="grid gap-4 md:grid-cols-2">
          <m-text-field
            v-model="userbook.book.title"
            :label="t('book.title')"
            required
          />
          <m-text-field
            v-model="userbook.book.originalTitle"
            :label="t('book.original_title')"
          />
        </div>
        <m-field :label="t('book.author', 2)">
          <o-taginput
            v-model="userbook.book.authors"
            :options="filteredAuthors"
            :allow-autocomplete="true"
            autocomplete="off"
            :allow-new="true"
            :allow-duplicates="false"
            :open-on-focus="true"
            :validate-item="(item: Author|string) => beforeAdd(item, userbook.book.authors as Array<Author>)"
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
              v-model="userbook.book.translators"
              :options="filteredTranslators"
              :allow-autocomplete="true"
              autocomplete="off"
              :allow-new="true"
              :allow-duplicates="false"
              :open-on-focus="true"
              :validate-item="(item: Author|string) => beforeAdd(item, userbook.book.translators as Array<Author>)"
              :create-item="ObjectUtils.createNamedItem"
              icon-pack="mdi"
              icon="account-plus"
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
              v-model="userbook.book.narrators"
              :options="filteredNarrators"
              :allow-autocomplete="true"
              autocomplete="off"
              :allow-new="true"
              :allow-duplicates="false"
              :open-on-focus="true"
              :validate-item="(item: Author|string) => beforeAdd(item, userbook.book.narrators as Array<Author>)"
              :create-item="ObjectUtils.createNamedItem"
              icon-pack="mdi"
              icon="account-plus"
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
            v-model="userbook.book.tags"
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
          v-model="userbook.book.summary"
          :label="t('book.summary')"
          multiline
          :rows="6"
          maxlength="50000"
        />
      </section>

      <hr class="m3-divider">

      <section class="flex flex-col gap-4">
        <h3 class="flex items-center gap-3 text-title-medium text-on-surface">
          <i
            class="mdi mdi-domain text-[1.25rem] text-on-surface-variant"
            aria-hidden="true"
          />
          {{ t('book.section_publication') }}
        </h3>
        <div class="grid gap-4 sm:grid-cols-2">
          <m-text-field
            v-model="userbook.book.publisher"
            :label="t('book.publisher')"
            :list="publishersListId"
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
            v-model="userbook.book.pageCount"
            type="number"
            :label="t('book.page_count')"
            min="0"
            clearable
            @clear="userbook.currentPageNumber = null"
          />
          <m-text-field
            v-model="userbook.book.language"
            :label="t('book.language')"
          />
        </div>
        <datalist :id="publishersListId">
          <option
            v-for="pub in filteredPublishers"
            :key="pub"
            :value="pub"
          />
        </datalist>
      </section>

      <hr class="m3-divider">

      <section class="flex flex-col gap-4">
        <h3 class="flex items-center gap-3 text-title-medium text-on-surface">
          <i
            class="mdi mdi-barcode text-[1.25rem] text-on-surface-variant"
            aria-hidden="true"
          />
          {{ t('book.identifiers') }}
        </h3>
        <div class="grid gap-4 sm:grid-cols-2">
          <m-text-field
            v-model="userbook.book.isbn10"
            :label="t('book.isbn10')"
            name="isbn10"
          />
          <m-text-field
            v-model="userbook.book.isbn13"
            :label="t('book.isbn13')"
            name="isbn13"
          />
        </div>
        <div class="grid gap-4 sm:grid-cols-2 md:grid-cols-3">
          <m-text-field
            v-model="userbook.book.goodreadsId"
            :label="t('book.goodreads_id')"
            name="goodreadsId"
          />
          <m-text-field
            v-model="userbook.book.googleId"
            :label="t('book.google_id')"
            name="googleId"
          />
          <m-text-field
            v-model="userbook.book.amazonId"
            :label="t('book.amazon_id')"
            name="amazonId"
          />
          <m-text-field
            v-model="userbook.book.librarythingId"
            :label="t('book.librarything_id')"
            name="librarythingId"
          />
          <m-text-field
            v-model="userbook.book.isfdbId"
            :label="t('book.isfdb_id')"
            name="isfdbId"
          />
          <m-text-field
            v-model="userbook.book.openlibraryId"
            :label="t('book.openlibrary_id')"
            name="openlibraryId"
          />
          <m-text-field
            v-model="userbook.book.noosfereId"
            :label="t('book.noosfere_id')"
            name="noosfereId"
          />
          <m-text-field
            v-model="userbook.book.inventaireId"
            :label="t('book.inventaire_id')"
            name="inventaireId"
          />
        </div>
      </section>

      <hr class="m3-divider">

      <section class="flex flex-col gap-4">
        <h3 class="flex items-center gap-3 text-title-medium text-on-surface">
          <i
            class="mdi mdi-glasses text-[1.25rem] text-on-surface-variant"
            aria-hidden="true"
          />
          {{ t('book.section_reading') }}
        </h3>
        <template v-if="props.canAddEvent">
          <div class="flex flex-col gap-1">
            <h4 class="text-title-small text-on-surface-variant">
              {{ t('status.status') }}
            </h4>
            <m-radio-group
              v-model="status"
              name="edit-book-status"
              :options="statusOptions"
              :label="t('status.status')"
              class="sm:flex-row sm:flex-wrap sm:gap-x-6"
            />
          </div>
          <div class="flex flex-col gap-1">
            <h4 class="text-title-small text-on-surface-variant">
              {{ t('ownership.ownership') }}
            </h4>
            <m-radio-group
              v-model="ownership"
              name="edit-book-ownership"
              :options="ownershipOptions"
              :label="t('ownership.ownership')"
              class="sm:flex-row sm:flex-wrap sm:gap-x-6"
            />
          </div>
        </template>
        <div class="grid gap-4 sm:grid-cols-2">
          <m-text-field
            v-model="userbook.price"
            type="number"
            :label="t('book.price')"
            :suffix="currency"
            step="0.01"
            min="0"
            clearable
            :error="userbook.price != null && userbook.price < 0 ? t('errors.positive_only') : false"
          />
          <m-text-field
            v-model="userbook.currentPageNumber"
            type="number"
            :label="t('book.current_page_number')"
            min="0"
            :max="userbook.book.pageCount ?? undefined"
            :disabled="userbook.book.pageCount == null"
            clearable
          />
        </div>
        <m-slider
          v-model="userbook.percentRead"
          :label="t('book.percent_read')"
          :disabled="userbook.book.pageCount != null"
          show-value
          unit="%"
        />
        <m-text-field
          v-model="userbook.personalNotes"
          :label="t('book.personal_notes')"
          multiline
          maxlength="5000"
        />
      </section>

      <hr class="m3-divider">

      <section class="flex flex-col gap-4">
        <h3 class="flex items-center gap-3 text-title-medium text-on-surface">
          <i
            class="mdi mdi-image-outline text-[1.25rem] text-on-surface-variant"
            aria-hidden="true"
          />
          {{ t('book.section_cover') }}
        </h3>
        <div class="flex flex-col gap-6 sm:flex-row sm:items-start">
          <div
            v-if="hasImage"
            class="flex shrink-0 flex-col gap-2"
          >
            <h4 class="text-title-small text-on-surface-variant">
              {{ t('labels.actual_cover') }}
            </h4>
            <div class="w-32">
              <book-cover
                :image="userbook.book.image"
                :title="userbook.book.title"
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
            <h4 class="text-title-small text-on-surface-variant">
              {{ t('labels.upload_cover') }}
            </h4>
            <m-segmented-button
              v-model="uploadType"
              :options="uploadTypeOptions"
              :aria-label="t('labels.upload_cover')"
              class="hidden lg:flex"
              block
            />
            <m-radio-group
              v-model="uploadType"
              name="edit-book-cover-source"
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
      </section>

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
    </div>
    <template #actions>
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
        :disabled="progress"
        @click="importBook"
      >
        <span
          v-if="progress"
          class="m3-spinner"
        />
        <span>{{ t('labels.save') }}</span>
      </button>
    </template>
  </m-dialog>
</template>
