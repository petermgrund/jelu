<script setup lang="ts">
import { useTitle } from '@vueuse/core';
import { computed, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import { UserBook } from "../model/Book";
import { ReadingEventType, ReadingEventWithUserBook } from '../model/ReadingEvent';
import { Review } from '../model/Review';
import { User } from '../model/User';
import dataService from "../services/DataService";
import BookCard from "./BookCard.vue";
import MAvatar from './m3/MAvatar.vue';
import MPageHeader from './m3/MPageHeader.vue';
import ReviewBookCard from './ReviewBookCard.vue';

const route = useRoute()

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const user: Ref<User> = ref({login: '', admin: false})
const currentlyReading: Ref<Array<UserBook>> = ref([]);
const events: Ref<Array<ReadingEventWithUserBook>> = ref([]);
const toRead: Ref<Array<UserBook>> = ref([]);
const userReviews: Ref<Array<Review>> = ref([]);

const currentlyReadingIsLoading: Ref<boolean> = ref(false)
const recentEventsIsLoading: Ref<boolean> = ref(false)
const getToReadIsLoading: Ref<boolean> = ref(false)

watch(() => route.params.userId, (newVal, oldVal) => {
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal && route.params.userId !== undefined) {
    getUser()
  }
})

const getUser = async () => {
  try {
    user.value = await dataService.getUserById(route.params.userId as string)
    useTitle('Jelu | User')
  } catch (error) {
    console.log("failed get user : " + error);
  }
};
const getCurrentlyReading = async () => {
  currentlyReadingIsLoading.value = true
  try {
    const res = await dataService.findUserBookByCriteria([ReadingEventType.CURRENTLY_READING], null, route.params.userId as string, null)
    currentlyReading.value = res.content
    currentlyReadingIsLoading.value = false
  } catch (error) {
    console.log("failed get books : " + error)
    currentlyReadingIsLoading.value = false
  }
};

const nonCurrentlyReadingEvents: Array<ReadingEventType> = [ReadingEventType.DROPPED, ReadingEventType.FINISHED]

const getReadEvents = async () => {
  recentEventsIsLoading.value = true
  try {
    const res = await dataService.findReadingEvents(nonCurrentlyReadingEvents, route.params.userId as string, undefined, undefined, undefined, undefined, undefined, 0, undefined, "end_date:desc")
    events.value = res.content
    recentEventsIsLoading.value = false
  } catch (error) {
    console.log("failed get events : " + error)
    recentEventsIsLoading.value = false
  }
};

const getToRead = async () => {
  getToReadIsLoading.value = true
  try {
    const res = await dataService.findUserBookByCriteria(
      null, null, route.params.userId as string,
    true, null, null,
    0, 20, undefined)
    toRead.value = res.content
    getToReadIsLoading.value = false
  } catch (error) {
    console.log("failed get books : " + error);
    getToReadIsLoading.value = false
  }
};

const getUserReviews = async () => {
  try {
    const res = await dataService.findReviews(
      route.params.userId as string, undefined, null,
    null, null,
    0, 20, null)
    userReviews.value = res.content
  } catch (error) {
    console.log("failed get reviews : " + error);
  }
};

const toUserBook = (book: UserBook): UserBook => {
    const converted = {
      book: book.book
    } as UserBook
    return converted
  }

const convertedBooks = computed(() => currentlyReading.value?.map(b => toUserBook(b)))
const convertedBooksToRead = computed(() => toRead.value?.map(b => toUserBook(b)))

getUser()
getCurrentlyReading()
getReadEvents()
getToRead()
getUserReviews()
</script>

