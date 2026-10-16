<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next"
import { until, useClipboard, useLocalStorage, usePermission, useTitle } from '@vueuse/core'
import dayjs from 'dayjs'
import { computed, ComputedRef, Ref, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import useDates from '../composables/dates'
import { Book, UserBook } from '../model/Book'
import { BookQuote } from "../model/BookQuote"
import { Metadata } from "../model/Metadata"
import { CreateReadingEvent, ReadingEvent, ReadingEventType } from '../model/ReadingEvent'
import { Review } from '../model/Review'
import { Series } from '../model/Series'
import { User } from '../model/User'
import dataService from "../services/DataService"
import { key } from '../store'
import { ObjectUtils } from '../utils/ObjectUtils'
import AutoImportFormModalVue from "./AutoImportFormModal.vue"
import BookQuoteCard from "./BookQuoteCard.vue"
import BookQuoteModalVue from './BookQuoteModal.vue'
import EditBookModal from "./EditBookModal.vue"
import MergeBookModal from './MergeBookModal.vue'
import ReadingEventModalVue from './ReadingEventModal.vue'
import ReadProgressModal from './ReadProgressModal.vue'
import ReviewCard from "./ReviewCard.vue"
import ReviewModalVue from './ReviewModal.vue'
import useEvents from '../composables/events'
import BookCover from './BookCover.vue'
import MAvatar from './m3/MAvatar.vue'
import MDialog from './m3/MDialog.vue'
import MMenu from './m3/MMenu.vue'
import MModal from './m3/MModal.vue'
import ReadingStatus from './ReadingStatus.vue'


const { t, d } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const { isSupported, copy } = useClipboard()
usePermission('clipboard-read')
usePermission('clipboard-write')

const props = defineProps<{ bookId: string }>()

const store = useStore(key)
const router = useRouter()
const oruga = useOruga();

const { stringToDate } = useDates()

const isAdmin = computed(() => {
  return store !== undefined && store.getters.isAdmin
})
const user: ComputedRef<User> = computed(() => {
  return store !== undefined && store.getters.getUser
})

let currency = localStorage.getItem("JL_CURRENCY")
if (currency == null) {
  currency = "EUR"
}

const book: Ref<UserBook | null> = ref(null)
const edit: Ref<boolean> = ref(false)
const showModal: Ref<boolean> = ref(false)

const getBookIsLoading: Ref<boolean> = ref(false)

const userReviews: Ref<Array<Review>> = ref([])

const bookQuotes: Ref<Array<BookQuote>> = ref([])
const users: Ref<Array<User>> = ref([])

const getBook = async () => {
  try {
    getBookIsLoading.value = true
    book.value = await dataService.getUserBookById(props.bookId)
    getBookIsLoading.value = false
    useTitle('Jelu | ' + book.value.book.title)
    getUserReviewsForBook()
    getBookQuotesForBook()
    getAllSeriesInfo()
    getBookUsers()
  } catch (error) {
    console.log("failed get book : " + error);
    getBookIsLoading.value = false
  }
};

const getBookUsers = async () => {
  await until(book.value).not.toBeNull()
  if (book.value?.book.id != null) {
    dataService.findBookUsers(book.value?.book.id, 0, 20, "login,desc")
      .then(res => {
        console.log(res)
        users.value = res.content
      })
      .catch(err => {
        console.log(err)
      })
  }
}

const getAllSeriesInfo = async () => {
  book.value?.book.series?.forEach(s => {
    fetchSeries(s.seriesId as string)
  })
}

const getUserReviewsForBook = async() => {
  await until(user.value).not.toBeNull()
  dataService.findReviews(user.value.id, book.value?.book.id, null, null, null, 0, 20)
  .then(res => {
    console.log(res)
    userReviews.value = res.content
  })
  .catch(err => {
    console.log(err)
  })
}

const getBookQuotesForBook = async() => {
  await until(user.value).not.toBeNull()
  dataService.findBookQuotes(user.value.id, book.value?.book.id, null, 0, 20)
  .then(res => {
    console.log(res)
    bookQuotes.value = res.content
  })
  .catch(err => {
    console.log(err)
  })
}

watch(() => props.bookId, (newValue, oldValue) => {
  console.log('The new bookId is: ' + props.bookId)
})

const sortedEvents = computed(() => {
  if (book.value && book.value.readingEvents) {
    return [...book.value.readingEvents].sort((a, b) => dayjs(a.startDate).isAfter(dayjs(b.startDate)) ? -1 : 1)
  }
  else {
    return []
  }
}
)

const hasExternalLink = computed(() => book.value?.book.amazonId != null
  || book.value?.book.goodreadsId != null
  || book.value?.book.googleId != null
  || book.value?.book.librarythingId != null
  || book.value?.book.openlibraryId != null
  || book.value?.book.isfdbId != null
  || book.value?.book.noosfereId != null
  || book.value?.book.inventaireId != null)

function modalClosed() {
  console.log("modal closed")
  currentTimestamp = ObjectUtils.timestamp()
  getBook()
}

function reviewModalClosed() {
  console.log("review modal closed")
  getUserReviewsForBook()
}

function bookQuoteModalClosed() {
  console.log("book quote modal closed")
    getBookQuotesForBook()
}

const toggleEdit = () => {
  edit.value = !edit.value
  oruga.modal.open({
    parent: this,
    component: EditBookModal,
    trapFocus: true,
    active: true,
    canCancel: ['x', 'button', 'outside'],
    scroll: 'clip',
    props: {
      "book": book.value,
      canAddEvent: false
    },
    onClose: modalClosed
  });
}

function toggleReadingEventModal(currentEvent: ReadingEvent, edit: boolean) {
  showModal.value = !showModal.value
  oruga.modal.open({
    component: ReadingEventModalVue,
    trapFocus: true,
    active: true,
    canCancel: ['x', 'button', 'outside'],
    scroll: 'keep',
    props: {
      "readingEvent": currentEvent,
      "edit": edit
    },
    onClose: modalClosed
  });
}

function toggleReviewModal(currentBook: Book|undefined, edit: boolean, review: Review|null) {
  if (currentBook != null && currentBook != undefined) {
    oruga.modal.open({
      component: ReviewModalVue,
      trapFocus: true,
      active: true,
      canCancel: ['x', 'button', 'outside'],
      scroll: 'keep',
      props: {
        "book": currentBook,
        "edit" : edit,
        "review": review
      },
      onClose: reviewModalClosed
    });
  }
}

function toggleBookQuoteModal(currentBook: Book|undefined, edit: boolean, bookQuote: BookQuote|null) {
  if (currentBook != null && currentBook != undefined) {
    oruga.modal.open({
      component: BookQuoteModalVue,
      trapFocus: true,
      active: true,
      canCancel: ['x', 'button', 'outside'],
      scroll: 'keep',
      props: {
        "book": currentBook,
        "edit" : edit,
        "bookQuote": bookQuote
      },
      onClose: bookQuoteModalClosed
    });
  }
}

const toggleFetchMetadataModal = (currentBook: Book|undefined) => {
  oruga.modal.open({
    parent: this,
    component: AutoImportFormModalVue,
    trapFocus: true,
    active: true,
    canCancel: ['x', 'button', 'outside'],
    scroll: 'keep',
    props: {
        "book": currentBook,
      },
    events: {
      metadataReceived: (modalMetadata: Metadata) => {
        console.log("received metadata")
        console.log(modalMetadata)
        toggleMergeBookModal(currentBook, modalMetadata)
      }
    },
    onClose: modalClosed
  });
}

const toggleMergeBookModal = (currentBook: Book|undefined, metadata: Metadata) => {
  oruga.modal.open({
    parent: this,
    component: MergeBookModal,
    trapFocus: true,
    active: true,
    canCancel: ['x', 'button', 'outside'],
    scroll: 'keep',
    props: {
        "book": currentBook,
        "metadata": metadata
      },
    onClose: modalClosed
  });
}

const toggleReadProgressModal = (userBookId: string, pageCount: number|null, currentProgress: number|null, currentPage: number|null) => {
  oruga.modal.open({
    component: ReadProgressModal,
    trapFocus: true,
    active: true,
    canCancel: ['x', 'button', 'outside'],
    scroll: 'keep',
    props: {
      "userBookId": userBookId,
      "pageCount": pageCount,
      "currentProgress": currentProgress,
      "currentPage": currentPage,
    },
    onClose: modalClosed
  });
}

const deleteBook = async () => {
  let deleteForUserOnly = true
  let abort = false
  if (isAdmin.value === true) {
    await ObjectUtils.swalMixin.fire({
      html: `<p>${t('labels.delete_for_all_or_only_you')}</p>`,
      showDenyButton: true,
      showCancelButton: true,
      confirmButtonText: t('labels.only_me'),
      denyButtonText: t('labels.all_users'),
      cancelButtonText: t('labels.dont_delete'),
    }).then((result) => {
      if (result.isDenied) {
        deleteForUserOnly = false
      } else if (result.isDismissed) {
        abort = true
        return;
      }
    })
  }
  else {
    await ObjectUtils.swalYesNoMixin.fire({
      html: `<p>${t('labels.delete_this_book')}</p>`,
      showCancelButton: true,
      showConfirmButton: true,
      showDenyButton: false,
      confirmButtonText: t('labels.delete'),
      cancelButtonText: t('labels.dont_delete'),
    }).then((result) => {
      if (result.isDismissed) {
        abort = true
        return;
      }
    })
  }
  if (abort) {
    return
  }
  console.log(`delete for user only ${deleteForUserOnly}`)
  let promise
  if (deleteForUserOnly) {
    if (book.value?.id) {
      promise = dataService.deleteUserBook(book.value?.id)
    }
  }
  else {
    if (book.value?.book?.id) {
      promise = dataService.deleteBook(book.value?.book?.id)
    }
  }
  promise?.then(res => {
    ObjectUtils.toast(oruga, "success", t('labels.book_was_deleted'), 4000);
    router.push({ name: 'home' })
  })
    .catch(err => {
      ObjectUtils.toast(oruga, "danger", t('labels.error_deleting', {msg : err.message}), 4000);
    })
}

const { eventIcon, eventLabel } = useEvents()

// tonal container used behind the icon of a reading event
const eventContainerClass = (type: ReadingEventType) => {
  if (type === ReadingEventType.FINISHED) {
    return 'bg-tertiary-container text-on-tertiary-container'
  } else if (type === ReadingEventType.DROPPED) {
    return 'bg-error-container text-on-error-container'
  } else if (type === ReadingEventType.CURRENTLY_READING) {
    return 'bg-primary-container text-on-primary-container'
  }
  return 'bg-surface-container-highest text-on-surface-variant'
}

const embedOpen = ref(false)

const otherOwners = computed(() => users.value.filter(u => u.id !== user.value?.id))

const progressLabel = computed(() => {
  if (book.value?.currentPageNumber != null && book.value?.book.pageCount != null) {
    return `${book.value.currentPageNumber} / ${book.value.book.pageCount}`
  }
  if (book.value?.percentRead != null) {
    return `${Math.round(book.value.percentRead)} %`
  }
  return ''
})

const showProgress = computed(() => book.value?.lastReadingEvent === ReadingEventType.CURRENTLY_READING
  && book.value?.percentRead != null && book.value.percentRead > 0)

function defaultCreateEvent(): CreateReadingEvent {
  return {
    eventType: ReadingEventType.CURRENTLY_READING,
    eventDate: new Date(),
    startDate: new Date(),
    bookId: book.value?.book.id
  }
}

const publisherQuery = computed(() => {
  if (book.value?.book.publisher) {
    return "\"" + book.value.book.publisher + "\""
  }
  return ""
})

const embedCode = computed(() => {
  if (book.value) {
    return generateEmbed(book.value)
  }
  return ''
})

function generateEmbed(book: UserBook) {
  let baseUrl = window.location.origin
  let bookUrl = router.resolve({ name: 'book-detail', params: { bookId: book.id } }).href
  let top = `<div id="embed-body" style="padding: 5px; width: 150px; border: 1px solid #cccccc;}"><div class="embed-element" style="overflow: hidden;list-style: none; text-align: center; padding: 5px; margin: 0px;">`
  if (book.book.image != null) {
    let couv = `<div class="embed-cover"> <a href="${baseUrl}${bookUrl}" target="_blank"><img src="${baseUrl}/files/${book.book.image}" title="${book.book.title}" alt="${book.book.title}" style="border: 1px solid #cccccc;border-width:1px; padding: 3px; background-color: #fff;width:80px;"></a></div>`
    top = top.concat(couv)
  }
  let body = `<div class="embed-book" style="margin: 0px 3px 5px 5px;font-size: 13px;font-family:sans-serif; font-weight : bold;"><a href="${baseUrl}${bookUrl}" target="_blank" style="text-decoration:none;">${book.book.title}</a></div>`
  top = top.concat(body)
  if (book.book.authors != undefined && book.book.authors?.length > 0) {
      let firstAuthor = book?.book.authors[0]
      let authorId = firstAuthor.id
      let rout = router.resolve({ name: 'author-detail', params: { authorId: authorId } }).href
  let authorPart = `<div class="embed-author" style="margin: 0px 3px 5px 5px;font-size: 12px;color: gray;"><a href="${baseUrl}${rout}" target="_blank" style="text-decoration:none;">${firstAuthor.name}</a></div>`
      top = top.concat(authorPart)
    }
  let bottom = `<div class="embed-tail" style="clear:both;"></div></div></div>`
  top = top.concat(bottom)
  return top
}

function copyToClipboard(content: string) {
  copy(content)
  ObjectUtils.toast(oruga, "success", t('labels.saved'), 1000)

}

const deleteReview = async (reviewId: string) => {
  console.log("delete " + reviewId)
  let abort = false
  await ObjectUtils.swalYesNoMixin.fire({
      html: `<p>${t('reviews.delete_review')}</p>`,
      showCancelButton: true,
      showConfirmButton: true,
      showDenyButton: false,
      confirmButtonText: t('labels.delete'),
      cancelButtonText: t('labels.dont_delete'),
      denyButtonText: t('labels.delete'),
    }).then((result) => {
      if (result.isDismissed) {
        abort = true
        return;
      }
    })
    console.log("abort " + abort)
    if (abort) {
      return
    }
    dataService.deleteReview(reviewId)
    .then(res => {
      getUserReviewsForBook()
    })
    .catch(err => {
      console.log(err)
    })
}

const deleteBookQuote = async (bookQuoteId: string) => {
  console.log("delete " + bookQuoteId)
  let abort = false
  await ObjectUtils.swalYesNoMixin.fire({
      html: `<p>${t('book_quotes.delete_quote')}</p>`,
      showCancelButton: true,
      showConfirmButton: true,
      showDenyButton: false,
      confirmButtonText: t('labels.delete'),
      cancelButtonText: t('labels.dont_delete'),
      denyButtonText: t('labels.delete'),
    }).then((result) => {
      if (result.isDismissed) {
        abort = true
        return;
      }
    })
    console.log("abort " + abort)
    if (abort) {
      return
    }
    dataService.deleteBookQuote(bookQuoteId)
    .then(res => {
      getBookQuotesForBook()
    })
    .catch(err => {
      console.log(err)
    })
}

const seriesmap: Map<string, Series> = new Map()

const fetchSeries = async (seriesId: string) => {
  dataService.getSeriesById(seriesId)
    .then(data => {
        seriesmap.set(seriesId, data)
    })
    .catch(e => {
        console.log("fetching series error")
    })
}

const getSeriesInfo = async (seriesId: string) => {
    if (seriesmap.get(seriesId) != null) {
        const s = seriesmap.get(seriesId)
        return await formatSeries(s as Series)
    }
    dataService.getSeriesById(seriesId)
    .then(data => {
        seriesmap.set(seriesId, data)
        return formatSeries(data)
    })
    .catch(e => {
        return "error"
    })
}

const formatSeries = async (series: Series)  => {
    let txt = ""
    if (series.description != null && series.description.length > 0) {
        txt += series.description.substring(0, 40)
        txt += " | "
    }
    if (series.avgRating != null) {
        txt += "avg : "
        txt += series.avgRating
        txt += " "
    }
    if (series.userRating != null) {
        txt += "me : "
        txt += series.userRating
    }
    if (txt.trim().length < 1) {
      return 'no data'
    }
    return txt
}

let currentTimestamp = ObjectUtils.timestamp()

const getIsbn = (): string|null => {
  if (book.value?.book.isbn13 && book.value.book.isbn13.length > 0) {
    return book.value.book.isbn13.replaceAll("-", "")
  }
  if (book.value?.book.isbn10 && book.value.book.isbn10.length > 0) {
    return book.value.book.isbn10.replaceAll("-", "")
  }
  return null
}

const storedLanguage = useLocalStorage("jelu_language", "en")

getBook()

</script>

<template>
  <div class="m3-page m3-page--medium">
    <div
      v-if="book != null"
      class="grid gap-6 pt-2 md:grid-cols-[minmax(0,17rem)_minmax(0,1fr)] md:gap-10"
    >
      <div class="mx-auto w-full max-w-[15rem] md:max-w-none">
        <book-cover
          :image="book.book.image"
          :title="book.book.title"
          :cache-key="currentTimestamp"
          rounded
          elevated
        />
      </div>
      <div class="flex min-w-0 flex-col gap-5">
        <div class="flex flex-col gap-2">
          <h1 class="text-headline-medium text-on-surface md:text-headline-large">
            {{ book.book.title }}
          </h1>
          <p
            v-if="book.book.originalTitle"
            class="text-title-medium text-on-surface-variant"
          >
            {{ book.book.originalTitle }}
          </p>
          <p
            v-if="book.book.authors != null && book.book.authors.length > 0"
            class="text-title-medium text-on-surface-variant"
          >
            <template
              v-for="(author, index) in book.book.authors"
              :key="author.id"
            >
              <router-link
                class="m3-link"
                :to="{ name: 'author-detail', params: { authorId: author.id } }"
              >
                {{ author.name }}
              </router-link>
              <span v-if="index < book.book.authors.length - 1">, </span>
            </template>
          </p>
        </div>

        <div
          v-if="book.lastReadingEvent || book.owned || book.toRead || book.borrowed"
          class="flex flex-wrap gap-2"
        >
          <reading-status :type="book.lastReadingEvent" />
          <span
            v-if="book.owned"
            class="m3-status m3-status--secondary"
          ><i
            class="mdi mdi-bookshelf"
            aria-hidden="true"
          /><span>{{ t('book.owned') }}</span></span>
          <span
            v-if="book.toRead"
            class="m3-status m3-status--secondary"
          ><i
            class="mdi mdi-bookmark-outline"
            aria-hidden="true"
          /><span>{{ t('book.to_read') }}</span></span>
          <span
            v-if="book.borrowed"
            class="m3-status m3-status--secondary"
          ><i
            class="mdi mdi-hand-extended-outline"
            aria-hidden="true"
          /><span>{{ t('book.borrowed') }}</span></span>
        </div>

        <div
          v-if="showProgress"
          class="flex max-w-md flex-col gap-2"
        >
          <div class="flex items-center justify-between text-label-large text-on-surface-variant">
            <span>{{ t('book.percent_read') }}</span>
            <span class="tabular-nums">{{ progressLabel }}</span>
          </div>
          <progress
            class="m3-linear-progress"
            max="100"
            :value="book.percentRead ?? 0"
          />
        </div>

        <div class="flex flex-wrap items-center gap-2">
          <button
            type="button"
            class="m3-btn m3-btn--filled"
            @click="toggleReadingEventModal(defaultCreateEvent(), false)"
          >
            <i
              class="mdi mdi-calendar-plus"
              aria-hidden="true"
            />
            <span>{{ t('labels.event') }}</span>
          </button>
          <button
            type="button"
            class="m3-btn m3-btn--tonal"
            @click="toggleEdit"
          >
            <i
              class="mdi mdi-pencil-outline"
              aria-hidden="true"
            />
            <span>{{ t('labels.edit') }}</span>
          </button>
          <button
            v-tooltip="t('labels.set_progress')"
            type="button"
            class="m3-icon-btn m3-icon-btn--outlined"
            :aria-label="t('labels.set_progress')"
            @click="toggleReadProgressModal(book.id!!, book.book.pageCount ?? null, book.percentRead ?? null, book.currentPageNumber ?? null)"
          >
            <i
              class="mdi mdi-progress-check"
              aria-hidden="true"
            />
          </button>
          <button
            v-tooltip="t('labels.get_embed_code')"
            type="button"
            class="m3-icon-btn m3-icon-btn--outlined"
            :aria-label="t('labels.get_embed_code')"
            @click="embedOpen = true"
          >
            <i
              class="mdi mdi-share-variant-outline"
              aria-hidden="true"
            />
          </button>
          <m-menu align="start">
            <template #trigger="{ toggle, open }">
              <button
                v-tooltip="t('labels.more_actions')"
                type="button"
                class="m3-icon-btn m3-icon-btn--outlined"
                :aria-label="t('labels.more_actions')"
                :aria-expanded="open"
                aria-haspopup="menu"
                @click="toggle"
              >
                <i
                  class="mdi mdi-dots-vertical"
                  aria-hidden="true"
                />
              </button>
            </template>
            <button
              type="button"
              class="m3-menu-item"
              role="menuitem"
              @click="toggleReviewModal(book?.book, false, null)"
            >
              <i
                class="mdi mdi-star-outline"
                aria-hidden="true"
              />{{ t('reviews.create_review') }}
            </button>
            <button
              type="button"
              class="m3-menu-item"
              role="menuitem"
              @click="toggleBookQuoteModal(book?.book, false, null)"
            >
              <i
                class="mdi mdi-format-quote-open-outline"
                aria-hidden="true"
              />{{ t('labels.add_quote') }}
            </button>
            <button
              type="button"
              class="m3-menu-item"
              role="menuitem"
              @click="toggleFetchMetadataModal(book?.book)"
            >
              <i
                class="mdi mdi-cloud-download-outline"
                aria-hidden="true"
              />{{ t('book_merge.fetch_metadata') }}
            </button>
            <hr class="m3-divider">
            <button
              type="button"
              class="m3-menu-item m3-menu-item--error"
              role="menuitem"
              @click="deleteBook"
            >
              <i
                class="mdi mdi-delete-outline"
                aria-hidden="true"
              />{{ t('labels.delete') }}
            </button>
          </m-menu>
        </div>

        <dl class="m3-meta">
          <template v-if="book.book.translators != null && book.book.translators.length > 0">
            <dt>{{ t('book.translator', book.book.translators.length) }}</dt>
            <dd>
              <template
                v-for="(translator, index) in book.book.translators"
                :key="translator.id"
              >
                <router-link
                  class="m3-link"
                  :to="{ name: 'author-detail', params: { authorId: translator.id } }"
                >
                  {{ translator.name }}
                </router-link>
                <span v-if="index < book.book.translators.length - 1">, </span>
              </template>
            </dd>
          </template>
          <template v-if="book.book.narrators != null && book.book.narrators.length > 0">
            <dt>{{ t('book.narrator', book.book.narrators.length) }}</dt>
            <dd>
              <template
                v-for="(narrator, index) in book.book.narrators"
                :key="narrator.id"
              >
                <router-link
                  class="m3-link"
                  :to="{ name: 'author-detail', params: { authorId: narrator.id } }"
                >
                  {{ narrator.name }}
                </router-link>
                <span v-if="index < book.book.narrators.length - 1">, </span>
              </template>
            </dd>
          </template>
          <template v-if="book.book.series != null && book.book.series.length > 0">
            <dt>{{ t('book.series') }}</dt>
            <dd>
              <template
                v-for="(seriesItem, index) in book.book.series"
                :key="seriesItem.seriesId"
              >
                <router-link
                  v-tooltip="{ content: () => getSeriesInfo(seriesItem.seriesId as string) }"
                  class="m3-link"
                  :to="{ name: 'series', params: { seriesId: seriesItem.seriesId } }"
                >
                  {{ seriesItem.name }}<span v-if="seriesItem.numberInSeries">&nbsp;#{{ seriesItem.numberInSeries }}</span>
                </router-link>
                <span v-if="index < book.book.series.length - 1">, </span>
              </template>
            </dd>
          </template>
          <template v-if="book.book.publisher">
            <dt>{{ t('book.publisher') }}</dt>
            <dd>
              <router-link
                class="m3-link"
                :to="{ name: 'search', query: { q: `publisher:` + publisherQuery } }"
              >
                {{ book.book.publisher }}
              </router-link>
            </dd>
          </template>
          <template v-if="book.book.publishedDate">
            <dt>{{ t('book.published_date') }}</dt>
            <dd>{{ d(stringToDate(book.book.publishedDate)!!, 'short') }}</dd>
          </template>
          <template v-if="book.book.pageCount">
            <dt>{{ t('book.page', 2) }}</dt>
            <dd>
              {{ book.book.pageCount }}<span
                v-if="book.currentPageNumber"
                class="text-on-surface-variant"
              > ({{ t('labels.current') }}&nbsp;: {{ book.currentPageNumber }})</span>
            </dd>
          </template>
          <template v-else-if="book.currentPageNumber">
            <dt>{{ t('book.current_page_number') }}</dt>
            <dd>{{ book.currentPageNumber }}</dd>
          </template>
          <template v-if="book.book.pageCount == null && book.currentPageNumber == null && book.percentRead != null">
            <dt>{{ t('book.percent_read') }}</dt>
            <dd>{{ book.percentRead }} %</dd>
          </template>
          <template v-if="book.book.isbn10">
            <dt>{{ t('book.isbn10') }}</dt>
            <dd class="tabular-nums">
              {{ book.book.isbn10 }}
            </dd>
          </template>
          <template v-if="book.book.isbn13">
            <dt>{{ t('book.isbn13') }}</dt>
            <dd class="tabular-nums">
              {{ book.book.isbn13 }}
            </dd>
          </template>
          <template v-if="book.book.language">
            <dt>{{ t('book.language') }}</dt>
            <dd>{{ book.book.language }}</dd>
          </template>
          <template v-if="book.price">
            <dt>{{ t('book.price') }}</dt>
            <dd>{{ ObjectUtils.amountInLocale(book.price, storedLanguage, currency) }}</dd>
          </template>
        </dl>

        <div
          v-if="book.book.tags != null && book.book.tags.length > 0"
          class="m3-chip-set"
        >
          <router-link
            v-for="tag in book.book.tags"
            :key="tag.id"
            class="m3-chip m3-chip--assist"
            :to="{ name: 'tag-detail', params: { tagId: tag.id } }"
          >
            <i
              class="mdi mdi-tag-outline"
              aria-hidden="true"
            />
            <span>{{ tag.name }}</span>
          </router-link>
        </div>

        <div
          v-if="hasExternalLink || getIsbn() != null"
          class="m3-chip-set"
        >
          <a
            v-if="book.book.goodreadsId"
            class="m3-chip"
            :href="'https://www.goodreads.com/book/show/' + book.book.goodreadsId"
            target="_blank"
            rel="noopener"
          ><span>Goodreads</span><i
            class="mdi mdi-open-in-new"
            aria-hidden="true"
          /></a>
          <a
            v-if="book.book.googleId"
            class="m3-chip"
            :href="'https://books.google.com/books?id=' + book.book.googleId"
            target="_blank"
            rel="noopener"
          ><span>Google Books</span><i
            class="mdi mdi-open-in-new"
            aria-hidden="true"
          /></a>
          <a
            v-if="book.book.amazonId"
            class="m3-chip"
            :href="'https://www.amazon.com/dp/' + book.book.amazonId"
            target="_blank"
            rel="noopener"
          ><span>Amazon</span><i
            class="mdi mdi-open-in-new"
            aria-hidden="true"
          /></a>
          <a
            v-if="book.book.librarythingId"
            class="m3-chip"
            :href="'https://www.librarything.com/work/' + book.book.librarythingId"
            target="_blank"
            rel="noopener"
          ><span>LibraryThing</span><i
            class="mdi mdi-open-in-new"
            aria-hidden="true"
          /></a>
          <a
            v-if="book.book.isfdbId"
            class="m3-chip"
            :href="'https://www.isfdb.org/cgi-bin/title.cgi?' + book.book.isfdbId"
            target="_blank"
            rel="noopener"
          ><span>ISFDB</span><i
            class="mdi mdi-open-in-new"
            aria-hidden="true"
          /></a>
          <a
            v-if="book.book.openlibraryId"
            class="m3-chip"
            :href="`https://openlibrary.org/works/${book.book.openlibraryId}?mode=all`"
            target="_blank"
            rel="noopener"
          ><span>Open Library</span><i
            class="mdi mdi-open-in-new"
            aria-hidden="true"
          /></a>
          <a
            v-if="book.book.noosfereId"
            class="m3-chip"
            :href="'https://www.noosfere.org/livres/EditionsLivre.asp?numitem=' + book.book.noosfereId"
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
            v-else-if="book.book.inventaireId"
            class="m3-chip"
            :href="'https://inventaire.io/entity/inv:' + book.book.inventaireId"
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
      v-if="book?.book?.summary"
      class="m3-section"
    >
      <h2 class="m3-section__title">
        {{ t('book.summary') }}
      </h2>
      <div
        class="prose max-w-none text-body-large"
        v-html="book.book.summary"
      />
    </section>

    <section
      v-if="book?.personalNotes"
      class="m3-section"
    >
      <h2 class="m3-section__title">
        {{ t('book.personal_notes') }}
      </h2>
      <p class="whitespace-pre-line text-body-large text-on-surface">
        {{ book.personalNotes }}
      </p>
    </section>

    <section
      v-if="book != null && sortedEvents.length > 0"
      class="m3-section"
    >
      <h2 class="m3-section__title">
        {{ t('reading_events.reading_events') }}
      </h2>
      <ol class="m3-card m3-card--outlined m3-list">
        <li
          v-for="event in sortedEvents"
          :key="event.id"
          class="m3-list-item m3-list-item--two-line"
        >
          <span
            class="m3-list-item__leading h-10 w-10 rounded-full"
            :class="eventContainerClass(event.eventType)"
          >
            <i
              class="mdi text-[1.25rem]"
              :class="eventIcon(event.eventType)"
              aria-hidden="true"
            />
          </span>
          <div class="m3-list-item__content">
            <span class="m3-list-item__headline">{{ eventLabel(event.eventType) }}</span>
            <span class="m3-list-item__supporting">
              <template v-if="event.startDate">{{ t('reading_events.started') }}&nbsp;{{ d(event.startDate, 'short') }}</template>
              <template v-if="event.startDate && event.endDate"> · </template>
              <template v-if="event.endDate">{{ eventLabel(event.eventType) }}&nbsp;{{ d(event.endDate, 'short') }}</template>
            </span>
          </div>
          <div class="m3-list-item__trailing">
            <button
              v-tooltip="t('reading_events.edit_event')"
              type="button"
              class="m3-icon-btn"
              :aria-label="t('reading_events.edit_event')"
              @click="toggleReadingEventModal(event, true)"
            >
              <i
                class="mdi mdi-pencil-outline"
                aria-hidden="true"
              />
            </button>
          </div>
        </li>
      </ol>
    </section>

    <section
      v-if="book != null"
      class="m3-section"
    >
      <h2 class="m3-section__title">
        <span>{{ t('reviews.my_reviews') }}</span>
        <router-link
          class="m3-btn m3-btn--text"
          :to="{ name: 'book-reviews', params: { bookId: book.book.id } }"
        >
          <span>{{ t('reviews.all_reviews') }}</span>
          <i
            class="mdi mdi-arrow-right"
            aria-hidden="true"
          />
        </router-link>
      </h2>
      <div
        v-if="userReviews.length > 0"
        class="grid gap-4 lg:grid-cols-2"
      >
        <review-card
          v-for="review in userReviews"
          :key="review.id"
          :review="review"
          :show-delete="true"
          :show-edit="true"
          @update:delete="deleteReview($event)"
          @update:edit="toggleReviewModal(book?.book, true, review)"
        />
      </div>
      <button
        v-else
        type="button"
        class="m3-btn m3-btn--tonal"
        @click="toggleReviewModal(book?.book, false, null)"
      >
        <i
          class="mdi mdi-star-outline"
          aria-hidden="true"
        />
        <span>{{ t('reviews.create_review') }}</span>
      </button>
    </section>

    <section
      v-if="book != null && bookQuotes.length > 0"
      class="m3-section"
    >
      <h2 class="m3-section__title">
        <span>{{ t('book_quotes.quote', 2) }}</span>
        <router-link
          class="m3-btn m3-btn--text"
          :to="{ name: 'book-quotes', params: { bookId: book.book.id } }"
        >
          <span>{{ t('book_quotes.quote', 2) }}</span>
          <i
            class="mdi mdi-arrow-right"
            aria-hidden="true"
          />
        </router-link>
      </h2>
      <div class="grid gap-4 lg:grid-cols-2">
        <book-quote-card
          v-for="quote in bookQuotes"
          :key="quote.id"
          :book-quote="quote"
          :show-delete="true"
          :show-edit="true"
          @update:delete="deleteBookQuote($event)"
          @update:edit="toggleBookQuoteModal(book?.book, true, quote)"
        />
      </div>
    </section>

    <section
      v-if="otherOwners.length > 0"
      class="m3-section"
    >
      <h2 class="m3-section__title">
        {{ t('labels.other_owners') }}
      </h2>
      <div class="m3-chip-set">
        <router-link
          v-for="ppl in otherOwners"
          :key="ppl.id"
          class="m3-chip m3-chip--assist"
          :to="{ name: 'user-detail', params: { userId: ppl.id } }"
        >
          <m-avatar
            :name="ppl.login"
            size="sm"
          />
          <span>{{ ppl.login }}</span>
        </router-link>
      </div>
    </section>
  </div>
  <o-loading
    v-model:active="getBookIsLoading"
    :full-page="true"
    :cancelable="true"
  />
  <m-modal v-model="embedOpen">
    <m-dialog
      :headline="t('labels.get_embed_code')"
      icon="code-tags"
      size="lg"
      @close="embedOpen = false"
    >
      <p class="mb-3">
        {{ t('labels.copy_paste_code') }}
      </p>
      <pre class="max-h-48 overflow-auto rounded-md bg-surface-container-highest p-4 font-mono text-body-small whitespace-pre-wrap break-all text-on-surface"><code>{{ embedCode }}</code></pre>
      <h3 class="mt-6 mb-3 text-title-small text-on-surface">
        {{ t('labels.preview') }}
      </h3>
      <div
        class="inline-block rounded-md bg-white p-2"
        v-html="embedCode"
      />
      <template #actions>
        <button
          type="button"
          class="m3-btn m3-btn--text"
          @click="embedOpen = false"
        >
          {{ t('labels.close') }}
        </button>
        <button
          v-if="isSupported"
          type="button"
          class="m3-btn m3-btn--filled"
          @click="copyToClipboard(embedCode)"
        >
          <i
            class="mdi mdi-content-copy"
            aria-hidden="true"
          />
          <span>{{ t('labels.copy') }}</span>
        </button>
      </template>
    </m-dialog>
  </m-modal>
</template>
