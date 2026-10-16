<script setup lang="ts">
import { computed, Ref, ref } from "vue";
import { useI18n } from 'vue-i18n';
import { Author } from "../model/Author";
import { Wrapper } from "../model/autocomplete-wrapper";
import { Book } from "../model/Book";
import { Metadata } from "../model/Metadata";
import { SeriesOrder } from "../model/Series";
import { Tag } from "../model/Tag";
import dataService from "../services/DataService";
import { ObjectUtils } from "../utils/ObjectUtils";
import SeriesCompleteInput from "./SeriesCompleteInput.vue";
import { Role } from "../model/Role";
import BookCover from "./BookCover.vue";
import ClosableBadge from "./ClosableBadge.vue";
import MDialog from "./m3/MDialog.vue";
import MField from "./m3/MField.vue";
import MTextField from "./m3/MTextField.vue";

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const props = defineProps<{
  book: Book,
  metadata: Metadata
}>()

const book: Ref<Book> = ref(props.book)
const initialImage = props.book.image
const initialIsbn10: Ref<string|undefined> = ref(book.value.isbn10)
const initialIsbn13: Ref<string|undefined> = ref(book.value.isbn13)
const seriesCopy: Array<SeriesOrder> = book.value.series ?? []

const emit = defineEmits<{
  (e: 'close'): void
}>()

let authors: Ref<Array<string|Author>> = ref([]);
let tags: Ref<Array<string|Tag>> = ref([])

let filteredAuthors: Ref<Array<Wrapper>> = ref([]);
let filteredTags: Ref<Array<Wrapper>> = ref([]);

const progress: Ref<boolean> = ref(false)
const replaceImage: Ref<boolean> = ref(false)

const discard = () => {
  emit('close')
}

const isbnHasChanged = () => {
  return (book.value.isbn10 != undefined && book.value.isbn10 !== initialIsbn10.value) ||
  (book.value.isbn13 != undefined && book.value.isbn13 !== initialIsbn13.value)
}

const importData = async () => {
  if (book.value.id != null) {
    progress.value = true
    let saveBook = true
    // only check if isbn has changed, otherwise we could
    // only be editing an already existing book
    // with an isbn already set. And we don't want to warn in that case
    if (isbnHasChanged()) {
      let alreadyExisting = await dataService.checkIsbnExists(book.value.isbn10, book.value.isbn13)
      console.log('already existing')
      console.log(alreadyExisting)
      if (alreadyExisting != null) {
        saveBook = false
        await ObjectUtils.swalYesNoMixin.fire({
          html: `<p>${t('labels.book_with_same_isbn_already_exists')}:<br>${alreadyExisting.title}<br>${t('labels.save_new_anyway')}</p>`,
          showDenyButton: false,
          confirmButtonText: t('labels.save'),
        }).then((result) => {
          if (result.isConfirmed) {
            saveBook = true
          } else if (result.isDenied) {
            ObjectUtils.baseSwalMixin.fire('', t('labels.changes_not_saved'), 'info')
          }
        })
      }
    }
    console.log(`save book ${saveBook}`)
    if (!saveBook) {
      progress.value = false
      return
    }
    if (seriesCopy.length > 0) {
      if (book.value.series == null) {
        book.value.series = []
      }
      seriesCopy.forEach(s => {
        if (s.name.trim().length > 0){
          book.value.series?.push(s)
        }
      })
    }
    dataService.updateBook(book.value.id, {...book.value})
    .then(res => {
          progress.value = false
          emit('close')
        })
        .catch(err => {
          progress.value = false
          console.log(err)
        })
  }
}

