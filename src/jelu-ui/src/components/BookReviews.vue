<script setup lang="ts">
import { useTitle } from '@vueuse/core';
import { computed, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import { Book } from "../model/Book";
import { Review } from "../model/Review";
import dataService from "../services/DataService";
import BookDataCard from "./BookDataCard.vue";
import MEmptyState from "./m3/MEmptyState.vue";
import ReviewCard from "./ReviewCard.vue";
import { useStore } from 'vuex'
import { key } from '../store'

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const route = useRoute()

const store = useStore(key)

const logged = computed(() => {
  return store.getters.getLogged
})

useTitle('Jelu | Reviews')
const reviews: Ref<Array<Review>> = ref([])
const book: Ref<Book> = ref({title:""})
// the empty state is only shown once the reviews request is over
const reviewsLoaded = ref(false)

watch(() => route.params.bookId, (newVal, oldVal) => {
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal && route.params.bookId !== undefined) {
    getBook()
    getReviews()
  }
})

const getBook = async () => {
  try {
    book.value = await dataService.findBookById(route.params.bookId as string)
  } catch (error) {
    console.log("failed get book : " + error);
  }
};

const getReviews = async () => {
  dataService.findReviews(undefined,
    route.params.bookId as string, null, null, null, 0, 50, null)
    .then(res => {
        if (!res.empty) {
            reviews.value = res.content
        }
    })
    .catch(err => console.log(err))
    .finally(() => {
      reviewsLoaded.value = true
    })
};

getBook()
getReviews()
</script>

<template>
  <div class="m3-page m3-page--medium">
    <book-data-card
      :book="book"
      :owned="null"
      :to-read="null"
      :borrowed="null"
      :book-link="true"
      :links="logged"
      :add-book="logged"
    />
    <section class="m3-section">
      <h2 class="m3-section__title">
        {{ t('reviews.review', 2) }}
      </h2>
      <div
        v-if="reviews.length > 0"
        class="grid gap-4 lg:grid-cols-2"
      >
        <review-card
          v-for="review in reviews"
          :key="review.id"
          :review="review"
          :show-delete="false"
          :show-edit="false"
        />
      </div>
      <m-empty-state
        v-else-if="reviewsLoaded"
        icon="comment-text-outline"
        :title="t('reviews.no_reviews')"
      />
    </section>
  </div>
</template>
