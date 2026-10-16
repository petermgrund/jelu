<script setup lang="ts">
import { ref } from "vue";
import { useI18n } from 'vue-i18n';
import { Review, Visibility } from "../model/Review";
import dataService from "../services/DataService";
import MAvatar from "./m3/MAvatar.vue";
import MStarRating from "./m3/MStarRating.vue";

const { t, d } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const props = defineProps<{ 
  review: Review,
  showDelete: boolean,
  showEdit: boolean,
}>();

const username = ref("")

const getUsername = async () => {
  username.value = await dataService.usernameById(props.review.user)
}

getUsername()

const emit = defineEmits<{
  (e: 'update:delete', reviewId: string): void,
  (e: 'update:edit', reviewId: string): void,
}>()

const deleteReview = async (reviewId: string) => {
  emit("update:delete", reviewId)
}

const editReview = async (reviewId: string) => {
  emit("update:edit", reviewId)
}

</script>

<template>
  <article class="m3-card m3-card--outlined h-full">
    <div class="m3-card__content gap-3">
      <header class="flex items-center gap-3">
        <m-avatar :name="username" />
        <div class="min-w-0 flex-1">
          <div class="truncate text-title-small text-on-surface">
            {{ username }}
          </div>
          <div class="flex items-center gap-1 text-body-small text-on-surface-variant">
            <router-link
              v-tooltip="t('reviews.review_detail')"
              class="hover:text-primary hover:underline"
              :to="{ name: 'review-detail', params: { reviewId: props.review.id } }"
            >
              {{ d(props.review.reviewDate, 'short') }}
            </router-link>
            <i
              v-tooltip="props.review.visibility === Visibility.PUBLIC ? t('reviews.public') : t('reviews.private')"
              class="mdi text-[1rem]"
              :class="props.review.visibility === Visibility.PUBLIC ? 'mdi-earth' : 'mdi-lock-outline'"
              :aria-label="props.review.visibility === Visibility.PUBLIC ? t('reviews.public') : t('reviews.private')"
            />
          </div>
        </div>
        <button
          v-if="props.showEdit"
          v-tooltip="t('reviews.edit_review')"
          type="button"
          class="m3-icon-btn"
          :aria-label="t('reviews.edit_review')"
          @click="editReview(props.review.id)"
        >
          <i
            class="mdi mdi-pencil-outline"
            aria-hidden="true"
          />
        </button>
        <button
          v-if="props.showDelete"
          v-tooltip="t('labels.delete')"
          type="button"
          class="m3-icon-btn"
          :aria-label="t('labels.delete')"
          @click="deleteReview(props.review.id)"
        >
          <i
            class="mdi mdi-delete-outline"
            aria-hidden="true"
          />
        </button>
      </header>
      <m-star-rating
        :model-value="props.review.rating"
        readonly
        size="sm"
        show-value
      />
      <v-md-preview
        class="text-body-large"
        :text="props.review.text"
      />
    </div>
  </article>
</template>
