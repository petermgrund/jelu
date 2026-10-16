<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next"
import { useTitle } from '@vueuse/core'
import { computed, Ref, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import { UserBook } from '../model/Book'
import { CreateReadingEvent, ReadingEvent, ReadingEventType, ReadingEventWithUserBook } from '../model/ReadingEvent'
import { Review } from "../model/Review"
import dataService from "../services/DataService"
import { key } from '../store'
import BookCard from "./BookCard.vue"
import QuotesDisplay from './QuotesDisplay.vue'
import ReadingEventModalVue from './ReadingEventModal.vue'
import ReadProgressModal from './ReadProgressModal.vue'
import ReviewBookCard from './ReviewBookCard.vue';
import BookCover from './BookCover.vue'
import BookGridSkeleton from './BookGridSkeleton.vue'
import MEmptyState from './m3/MEmptyState.vue'

useTitle('Jelu | Home')

const store = useStore(key)
const oruga = useOruga()
const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const isLogged = computed(() => {
    return store != null && store != undefined && store.getters.getLogged
  })

const initialLoad : Ref<boolean> = ref(true)

const showModal: Ref<boolean> = ref(false)

const currentlyReadingIsLoading: Ref<boolean> = ref(false)

const recentEventsIsLoading: Ref<boolean> = ref(false)

const books: Ref<Array<UserBook>> = ref([]);

const events: Ref<Array<ReadingEventWithUserBook>> = ref([]);

const hasBooks = computed(() => books.value.length > 0)

const userReviews: Ref<Array<Review>> = ref([]);

const getCurrentlyReading = async () => {
  currentlyReadingIsLoading.value = true
  try {
    const res = await dataService.findUserBookByCriteria([ReadingEventType.CURRENTLY_READING], null, null, null)
    if (res.numberOfElements <= 6) {
      books.value = res.content
    }
    else {
      books.value = res.content.slice(0,6)
    }
    currentlyReadingIsLoading.value = false
  } catch (error) {
    console.log("failed get books : " + error)
    currentlyReadingIsLoading.value = false
  }

};

const nonCurrentlyReadingEvents: Array<ReadingEventType> = [ReadingEventType.DROPPED, ReadingEventType.FINISHED]

const getMyEvents = async () => {
  recentEventsIsLoading.value = true
  try {
    const res = await dataService.myReadingEvents(nonCurrentlyReadingEvents, undefined, undefined, undefined, undefined, undefined, 0, 8, 'endDate,desc')
    const notCurrentlyReading = res.content.filter(e => e.eventType !== ReadingEventType.CURRENTLY_READING)
    events.value = notCurrentlyReading
    recentEventsIsLoading.value = false
  } catch (error) {
    console.log("failed get events : " + error)
    recentEventsIsLoading.value = false
  }
};

const getUserReviews = async () => {
  try {
    const res = await dataService.findReviews(
      undefined, undefined, null,
    null, null,
    0, 20, null)
    userReviews.value = res.content
  } catch (error) {
    console.log("failed get reviews : " + error);
  }
};

if (isLogged.value) {
  try {
      getCurrentlyReading()
      getMyEvents()
      getUserReviews()
  } catch (error) {
    console.log("failed get books : " + error);
  }
}

watch(() => isLogged.value, (newValue, oldValue) => {
  console.log('logged changed ' + isLogged.value)
  if (initialLoad.value && isLogged.value) {
    try {
      initialLoad.value = false
      getCurrentlyReading()
      getMyEvents()
  } catch (error) {
    console.log("failed get books : " + error);
  }
  }
})

function modalClosed() {
  console.log("modal closed")
  getCurrentlyReading()
}

function defaultCreateEvent(bookId: string): CreateReadingEvent {
  return {
  eventType: ReadingEventType.FINISHED,
  eventDate: new Date(),
  bookId: bookId
}
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

function toggleReadProgressModal(userBookId: string, pageCount: number|null, currentProgress: number|null, currentPage: number|null) {
  showModal.value = !showModal.value
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

</script>

<template>
  <div class="m3-page">
    <template v-if="isLogged">
      <section class="pt-2">
        <h1 class="m3-section__title">
          {{ t('home.currently_reading') }}
        </h1>
        <div
          v-if="hasBooks"
          class="grid gap-4 md:grid-cols-2 xl:grid-cols-3"
        >
          <article
            v-for="book in books"
            :key="book.id"
            class="m3-card m3-card--filled flex-row"
          >
            <router-link
              :to="{ name: 'book-detail', params: { bookId: book.id } }"
              class="w-28 shrink-0 sm:w-32"
              tabindex="-1"
              aria-hidden="true"
            >
              <book-cover
                :image="book.book.image"
                :title="book.book.title"
                class="h-full"
              />
            </router-link>
            <div class="flex min-w-0 flex-1 flex-col gap-2 p-4">
              <router-link
                :to="{ name: 'book-detail', params: { bookId: book.id } }"
                class="line-clamp-2 text-title-medium text-on-surface hover:underline"
              >
                {{ book.book.title }}
              </router-link>
              <p
                v-if="book.book.authors != null && book.book.authors.length > 0"
                class="line-clamp-1 text-body-medium text-on-surface-variant"
              >
                {{ book.book.authors.map(a => a.name).join(', ') }}
              </p>
              <div
                v-if="book.percentRead != null && book.percentRead > 0"
                class="mt-1 flex flex-col gap-1.5"
              >
                <progress
                  class="m3-linear-progress"
                  max="100"
                  :value="book.percentRead"
                />
                <span class="text-label-medium text-on-surface-variant tabular-nums">
                  <template v-if="book.currentPageNumber != null">p. {{ book.currentPageNumber }} · </template>{{ Math.round(book.percentRead) }} %
                </span>
              </div>
              <div class="mt-auto flex flex-wrap items-center gap-2 pt-2">
                <button
                  type="button"
                  class="m3-btn m3-btn--tonal m3-btn--sm"
                  @click="toggleReadProgressModal(book.id!!, book.book.pageCount ?? null, book.percentRead ?? null, book.currentPageNumber ?? null)"
                >
                  <i
                    class="mdi mdi-progress-check"
                    aria-hidden="true"
                  />
                  <span>{{ t('labels.set_progress') }}</span>
                </button>
                <button
                  v-tooltip="t('labels.mark_read_or_drop')"
                  type="button"
                  class="m3-icon-btn m3-icon-btn--sm m3-icon-btn--outlined"
                  :aria-label="t('labels.mark_read_or_drop')"
                  @click="toggleReadingEventModal(defaultCreateEvent(book.book.id!!), false)"
                >
                  <i
                    class="mdi mdi-check"
                    aria-hidden="true"
                  />
                </button>
              </div>
            </div>
          </article>
        </div>
        <div
          v-else-if="currentlyReadingIsLoading"
          class="grid gap-4 md:grid-cols-2 xl:grid-cols-3"
          aria-hidden="true"
        >
          <div class="m3-skeleton h-44" />
          <div class="m3-skeleton h-44" />
        </div>
        <m-empty-state
          v-else
          icon="book-open-page-variant-outline"
          :title="t('home.not_reading')"
        >
          <router-link
            :to="{ name: 'to-read' }"
            class="m3-btn m3-btn--tonal"
          >
            <i
              class="mdi mdi-bookmark-multiple-outline"
              aria-hidden="true"
            />
            <span>{{ t('nav.to_read') }}</span>
          </router-link>
          <router-link
            :to="{ name: 'add-book' }"
            class="m3-btn m3-btn--filled"
          >
            <i
              class="mdi mdi-plus"
              aria-hidden="true"
            />
            <span>{{ t('nav.add_book') }}</span>
          </router-link>
        </m-empty-state>
      </section>

      <section
        v-if="events.length > 0"
        class="m3-section"
      >
        <h2 class="m3-section__title">
          <span>{{ t('home.recent_events') }}</span>
          <router-link
            class="m3-btn m3-btn--text"
            :to="{ name: 'history' }"
          >
            <span>{{ t('nav.history') }}</span>
            <i
              class="mdi mdi-arrow-right"
              aria-hidden="true"
            />
          </router-link>
        </h2>
        <div class="-mx-4 flex snap-x gap-4 overflow-x-auto px-4 pb-2 sm:mx-0 sm:px-0">
          <div
            v-for="event in events"
            :key="event.id"
            class="w-40 shrink-0 snap-start sm:w-44"
          >
            <book-card
              :book="{ ...event.userBook, lastReadingEvent: event.eventType }"
              :public="false"
              :force-select="false"
              :show-select="false"
              :propose-add="true"
            />
          </div>
        </div>
      </section>
      <div
        v-else-if="recentEventsIsLoading"
        class="m3-section"
      >
        <book-grid-skeleton :count="4" />
      </div>

      <section
        v-if="userReviews.length > 0"
        class="m3-section"
      >
        <h2 class="m3-section__title">
          <span>{{ t('reviews.review', 2) }}</span>
          <router-link
            class="m3-btn m3-btn--text"
            :to="{ name: 'reviews' }"
          >
            <span>{{ t('nav.activity') }}</span>
            <i
              class="mdi mdi-arrow-right"
              aria-hidden="true"
            />
          </router-link>
        </h2>
        <div class="-mx-4 flex snap-x gap-4 overflow-x-auto px-4 pb-2 sm:mx-0 sm:px-0">
          <div
            v-for="review in userReviews"
            :key="review.id"
            class="shrink-0 snap-start"
          >
            <review-book-card
              :review="review"
              :book-reviews-link="true"
              :show-user-name="true"
            />
          </div>
        </div>
      </section>

      <quotes-display />
    </template>
    <m-empty-state
      v-else
      icon="account-lock-outline"
      :title="t('user.log_first')"
    >
      <router-link
        :to="{ name: 'login' }"
        class="m3-btn m3-btn--filled"
      >
        <i
          class="mdi mdi-login"
          aria-hidden="true"
        />
        <span>{{ t('nav.login') }}</span>
      </router-link>
    </m-empty-state>
  </div>
</template>

<style lang="scss" scoped>

</style>
