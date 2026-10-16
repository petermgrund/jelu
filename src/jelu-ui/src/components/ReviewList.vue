<script setup lang="ts">
import { useThrottleFn, useTitle } from '@vueuse/core';
import { Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import usePagination from '../composables/pagination';
import useSort from "../composables/sort";
import { Review } from '../model/Review';
import dataService from "../services/DataService";
import MEmptyState from './m3/MEmptyState.vue';
import MPageHeader from './m3/MPageHeader.vue';
import ReviewBookCard from './ReviewBookCard.vue';

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | ' + t('nav.activity'))

const reviews: Ref<Array<Review>> = ref([]);

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading, pageCount } = usePagination(16)

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('reviewDate,desc')

const getBookIsLoading: Ref<boolean> = ref(false)

watch([page, sortQuery], (newVal, oldVal) => {
  console.log("all " + newVal + " " + oldVal)
  if (newVal !== oldVal) {
    throttledGetReviews()
  }
})

const getReviews = () => {
  getBookIsLoading.value = true
  dataService.findReviews(undefined, undefined, null,
  null, null,
  pageAsNumber.value - 1, perPage.value, sortQuery.value)
  .then(res => {
        console.log(res)
          total.value = res.totalElements
          reviews.value = res.content
        if (! res.empty) {
          page.value =  (res.number + 1).toString(10)
        }
        else {
          page.value = "1"
        }
        getBookIsLoading.value = false
        updatePageLoading(false)
    }
    )
    .catch(e => {
      getBookIsLoading.value = false
      updatePageLoading(false)
    })

};

// watches set above sometimes called twice
// so getBooks was sometimes called twice at the same instant
const throttledGetReviews = useThrottleFn(() => {
  getReviews()
}, 100, false)

try {
  getReviews();
} catch (error) {
  console.log("failed get reviews : " + error);
}
</script>

<template>
  <div class="m3-page">
    <m-page-header :title="t('nav.activity')" />
    <o-pagination
      v-if="reviews.length > 0 && pageCount > 1"
      :current="pageAsNumber"
      :total="total"
      position="centered"
      :per-page="perPage"
      @change="updatePage"
    />
    <TransitionGroup
      v-if="reviews.length > 0"
      name="list"
      tag="div"
      class="grid grid-cols-[repeat(auto-fill,minmax(min(20rem,100%),1fr))] gap-4"
    >
      <review-book-card
        v-for="review in reviews"
        :key="review.id"
        :review="review"
        :book-reviews-link="true"
        :show-user-name="true"
      />
    </TransitionGroup>
    <div
      v-else-if="getBookIsLoading"
      class="grid grid-cols-[repeat(auto-fill,minmax(min(20rem,100%),1fr))] gap-4"
      aria-hidden="true"
    >
      <div
        v-for="index in 6"
        :key="index"
        class="m3-skeleton h-36"
      />
    </div>
    <m-empty-state
      v-else
      icon="comment-text-multiple-outline"
      :title="t('reviews.no_reviews')"
    />
    <o-pagination
      v-if="reviews.length > 0 && pageCount > 1"
      :current="pageAsNumber"
      :total="total"
      position="centered"
      :per-page="perPage"
      @change="updatePage"
    />
    <o-loading
      v-model:active="getPageIsLoading"
      :full-page="true"
      :cancelable="true"
    />
  </div>
</template>

<style scoped>
.list-enter-active,
.list-leave-active {
  transition: opacity var(--md-sys-motion-duration-short4) linear;
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
}
</style>
