<script setup lang="ts">
import { useTitle } from '@vueuse/core';
import { Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import { Book } from "../model/Book";
import { Review } from "../model/Review";
import dataService from "../services/DataService";
import BookDataCard from "./BookDataCard.vue";
import MEmptyState from "./m3/MEmptyState.vue";
import ReviewCard from "./ReviewCard.vue";

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const route = useRoute()

const review: Ref<Review|null> = ref(null)
const book: Ref<Book|null> = ref(null)
// the empty state is only shown once the review request is over
const reviewLoaded = ref(false)

watch(() => route.params.reviewId, (newVal, oldVal) => {
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal && route.params.reviewId !== undefined) {
    getReview()
  }
})

const getReview = async () => {
  try {
    review.value = await dataService.findReviewById(route.params.reviewId as string)
    book.value = await dataService.findBookById(review.value.book)
    useTitle('Jelu | Review')
  } catch (error) {
    console.log("failed get review : " + error);
  }
  reviewLoaded.value = true
};

getReview()
</script>

<template>
  <div class="m3-page m3-page--medium">
    <book-data-card
      v-if="book != null"
      :book="book"
      :owned="null"
      :to-read="null"
      :borrowed="null"
      :book-link="false"
      :links="false"
      :add-book="false"
    />
    <section
      v-if="review != null"
      class="m3-section"
    >
      <h2 class="m3-section__title">
        {{ t('reviews.review', 1) }}
      </h2>
      <div class="grid gap-4 lg:grid-cols-2">
        <review-card
          :review="review"
          :show-delete="false"
          :show-edit="false"
        />
      </div>
    </section>
    <m-empty-state
      v-else-if="reviewLoaded"
      icon="comment-off-outline"
      :title="t('reviews.not_found')"
    />
  </div>
</template>
