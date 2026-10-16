<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next"
import { until } from '@vueuse/core'
import { computed, Ref, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import useDates from '../composables/dates'
import { Book, UserBook } from '../model/Book'
import dataService from "../services/DataService"
import { ObjectUtils } from '../utils/ObjectUtils'
import EditBookModal from "./EditBookModal.vue"
import BookCover from './BookCover.vue'

const { t, d } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const props = defineProps<{
  book: Book,
  owned: boolean|null,
  toRead: boolean|null,
  borrowed: boolean|null,
  links: boolean|null,
  bookLink: boolean|null,
  addBook: boolean
}>()

const router = useRouter()
const oruga = useOruga();
console.log(oruga)

const { stringToDate } = useDates()

const getBookIsLoading: Ref<boolean> = ref(false)

const hasExternalLink = computed(() => props.book.amazonId != null
  || props.book.goodreadsId != null
  || props.book.googleId != null
  || props.book.librarythingId != null
  || props.book.inventaireId != null
  || props.book.isfdbId != null
  || props.book.noosfereId != null
  || props.book.openlibraryId != null)

const userbookId: Ref<string|null> = ref(null)
const bookCanBeAdded: Ref<boolean> = ref(false)

const getUserbookId = async () => {
  bookCanBeAdded.value = false
  await until(() => props.book.id).not.toBeNull()
  console.log("book id " + props.book.id)
  dataService.findUserBookByCriteria(null, props.book.id, null, null, null, null, 0, 10)
  .then(res => {
    if (!res.empty) {
      if (res.content.length > 0 && res.content[0].id != null) {
        userbookId.value = res.content[0].id
      } else {
        bookCanBeAdded.value = true
      }
    } else {
      bookCanBeAdded.value = true
    }
  })
}

const publisherQuery = computed(() => {
  if (props.book.publisher) {
    return "\"" + props.book.publisher + "\""
  }
  return ""
})

watch(() => props.book.id, (newVal, oldVal) => {
  console.log("props.book.id ")
  console.log(newVal + " " + oldVal)
  bookCanBeAdded.value = false
  if (props.addBook && newVal !== oldVal && props.book.id != null) {
    getUserbookId()
  }
})

const toggleEdit = (book: UserBook) => {
  if (book.id == null) {
    console.log("book")
    console.log(book)
    oruga.modal.open({
            component: EditBookModal,
            trapFocus: true,
            active: true,
            canCancel: ['x', 'button', 'outside'],
            scroll: 'clip',
            props: {
              "book" : book,
              canAddEvent: true
            },
            onClose: modalClosed
          });
  }
}

function modalClosed() {
  console.log("modal closed from data card")
  router.push({ name: 'book-reviews', params: { bookId: props.book.id } })
}

const getIsbn = (): string|null => {
  if (props.book.isbn13 && props.book.isbn13.length > 0) {
    return props.book.isbn13.replaceAll("-", "")
  }
  if (props.book.isbn10 && props.book.isbn10.length > 0) {
    return props.book.isbn10.replaceAll("-", "")
  }
  return null
}

const hasMeta = computed(() => (props.book.translators != null && props.book.translators.length > 0)
  || (props.book.narrators != null && props.book.narrators.length > 0)
  || (props.book.series != null && props.book.series.length > 0)
  || props.book.publisher
  || props.book.publishedDate
  || props.book.pageCount
  || props.book.isbn10
  || props.book.isbn13
  || props.book.language)

</script>

<template>
  <div>
    <div class="grid gap-6 pt-2 md:grid-cols-[minmax(0,17rem)_minmax(0,1fr)] md:gap-10">
      <div class="mx-auto w-full max-w-[15rem] md:max-w-none">
        <book-cover
          :image="props.book.image"
          :title="props.book.title"
          rounded
          elevated
        />
      </div>
      <div class="flex min-w-0 flex-col gap-5">
        <div
          v-if="!props.book.title"
          class="flex flex-col gap-3"
          aria-hidden="true"
        >
          <div class="m3-skeleton h-10 w-2/3 rounded-sm" />
          <div class="m3-skeleton h-6 w-1/3 rounded-sm" />
        </div>
        <div
          v-else
          class="flex flex-col gap-2"
        >
          <h1 class="text-headline-medium text-on-surface md:text-headline-large">
            <router-link
              v-if="props.bookLink != null && props.bookLink === true && userbookId != null"
              class="rounded-xs text-on-surface decoration-2 underline-offset-4 hover:underline"
              :to="{ name: 'book-detail', params: { bookId: userbookId } }"
            >
              {{ props.book.title }}
            </router-link>
            <template v-else>
              {{ props.book.title }}
            </template>
          </h1>
          <p
            v-if="props.book.originalTitle"
            class="text-title-medium text-on-surface-variant"
          >
            {{ props.book.originalTitle }}
          </p>
          <p
            v-if="props.book.authors != null && props.book.authors.length > 0"
            class="text-title-medium text-on-surface-variant"
          >
            <template
              v-for="(author, index) in props.book.authors"
              :key="author.id"
            >
              <router-link
                v-if="props.links === true"
                class="m3-link"
                :to="{ name: 'author-detail', params: { authorId: author.id } }"
              >
                {{ author.name }}
              </router-link>
              <span v-else>{{ author.name }}</span>
              <span v-if="index < props.book.authors.length - 1">, </span>
            </template>
          </p>
        </div>

        <div
          v-if="props.owned || props.toRead || props.borrowed"
          class="flex flex-wrap gap-2"
        >
          <span
            v-if="props.owned"
            class="m3-status m3-status--secondary"
          ><i
            class="mdi mdi-bookshelf"
            aria-hidden="true"
          /><span>{{ t('book.owned') }}</span></span>
          <span
            v-if="props.toRead"
            class="m3-status m3-status--secondary"
          ><i
            class="mdi mdi-bookmark-outline"
            aria-hidden="true"
          /><span>{{ t('book.to_read') }}</span></span>
          <span
            v-if="props.borrowed"
            class="m3-status m3-status--secondary"
          ><i
            class="mdi mdi-hand-extended-outline"
            aria-hidden="true"
          /><span>{{ t('book.borrowed') }}</span></span>
        </div>

        <div
          v-if="props.addBook && bookCanBeAdded"
          class="flex flex-wrap items-center gap-2"
        >
          <button
            v-tooltip="t('labels.add_to_my_books')"
            type="button"
            class="m3-btn m3-btn--filled"
            @click="toggleEdit(ObjectUtils.toUserBook(props.book))"
          >
            <i
              class="mdi mdi-plus"
              aria-hidden="true"
            />
            <span>{{ t('labels.add') }}</span>
          </button>
        </div>

        <dl
          v-if="hasMeta"
          class="m3-meta"
        >
          <template v-if="props.book.translators != null && props.book.translators.length > 0">
            <dt>{{ t('book.translator', props.book.translators.length) }}</dt>
            <dd>
              <template
                v-for="(translator, index) in props.book.translators"
                :key="translator.id"
              >
                <router-link
                  v-if="props.links === true"
                  class="m3-link"
                  :to="{ name: 'author-detail', params: { authorId: translator.id } }"
                >
                  {{ translator.name }}
                </router-link>
                <span v-else>{{ translator.name }}</span>
                <span v-if="index < props.book.translators.length - 1">, </span>
              </template>
            </dd>
          </template>
          <template v-if="props.book.narrators != null && props.book.narrators.length > 0">
            <dt>{{ t('book.narrator', props.book.narrators.length) }}</dt>
            <dd>
              <template
                v-for="(narrator, index) in props.book.narrators"
                :key="narrator.id"
              >
                <router-link
                  v-if="props.links === true"
                  class="m3-link"
                  :to="{ name: 'author-detail', params: { authorId: narrator.id } }"
                >
                  {{ narrator.name }}
                </router-link>
                <span v-else>{{ narrator.name }}</span>
                <span v-if="index < props.book.narrators.length - 1">, </span>
              </template>
            </dd>
          </template>
          <template v-if="props.book.series != null && props.book.series.length > 0">
            <dt>{{ t('book.series') }}</dt>
            <dd>
              <template
                v-for="(seriesItem, index) in props.book.series"
                :key="seriesItem.seriesId"
              >
                <router-link
                  v-if="props.links === true"
                  class="m3-link"
                  :to="{ name: 'series', params: { seriesId: seriesItem.seriesId } }"
                >
                  {{ seriesItem.name }}<span v-if="seriesItem.numberInSeries">&nbsp;#{{ seriesItem.numberInSeries }}</span>
                </router-link>
                <span v-else>{{ seriesItem.name }}<span v-if="seriesItem.numberInSeries">&nbsp;#{{ seriesItem.numberInSeries }}</span></span>
                <span v-if="index < props.book.series.length - 1">, </span>
              </template>
            </dd>
          </template>
          <template v-if="props.book.publisher">
            <dt>{{ t('book.publisher') }}</dt>
            <dd>
              <router-link
                v-if="props.links === true"
                class="m3-link"
                :to="{ name: 'search', query: { q: `publisher:` + publisherQuery } }"
              >
                {{ props.book.publisher }}
              </router-link>
              <template v-else>
                {{ props.book.publisher }}
              </template>
            </dd>
          </template>
          <template v-if="props.book.publishedDate">
            <dt>{{ t('book.published_date') }}</dt>
            <dd>{{ d(stringToDate(props.book.publishedDate)!!, 'short') }}</dd>
          </template>
          <template v-if="props.book.pageCount">
            <dt>{{ t('book.page', 2) }}</dt>
            <dd>{{ props.book.pageCount }}</dd>
          </template>
          <template v-if="props.book.isbn10">
            <dt>{{ t('book.isbn10') }}</dt>
            <dd class="tabular-nums">
              {{ props.book.isbn10 }}
            </dd>
          </template>
          <template v-if="props.book.isbn13">
            <dt>{{ t('book.isbn13') }}</dt>
            <dd class="tabular-nums">
              {{ props.book.isbn13 }}
            </dd>
          </template>
          <template v-if="props.book.language">
            <dt>{{ t('book.language') }}</dt>
            <dd>{{ props.book.language }}</dd>
          </template>
        </dl>

        <div
          v-if="props.book.tags != null && props.book.tags.length > 0"
          class="m3-chip-set"
        >
          <template
            v-for="tag in props.book.tags"
            :key="tag.id"
          >
            <router-link
              v-if="props.links === true"
              class="m3-chip m3-chip--assist"
              :to="{ name: 'tag-detail', params: { tagId: tag.id } }"
            >
              <i
                class="mdi mdi-tag-outline"
                aria-hidden="true"
              />
              <span>{{ tag.name }}</span>
            </router-link>
            <span
              v-else
              class="m3-chip m3-chip--assist m3-chip--static"
            >
              <i
                class="mdi mdi-tag-outline"
                aria-hidden="true"
              />
              <span>{{ tag.name }}</span>
            </span>
          </template>
        </div>

        <div
          v-if="hasExternalLink"
          class="m3-chip-set"
        >
          <a
            v-if="props.book.goodreadsId"
            class="m3-chip"
            :href="'https://www.goodreads.com/book/show/' + props.book.goodreadsId"
            target="_blank"
            rel="noopener"
          ><span>Goodreads</span><i
            class="mdi mdi-open-in-new"
            aria-hidden="true"
          /></a>
          <a
            v-if="props.book.googleId"
            class="m3-chip"
            :href="'https://books.google.com/books?id=' + props.book.googleId"
            target="_blank"
            rel="noopener"
          ><span>Google Books</span><i
            class="mdi mdi-open-in-new"
            aria-hidden="true"
          /></a>
          <a
            v-if="props.book.amazonId"
            class="m3-chip"
            :href="'https://www.amazon.com/dp/' + props.book.amazonId"
            target="_blank"
            rel="noopener"
          ><span>Amazon</span><i
            class="mdi mdi-open-in-new"
            aria-hidden="true"
          /></a>
          <a
            v-if="props.book.librarythingId"
            class="m3-chip"
            :href="'https://www.librarything.com/work/' + props.book.librarythingId"
            target="_blank"
            rel="noopener"
          ><span>LibraryThing</span><i
            class="mdi mdi-open-in-new"
            aria-hidden="true"
          /></a>
          <a
            v-if="props.book.isfdbId"
            class="m3-chip"
            :href="'https://www.isfdb.org/cgi-bin/title.cgi?' + props.book.isfdbId"
            target="_blank"
            rel="noopener"
          ><span>ISFDB</span><i
            class="mdi mdi-open-in-new"
            aria-hidden="true"
          /></a>
          <a
            v-if="props.book.openlibraryId"
            class="m3-chip"
            :href="`https://openlibrary.org/works/${props.book.openlibraryId}?mode=all`"
            target="_blank"
            rel="noopener"
          ><span>Open Library</span><i
            class="mdi mdi-open-in-new"
            aria-hidden="true"
          /></a>
          <a
            v-if="props.book.noosfereId"
            class="m3-chip"
            :href="'https://www.noosfere.org/livres/EditionsLivre.asp?numitem=' + props.book.noosfereId"
            target="_blank"
            rel="noopener"
          ><span>Noosfere</span><i
            class="mdi mdi-open-in-new"
            aria-hidden="true"
          /></a>
          <a
            v-if="getIsbn() != null"
            class="m3-chip"
            :href="'https://inventaire.io/entity/isbn:' + getIsbn()"
            target="_blank"
            rel="noopener"
          ><span>Inventaire</span><i
            class="mdi mdi-open-in-new"
            aria-hidden="true"
          /></a>
          <a
            v-else-if="props.book.inventaireId"
            class="m3-chip"
            :href="'https://inventaire.io/entity/inv:' + props.book.inventaireId"
            target="_blank"
            rel="noopener"
          ><span>Inventaire</span><i
            class="mdi mdi-open-in-new"
            aria-hidden="true"
          /></a>
        </div>
      </div>
    </div>

    <section
      v-if="props.book.summary"
      class="m3-section"
    >
      <h2 class="m3-section__title">
        {{ t('book.summary') }}
      </h2>
      <div
        class="prose max-w-none text-body-large"
        v-html="props.book.summary"
      />
    </section>
    <o-loading
      v-model:active="getBookIsLoading"
      :full-page="true"
      :cancelable="true"
    />
  </div>
</template>
