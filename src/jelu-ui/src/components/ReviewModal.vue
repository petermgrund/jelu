<script setup lang="ts">
import { computed, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import { Book } from "../model/Book";
import { Review, Visibility } from "../model/Review";
import dataService from "../services/DataService";
import MDialog from "./m3/MDialog.vue";
import MSegmentedButton from "./m3/MSegmentedButton.vue";
import MStarRating from "./m3/MStarRating.vue";

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const props = defineProps<{
  book: Book,
  edit: boolean,
  review: Review | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const progress: Ref<boolean> = ref(false)

const visibility: Ref<Visibility> = ref(props.edit != null && props.edit === true && props.review?.visibility != null ? props.review?.visibility : Visibility.PRIVATE)
const rating = ref(props.edit != null && props.edit === true && props.review?.rating != null ? props.review?.rating : 5.0)
const reviewText = ref(props.edit != null && props.edit === true && props.review?.text != null ? props.review?.text : "")

watch(visibility, (newVal, oldVal) => {
  console.log("visibilty " + visibility.value)
})

watch(rating, (newVal, oldVal) => {
  console.log("visibilty " + newVal + " " + oldVal)
})

const submit = () => {
  console.log("submit ")
  if (props.book.id != null) {
    progress.value = true
    dataService.saveReview({
      bookId: props.book.id,
      rating: rating.value,
      text: reviewText.value,
      visibility: visibility.value,
      reviewDate: new Date()
    })
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

const editReview = () => {
  console.log("edit ")
  if (props.book.id != null && props.review?.id != null) {
    progress.value = true
    dataService.updateReview(props.review.id, {
      rating: rating.value,
      text: reviewText.value,
      visibility: visibility.value,
    })
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

const visibilityOptions = computed(() => [
  { value: Visibility.PRIVATE, label: t('reviews.private'), icon: 'lock-outline' },
  { value: Visibility.PUBLIC, label: t('reviews.public'), icon: 'earth' },
])
</script>

<template>
  <m-dialog
    :headline="props.edit === true ? t('reviews.edit_review') : t('reviews.create_review')"
    size="lg"
    fullscreen
    @close="emit('close')"
  >
    <div class="flex flex-col gap-6">
      <div class="flex flex-col gap-3">
        <h3 class="text-title-small text-on-surface-variant">
          {{ t('reviews.review', 1) }}
        </h3>
        <v-md-editor
          v-model="reviewText"
          :disabled-menus="['image/upload-image', 'toc', 'save']"
          height="20rem"
        />
      </div>
      <div class="flex flex-col gap-3">
        <h3 class="text-title-small text-on-surface-variant">
          {{ t('reviews.visibility') }}
        </h3>
        <m-segmented-button
          v-model="visibility"
          :options="visibilityOptions"
          :aria-label="t('reviews.visibility')"
          class="max-w-md"
          block
        />
      </div>
      <div class="flex flex-col gap-3">
        <h3 class="text-title-small text-on-surface-variant">
          {{ t('reviews.rating') }}
        </h3>
        <m-star-rating
          v-model="rating"
          :label="t('reviews.rating')"
          show-value
        />
      </div>
    </div>
    <template #actions>
      <button
        type="button"
        class="m3-btn m3-btn--text"
        @click="emit('close')"
      >
        {{ t('labels.cancel') }}
      </button>
      <button
        v-if="props.edit == null || props.edit === false"
        type="button"
        class="m3-btn m3-btn--filled"
        :disabled="progress"
        @click="submit"
      >
        <span
          v-if="progress"
          class="m3-spinner"
        />
        <span>{{ t('labels.save') }}</span>
      </button>
      <button
        v-else
        type="button"
        class="m3-btn m3-btn--filled"
        :disabled="progress"
        @click="editReview"
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