<template>
  <div class="m3-page">
    <m-page-header>
      <template #title>
        <span class="flex items-center gap-4">
          <m-avatar
            :name="user.login"
            size="lg"
            class="font-sans"
          />
          <span class="min-w-0">{{ user.login }}</span>
        </span>
      </template>
      <template #actions>
        <router-link
          class="m3-btn m3-btn--tonal"
          :to="{ name: 'my-books', query: { userId: route.params.userId } }"
        >
          <i
            class="mdi mdi-bookshelf"
            aria-hidden="true"
          />
          <span>{{ t('labels.books_from_name', { name: user.login }) }}</span>
        </router-link>
        <router-link
          class="m3-btn m3-btn--tonal"
          :to="{ name: 'to-read', query: { userId: route.params.userId } }"
        >
          <i
            class="mdi mdi-bookmark-multiple-outline"
            aria-hidden="true"
          />
          <span>{{ t('nav.to_read') }}</span>
        </router-link>
      </template>
    </m-page-header>

    <section
      v-if="currentlyReading.length > 0 || currentlyReadingIsLoading"
      class="m3-section"
    >
      <h2 class="m3-section__title">
        {{ t('home.currently_reading') }}
      </h2>
      <div
        v-if="currentlyReading.length > 0"
        class="-mx-4 flex snap-x scroll-px-4 gap-4 overflow-x-auto px-4 pb-2 sm:mx-0 sm:scroll-px-0 sm:px-0"
      >
        <div
          v-for="book in convertedBooks"
          :key="book.book.id"
          class="w-40 shrink-0 snap-start sm:w-44"
        >
          <book-card
            :book="book"
            :public="false"
            :force-select="false"
            :show-select="false"
            :propose-add="false"
          />
        </div>
      </div>
      <div
        v-else
        class="flex gap-4 overflow-hidden"
        aria-hidden="true"
      >
        <div
          v-for="index in 6"
          :key="index"
          class="m3-skeleton aspect-[2/3] w-40 shrink-0 sm:w-44"
        />
      </div>
    </section>

    <section
      v-if="events.length > 0 || recentEventsIsLoading"
      class="m3-section"
    >
      <h2 class="m3-section__title">
        {{ t('home.recent_events') }}
      </h2>
      <div
        v-if="events.length > 0"
        class="-mx-4 flex snap-x scroll-px-4 gap-4 overflow-x-auto px-4 pb-2 sm:mx-0 sm:scroll-px-0 sm:px-0"
      >
        <div
          v-for="event in events"
          :key="event.id"
          class="w-40 shrink-0 snap-start sm:w-44"
        >
          <book-card
            :book="{ ...toUserBook(event.userBook), lastReadingEvent: event.eventType, toRead: false }"
            :public="false"
            :force-select="false"
            :show-select="false"
            :propose-add="false"
          />
        </div>
      </div>
      <div
        v-else
        class="flex gap-4 overflow-hidden"
        aria-hidden="true"
      >
        <div
          v-for="index in 6"
          :key="index"
          class="m3-skeleton aspect-[2/3] w-40 shrink-0 sm:w-44"
        />
      </div>
    </section>

    <section
      v-if="toRead.length > 0 || getToReadIsLoading"
      class="m3-section"
    >
      <h2 class="m3-section__title">
        {{ t('nav.to_read') }}
      </h2>
      <div
        v-if="toRead.length > 0"
        class="-mx-4 flex snap-x scroll-px-4 gap-4 overflow-x-auto px-4 pb-2 sm:mx-0 sm:scroll-px-0 sm:px-0"
      >
        <div
          v-for="book in convertedBooksToRead"
          :key="book.book.id"
          class="w-40 shrink-0 snap-start sm:w-44"
        >
          <book-card
            :book="book"
            :public="false"
            :force-select="false"
            :show-select="false"
            :propose-add="false"
          />
        </div>
      </div>
      <div
        v-else
        class="flex gap-4 overflow-hidden"
        aria-hidden="true"
      >
        <div
          v-for="index in 6"
          :key="index"
          class="m3-skeleton aspect-[2/3] w-40 shrink-0 sm:w-44"
        />
      </div>
    </section>

    <section
      v-if="userReviews.length > 0"
      class="m3-section"
    >
      <h2 class="m3-section__title">
        {{ t('reviews.review', 2) }}
      </h2>
      <div class="-mx-4 flex snap-x scroll-px-4 gap-4 overflow-x-auto px-4 pb-2 sm:mx-0 sm:scroll-px-0 sm:px-0">
        <div
          v-for="review in userReviews"
          :key="review.id"
          class="w-[min(20rem,80vw)] shrink-0 snap-start"
        >
          <review-book-card
            :review="review"
            :book-reviews-link="false"
            :show-user-name="false"
          />
        </div>
      </div>
    </section>
  </div>
</template>