function beforeAdd(item: Author | string) {
  let shouldAdd = true
  if (item instanceof Object) {
    book.value.authors?.forEach(author => {
      console.log(`author ${author.name}`)
      if (author.name === item.name) {
        console.log(`author ${author.name} item ${item.name}`)
        shouldAdd = false;
      }
    });
  }
  else {
    book.value.authors?.forEach(author => {
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
    book.value.tags?.forEach(tag => {
      console.log(`tag ${tag.name}`)
      if (tag.name === item.name) {
        console.log(`tag ${tag.name} item ${item.name}`)
        shouldAdd = false;
      }
    });
  }
  else {
    book.value.tags?.forEach(tag => {
      console.log(`tag ${tag.name}`)
      if (tag.name === item) {
        console.log(`tag ${tag.name} item ${item}`)
        shouldAdd = false;
      }
    });
  }
  return shouldAdd
}

function createAuthor(item: Author | string) {
    return item
}

function createTag(item: Tag | string) {
    return item
}

function getFilteredAuthors(text: string) {
  console.log("option " + text)
  dataService.findAuthorByCriteria(Role.ANY, text).then((data) => {
    filteredAuthors.value.splice(filteredAuthors.value.length)
    data.content.forEach(a => filteredAuthors.value.push(ObjectUtils.wrapForOptions(a)))
  })
}

function getFilteredTags(text: string) {
  dataService.findTagsByCriteria(text).then((data) => data.content.forEach(t => filteredTags.value.push(ObjectUtils.wrapForOptions(t))))
}

function authorAdded(item: string|Author) {
  itemAdded(item, book.value.authors as Array<Author>)
}

function tagAdded(item: string|Tag) {
  itemAdded(item, book.value.tags as Array<Tag>)
}

function itemAdded(item: string|Author|Tag, target: Array<Author|Tag>) {
  console.log("added")
  console.log(item)
  console.log(book.value.authors)
  console.log(authors.value)
    if (typeof item === 'string') {
      console.log("item is string : " + item)
      target.push({"name": item})
    } else {
      console.log("item is author")
      console.log(item)
      target.push(item)
    }
  console.log(book.value.authors)
}

function authorRemoved(item: string|Author) {
  console.log("removed")
  console.log(item)
  if (typeof item === 'string') {
    console.log("type string")
    const toKeep = book.value.authors?.filter(a => a.name !== item)
    book.value.authors = toKeep
  } else {
    console.log('type author')
    const toKeep = book.value.authors?.filter(a => a.id !== item.id)
    book.value.authors = toKeep
  }
  console.log(book.value.authors)
}

function tagRemoved(item: string|Tag) {
  console.log("removed")
  console.log(item)
  if (typeof item === 'string') {
    console.log("type string")
    const toKeep = book.value.tags?.filter(a => a.name !== item)
    book.value.tags = toKeep
  } else {
    console.log('type tag')
    const toKeep = book.value.tags?.filter(a => a.id !== item.id)
    book.value.tags = toKeep
  }
  console.log(book.value.tags)
}

book.value.authors?.forEach(a => authors.value.push(a.name))
console.log("authors")
console.log(authors.value)
book.value.tags?.forEach(t => tags.value.push(t.name))

// text identifiers compared on one row each, their book and metadata types match
type IdentifierKey = 'isbn10' | 'isbn13' | 'googleId' | 'goodreadsId' | 'amazonId'
  | 'librarythingId' | 'isfdbId' | 'openlibraryId' | 'noosfereId' | 'inventaireId'

const identifierFields = computed((): Array<{ key: IdentifierKey, label: string }> => [
  { key: 'isbn10', label: t('book.isbn10') },
  { key: 'isbn13', label: t('book.isbn13') },
  { key: 'googleId', label: t('book.google_id') },
  { key: 'goodreadsId', label: t('book.goodreads_id') },
  { key: 'amazonId', label: t('book.amazon_id') },
  { key: 'librarythingId', label: t('book.librarything_id') },
  { key: 'isfdbId', label: t('book.isfdb_id') },
  { key: 'openlibraryId', label: t('book.openlibrary_id') },
  { key: 'noosfereId', label: t('book.noosfere_id') },
  { key: 'inventaireId', label: t('book.inventaire_id') },
])
</script>

<template>
  <m-dialog
    :headline="t('book_merge.merge_books')"
    size="xl"
    fullscreen
    @close="emit('close')"
  >
    <div class="sticky -top-4 z-10 -mt-4 hidden border-b border-outline-variant bg-surface-container-high pt-4 pb-3 md:grid md:grid-cols-[9rem_minmax(0,1fr)_minmax(0,1fr)] md:items-center md:gap-x-4">
      <span aria-hidden="true" />
      <span class="text-title-small text-on-surface">{{ t('labels.current') }}</span>
      <span class="flex min-w-0 items-center gap-2 pl-1">
        <span class="truncate text-title-small text-on-surface">{{ t('book_merge.fetched_metadata') }}</span>
        <span
          v-if="props.metadata.sourcePlugin"
          v-tooltip="t('metadata.source_plugin')"
          class="m3-status m3-status--outlined"
        ><i
          class="mdi mdi-puzzle-outline"
          aria-hidden="true"
        /><span>{{ props.metadata.sourcePlugin }}</span></span>
      </span>
    </div>

    <div class="divide-y divide-outline-variant">
      <!-- cover: both thumbnails stay side by side, even on phones -->
      <div
        v-if="props.metadata.image"
        class="grid grid-cols-2 gap-x-4 gap-y-2 py-4 md:grid-cols-[9rem_minmax(0,1fr)_minmax(0,1fr)]"
      >
        <span class="col-span-2 text-label-large text-on-surface-variant md:col-span-1 md:pt-2">{{ t('book.section_cover') }}</span>
        <div class="flex flex-col items-start gap-2">
          <div class="w-24 sm:w-32">
            <book-cover
              :image="book.image"
              :title="book.title"
              rounded
            />
          </div>
          <button
            v-if="replaceImage"
            type="button"
            class="m3-btn m3-btn--text m3-btn--sm"
            @click="book.image = initialImage; replaceImage = false;"
          >
            <i
              class="mdi mdi-undo-variant"
              aria-hidden="true"
            />
            <span>{{ t('labels.discard') }}</span>
          </button>
        </div>
        <div class="flex items-start gap-1 self-start justify-self-start rounded-md bg-surface-container-highest py-2 pr-2 pl-1 sm:pr-4">
          <button
            v-tooltip="t('book_merge.use_value')"
            type="button"
            class="m3-icon-btn"
            :aria-label="`${t('book_merge.use_value')} (${t('book.section_cover')})`"
            @click="book.image = props.metadata.image; replaceImage = true;"
          >
            <i
              class="mdi mdi-arrow-left"
              aria-hidden="true"
            />
          </button>
          <div class="w-24 sm:w-32">
            <book-cover
              :image="props.metadata.image"
              :title="props.metadata.title"
              rounded
            />
          </div>
        </div>
      </div>

      <div class="grid gap-2 py-4 md:grid-cols-[9rem_minmax(0,1fr)_minmax(0,1fr)] md:gap-x-4">
        <span class="text-label-large text-on-surface-variant md:pt-4">{{ t('book.title') }}</span>
        <m-text-field
          v-model="book.title"
          :aria-label="t('book.title')"
        />
        <div class="flex min-h-14 items-start gap-1 rounded-md bg-surface-container-highest py-2 pr-4 pl-1">
          <button
            v-tooltip="t('book_merge.use_value')"
            type="button"
            class="m3-icon-btn"
            :aria-label="`${t('book_merge.use_value')} (${t('book.title')})`"
            @click="book.title = props.metadata.title != null ? props.metadata.title : ''"
          >
            <i
              class="mdi mdi-arrow-up md:hidden"
              aria-hidden="true"
            />
            <i
              class="mdi mdi-arrow-left hidden md:inline"
              aria-hidden="true"
            />
          </button>
          <div class="flex min-w-0 flex-1 flex-col py-2">
            <span class="text-label-small text-on-surface-variant md:hidden">{{ t('book_merge.fetched_metadata') }}</span>
            <span class="text-body-large break-words text-on-surface">{{ props.metadata.title || '—' }}</span>
          </div>
        </div>
      </div>

      <div class="grid gap-2 py-4 md:grid-cols-[9rem_minmax(0,1fr)_minmax(0,1fr)] md:gap-x-4">
        <span class="text-label-large text-on-surface-variant md:pt-4">{{ t('book.author', 2) }}</span>
        <m-field>
          <o-taginput
            v-model="authors"
            :options="filteredAuthors"
            :allow-autocomplete="true"
            autocomplete="off"
            :allow-new="true"
            :allow-duplicates="false"
            :open-on-focus="true"
            :validate-item="beforeAdd"
            :create-item="createAuthor"
            icon-pack="mdi"
            icon="account-plus"
            :placeholder="t('labels.add_author')"
            @input="getFilteredAuthors"
            @add="authorAdded"
            @remove="authorRemoved"
          >
            <template #default="{ value }">
              <span>{{ value.name }}</span>
            </template>
            <template #selected="{ items, removeItem }">
              <closable-badge
                v-for="(item, index) in items"
                :key="typeof item === 'string' ? item : item.name"
                :content="typeof item === 'string' ? item : item.name"
                @closed="removeItem(index)"
              />
            </template>
          </o-taginput>
        </m-field>
        <div class="flex min-h-14 items-start gap-1 rounded-md bg-surface-container-highest py-2 pr-4 pl-1">
          <span
            class="w-10 shrink-0"
            aria-hidden="true"
          />
          <div class="flex min-w-0 flex-1 flex-col gap-1 py-1">
            <span class="text-label-small text-on-surface-variant md:hidden">{{ t('book_merge.fetched_metadata') }}</span>
            <div
              v-if="props.metadata.authors != null && props.metadata.authors.length > 0"
              class="m3-chip-set"
            >
              <span
                v-for="author in props.metadata.authors"
                :key="author"
                class="m3-chip m3-chip--assist m3-chip--static"
              >
                <i
                  class="mdi mdi-account-outline"
                  aria-hidden="true"
                />
                <span>{{ author }}</span>
              </span>
            </div>
            <span
              v-else
              class="py-1 text-body-large text-on-surface"
            >—</span>
          </div>
        </div>
      </div>

      <div class="grid gap-2 py-4 md:grid-cols-[9rem_minmax(0,1fr)_minmax(0,1fr)] md:gap-x-4">
        <span class="text-label-large text-on-surface-variant md:pt-4">{{ t('book.tag', 2) }}</span>
        <m-field>
          <o-taginput
            v-model="tags"
            :options="filteredTags"
            :allow-autocomplete="true"
            autocomplete="off"
            :allow-new="true"
            :allow-duplicates="false"
            :open-on-focus="true"
            :validate-item="beforeAddTag"
            :create-item="createTag"
            icon-pack="mdi"
            icon="tag-plus"
            :placeholder="t('labels.add_tag')"
            @input="getFilteredTags"
            @add="tagAdded"
            @remove="tagRemoved"
          >
            <template #default="{ value }">
              <span>{{ value.name }}</span>
            </template>
            <template #selected="{ items, removeItem }">
              <closable-badge
                v-for="(item, index) in items"
                :key="typeof item === 'string' ? item : item.name"
                :content="typeof item === 'string' ? item : item.name"
                @closed="removeItem(index)"
              />
            </template>
          </o-taginput>
        </m-field>
        <div class="flex min-h-14 items-start gap-1 rounded-md bg-surface-container-highest py-2 pr-4 pl-1">
          <span
            class="w-10 shrink-0"
            aria-hidden="true"
          />
          <div class="flex min-w-0 flex-1 flex-col gap-1 py-1">
            <span class="text-label-small text-on-surface-variant md:hidden">{{ t('book_merge.fetched_metadata') }}</span>
            <div
              v-if="props.metadata.tags != null && props.metadata.tags.length > 0"
              class="m3-chip-set"
            >
              <span
                v-for="tag in props.metadata.tags"
                :key="tag"
                class="m3-chip m3-chip--assist m3-chip--static"
              >
                <i
                  class="mdi mdi-tag-outline"
                  aria-hidden="true"
                />
                <span>{{ tag }}</span>
              </span>
            </div>
            <span
              v-else
              class="py-1 text-body-large text-on-surface"
            >—</span>
          </div>
        </div>
      </div>

      <div class="grid gap-2 py-4 md:grid-cols-[9rem_minmax(0,1fr)_minmax(0,1fr)] md:gap-x-4">
        <span class="text-label-large text-on-surface-variant md:pt-4">{{ t('book.series') }}</span>
        <div class="min-w-0">
          <series-complete-input v-model="seriesCopy" />
        </div>
        <div class="flex min-h-14 items-start gap-1 rounded-md bg-surface-container-highest py-2 pr-4 pl-1">
          <span
            class="w-10 shrink-0"
            aria-hidden="true"
          />
          <div class="flex min-w-0 flex-1 flex-col py-2">
            <span class="text-label-small text-on-surface-variant md:hidden">{{ t('book_merge.fetched_metadata') }}</span>
            <span class="text-body-large break-words text-on-surface">
              <template v-if="props.metadata.series">{{ props.metadata.series }}<span v-if="props.metadata.numberInSeries">&nbsp;#{{ props.metadata.numberInSeries }}</span></template>
              <template v-else>—</template>
            </span>
          </div>
        </div>
      </div>

      <div class="grid gap-2 py-4 md:grid-cols-[9rem_minmax(0,1fr)_minmax(0,1fr)] md:gap-x-4">
        <span class="text-label-large text-on-surface-variant md:pt-4">{{ t('book.publisher') }}</span>
        <m-text-field
          v-model="book.publisher"
          :aria-label="t('book.publisher')"
        />
        <div class="flex min-h-14 items-start gap-1 rounded-md bg-surface-container-highest py-2 pr-4 pl-1">
          <button
            v-tooltip="t('book_merge.use_value')"
            type="button"
            class="m3-icon-btn"
            :aria-label="`${t('book_merge.use_value')} (${t('book.publisher')})`"
            @click="book.publisher = props.metadata.publisher"
          >
            <i
              class="mdi mdi-arrow-up md:hidden"
              aria-hidden="true"
            />
            <i
              class="mdi mdi-arrow-left hidden md:inline"
              aria-hidden="true"
            />
          </button>
          <div class="flex min-w-0 flex-1 flex-col py-2">
            <span class="text-label-small text-on-surface-variant md:hidden">{{ t('book_merge.fetched_metadata') }}</span>
            <span class="text-body-large break-words text-on-surface">{{ props.metadata.publisher || '—' }}</span>
          </div>
        </div>
      </div>

      <div class="grid gap-2 py-4 md:grid-cols-[9rem_minmax(0,1fr)_minmax(0,1fr)] md:gap-x-4">
        <span class="text-label-large text-on-surface-variant md:pt-4">{{ t('book.published_date') }}</span>
        <m-text-field
          v-model="book.publishedDate"
          :aria-label="t('book.published_date')"
        />
        <div class="flex min-h-14 items-start gap-1 rounded-md bg-surface-container-highest py-2 pr-4 pl-1">
          <button
            v-tooltip="t('book_merge.use_value')"
            type="button"
            class="m3-icon-btn"
            :aria-label="`${t('book_merge.use_value')} (${t('book.published_date')})`"
            @click="book.publishedDate = props.metadata.publishedDate"
          >
            <i
              class="mdi mdi-arrow-up md:hidden"
              aria-hidden="true"
            />
            <i
              class="mdi mdi-arrow-left hidden md:inline"
              aria-hidden="true"
            />
          </button>
          <div class="flex min-w-0 flex-1 flex-col py-2">
            <span class="text-label-small text-on-surface-variant md:hidden">{{ t('book_merge.fetched_metadata') }}</span>
            <span class="text-body-large break-words text-on-surface">{{ props.metadata.publishedDate || '—' }}</span>
          </div>
        </div>
      </div>

      <div class="grid gap-2 py-4 md:grid-cols-[9rem_minmax(0,1fr)_minmax(0,1fr)] md:gap-x-4">
        <span class="text-label-large text-on-surface-variant md:pt-4">{{ t('book.page_count') }}</span>
        <m-text-field
          v-model="book.pageCount"
          type="number"
          :aria-label="t('book.page_count')"
        />
        <div class="flex min-h-14 items-start gap-1 rounded-md bg-surface-container-highest py-2 pr-4 pl-1">
          <button
            v-tooltip="t('book_merge.use_value')"
            type="button"
            class="m3-icon-btn"
            :aria-label="`${t('book_merge.use_value')} (${t('book.page_count')})`"
            @click="book.pageCount = props.metadata.pageCount"
          >
            <i
              class="mdi mdi-arrow-up md:hidden"
              aria-hidden="true"
            />
            <i
              class="mdi mdi-arrow-left hidden md:inline"
              aria-hidden="true"
            />
          </button>
          <div class="flex min-w-0 flex-1 flex-col py-2">
            <span class="text-label-small text-on-surface-variant md:hidden">{{ t('book_merge.fetched_metadata') }}</span>
            <span class="text-body-large break-words text-on-surface tabular-nums">{{ props.metadata.pageCount || '—' }}</span>
          </div>
        </div>
      </div>

      <div class="grid gap-2 py-4 md:grid-cols-[9rem_minmax(0,1fr)_minmax(0,1fr)] md:gap-x-4">
        <span class="text-label-large text-on-surface-variant md:pt-4">{{ t('book.language') }}</span>
        <m-text-field
          v-model="book.language"
          :aria-label="t('book.language')"
        />
        <div class="flex min-h-14 items-start gap-1 rounded-md bg-surface-container-highest py-2 pr-4 pl-1">
          <button
            v-tooltip="t('book_merge.use_value')"
            type="button"
            class="m3-icon-btn"
            :aria-label="`${t('book_merge.use_value')} (${t('book.language')})`"
            @click="book.language = props.metadata.language"
          >
            <i
              class="mdi mdi-arrow-up md:hidden"
              aria-hidden="true"
            />
            <i
              class="mdi mdi-arrow-left hidden md:inline"
              aria-hidden="true"
            />
          </button>
          <div class="flex min-w-0 flex-1 flex-col py-2">
            <span class="text-label-small text-on-surface-variant md:hidden">{{ t('book_merge.fetched_metadata') }}</span>
            <span class="text-body-large break-words text-on-surface">{{ props.metadata.language || '—' }}</span>
          </div>
        </div>
      </div>

      <div class="grid gap-2 py-4 md:grid-cols-[9rem_minmax(0,1fr)_minmax(0,1fr)] md:gap-x-4">
        <span class="text-label-large text-on-surface-variant md:pt-4">{{ t('book.summary') }}</span>
        <m-text-field
          v-model="book.summary"
          multiline
          :rows="6"
          :aria-label="t('book.summary')"
        />
        <div class="flex min-h-14 items-start gap-1 rounded-md bg-surface-container-highest py-2 pr-4 pl-1">
          <button
            v-tooltip="t('book_merge.use_value')"
            type="button"
            class="m3-icon-btn"
            :aria-label="`${t('book_merge.use_value')} (${t('book.summary')})`"
            @click="book.summary = props.metadata.summary"
          >
            <i
              class="mdi mdi-arrow-up md:hidden"
              aria-hidden="true"
            />
            <i
              class="mdi mdi-arrow-left hidden md:inline"
              aria-hidden="true"
            />
          </button>
          <div class="flex min-w-0 flex-1 flex-col py-2">
            <span class="text-label-small text-on-surface-variant md:hidden">{{ t('book_merge.fetched_metadata') }}</span>
            <p class="max-h-60 overflow-y-auto text-body-large break-words whitespace-pre-line text-on-surface">
              {{ props.metadata.summary || '—' }}
            </p>
          </div>
        </div>
      </div>
    </div>

    <h3 class="mt-4 border-b border-outline-variant pb-3 text-title-medium text-on-surface">
      {{ t('book.identifiers') }}
    </h3>
    <div class="divide-y divide-outline-variant">
      <div
        v-for="field in identifierFields"
        :key="field.key"
        class="grid gap-2 py-4 md:grid-cols-[9rem_minmax(0,1fr)_minmax(0,1fr)] md:gap-x-4"
      >
        <span class="text-label-large text-on-surface-variant md:pt-4">{{ field.label }}</span>
        <m-text-field
          v-model="book[field.key]"
          :aria-label="field.label"
        />
        <div class="flex min-h-14 items-start gap-1 rounded-md bg-surface-container-highest py-2 pr-4 pl-1">
          <button
            v-tooltip="t('book_merge.use_value')"
            type="button"
            class="m3-icon-btn"
            :aria-label="`${t('book_merge.use_value')} (${field.label})`"
            @click="book[field.key] = props.metadata[field.key]"
          >
            <i
              class="mdi mdi-arrow-up md:hidden"
              aria-hidden="true"
            />
            <i
              class="mdi mdi-arrow-left hidden md:inline"
              aria-hidden="true"
            />
          </button>
          <div class="flex min-w-0 flex-1 flex-col py-2">
            <span class="text-label-small text-on-surface-variant md:hidden">{{ t('book_merge.fetched_metadata') }}</span>
            <span class="text-body-large break-words text-on-surface">{{ props.metadata[field.key] || '—' }}</span>
          </div>
        </div>
      </div>
    </div>

    <template #actions>
      <button
        type="button"
        class="m3-btn m3-btn--text"
        :disabled="progress"
        @click="discard"
      >
        {{ t('labels.cancel') }}
      </button>
      <button
        type="button"
        class="m3-btn m3-btn--filled"
        :disabled="progress"
        @click="importData"
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
