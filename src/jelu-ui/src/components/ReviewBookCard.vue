<script setup lang="ts">
import { ref, Ref } from "vue";
import { useI18n } from 'vue-i18n';
import { Book } from "../model/Book";
import { Review } from "../model/Review";
import dataService from "../services/DataService";
import BookCover from "./BookCover.vue";
import MAvatar from "./m3/MAvatar.vue";
import MStarRating from "./m3/MStarRating.vue";

const { t, d } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const props = defineProps<{ 
  review: Review,
  bookReviewsLink: boolean,
  showUserName: boolean
}>();

const book: Ref<Book|null> = ref(null)

const getBook = async () => {
  try {
    const res = await dataService.findBookById(props.review.book)
    book.value = res
  } catch (error) {
    console.log("failed get book : " + error)
  }
};

const username = ref("")

const getUsername = async () => {
  username.value = await dataService.usernameById(props.review.user)
}
if (props.showUserName === true) {
  getUsername()
}

getBook()

</script>

<template>
  <article class="m3-card m3-card--outlined flex h-full w-full flex-row">
    <router-link
      v-if="book != null"
      :to="props.bookReviewsLink ? { name: 'book-reviews', params: { bookId: book.id } } : { name: 'review-detail', params: { reviewId: props.review.id } }"
      class="w-24 shrink-0"
      tabindex="-1"
      aria-hidden="true"
    >
      <book-cover
        :image="book.image"
        :title="book.title"
        class="h-full"
      />
    </router-link>
    <div class="flex min-w-0 flex-1 flex-col gap-2 p-3">
      <router-link
        v-if="props.bookReviewsLink === true && book != null"
        v-tooltip="t('reviews.book_all_reviews')"
        class="line-clamp-2 text-title-small text-on-surface hover:underline"
        :to="{ name: 'book-reviews', params: { bookId: book.id } }"
      >
        {{ book.title }}
      </router-link>
      <h3
        v-else-if="book != null"
        class="line-clamp-2 text-title-small text-on-surface"
      >
        {{ book.title }}
      </h3>
      <m-star-rating
        :model-value="review.rating"
        readonly
        size="sm"
      />
      <p class="line-clamp-3 text-body-medium text-on-surface-variant">
        {{ review.text }}
      </p>
      <div class="mt-auto flex items-center gap-2 text-body-small text-on-surface-variant">
        <m-avatar
          v-if="props.showUserName"
          :name="username"
          size="sm"
        />
        <span
          v-if="props.showUserName"
          class="truncate"
        >{{ username }}</span>
        <router-link
          v-tooltip="t('reviews.review_detail')"
          class="ml-auto shrink-0 hover:text-primary hover:underline"
          :to="{ name: 'review-detail', params: { reviewId: props.review.id } }"
        >
          {{ d(props.review.reviewDate, 'short') }}
        </router-link>
      </div>
    </div>
  </article>
</template>

<style lang="scss" scoped>

</style>
