<script setup lang="ts">
import { computed, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import { UserBook } from "../model/Book";
import { ReadingEventType } from "../model/ReadingEvent";
import { ObjectUtils } from "../utils/ObjectUtils";
import BookCover from "./BookCover.vue";
import ReadingStatus from "./ReadingStatus.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const props = defineProps<{
  book: UserBook,
  forceSelect: boolean,
  showSelect: boolean,
  proposeAdd: boolean,
  seriesId?: string,
  public: boolean // is it on a public facing page (so hide links etc...)
}>();
const emit = defineEmits<{
  (e: 'update:modalClosed', open: boolean): void,
  (e: 'update:checked', id: string|null, checked: boolean): void
}>()

const checked: Ref<boolean> = ref(false)

watch(() => props.forceSelect, () => {
  checked.value = props.forceSelect
})

const authorsText = computed(() => {
  return (props.book.book.authors ?? []).map(author => author.name).join(", ")
});

const showProgressBar = computed(() => {
  return props.book.percentRead != null
      && props.book.percentRead > 0
      && props.book.lastReadingEvent === ReadingEventType.CURRENTLY_READING
})

const progressBarTooltip = computed(() => {
  return props.book.currentPageNumber != null ? `p. ${props.book.currentPageNumber}` : `${props.book.percentRead} %`
})

const currentSeries = computed(() => {
  if (props.book.book.series != null && props.book.book.series?.length > 0) {
    if (props.seriesId != null) {
      return props.book.book.series?.find(s => s.seriesId === props.seriesId)
    } else {
      return props.book.book.series[0]
    }
  }
  return null
})

// books which are not in the user library link to their public reviews page
const bookLink = computed(() => props.book.id != null
  ? { name: 'book-detail', params: { bookId: props.book.id } }
  : { name: 'book-reviews', params: { bookId: props.book.book.id } })

watch(checked, () => {
  emit("update:checked", props.book.id != null ? props.book.id as string : props.book.book.id as string , checked.value)
})

const currentTimestamp = ObjectUtils.timestamp()
</script>

<template>
  <article
    class="m3-card m3-card--elevated relative h-full"
    :class="{ 'outline-2 outline-primary': props.showSelect && checked }"
  >
    <router-link
      :to="bookLink"
      class="block"
      tabindex="-1"
      aria-hidden="true"
    >
      <book-cover
        :image="book.book.image"
        :title="book.book.title"
        :cache-key="book.id != null ? currentTimestamp : undefined"
      >
        <div
          v-if="showProgressBar"
          v-tooltip="progressBarTooltip"
          class="absolute inset-x-0 bottom-0 h-1 bg-secondary-container"
        >
          <div
            class="h-full rounded-r-full bg-primary"
            :style="{ width: book.percentRead + '%' }"
          />
        </div>
      </book-cover>
    </router-link>
    <label
      v-if="props.showSelect"
      class="absolute top-2 left-2 grid h-10 w-10 cursor-pointer place-items-center rounded-full bg-surface-container-highest shadow-elevation-1"
    >
      <input
        v-model="checked"
        type="checkbox"
        class="m3-checkbox"
        :aria-label="book.book.title"
      >
    </label>
    <div class="flex flex-1 flex-col gap-1 p-3">
      <router-link
        :to="bookLink"
        class="line-clamp-2 text-title-small text-on-surface hover:underline"
        :title="book.book.title"
      >
        {{ book.book.title }}
      </router-link>
      <p
        v-if="book.book.authors != null && book.book.authors.length > 0"
        class="line-clamp-2 text-body-small text-on-surface-variant"
        :title="authorsText"
      >
        <template
          v-for="(author, index) in book.book.authors.slice(0, 3)"
          :key="author.id"
        >
          <router-link
            v-if="!public"
            class="hover:text-primary hover:underline"
            :to="{ name: 'author-detail', params: { authorId: author.id } }"
          >
            {{ author.name }}
          </router-link>
          <span v-else>{{ author.name }}</span>
          <span v-if="index < Math.min(book.book.authors.length, 3) - 1">, </span>
        </template>
        <span v-if="book.book.authors.length > 3">&#8230;</span>
      </p>
      <div class="mt-auto flex flex-wrap items-center gap-1.5 pt-2 text-label-medium text-on-surface-variant">
        <reading-status :type="book.lastReadingEvent" />
        <router-link
          v-if="currentSeries != null && !props.public"
          v-tooltip="currentSeries.name"
          class="m3-status m3-status--outlined hover:bg-surface-container-highest"
          :to="{ name: 'series', params: { seriesId: currentSeries.seriesId } }"
        >
          #{{ currentSeries.numberInSeries }}
        </router-link>
        <span
          v-else-if="currentSeries != null"
          class="m3-status m3-status--outlined"
        >#{{ currentSeries.numberInSeries }}</span>
        <span
          v-if="book.userAvgRating"
          v-tooltip="t('labels.user_avg_rating', {rating : book.userAvgRating})"
          class="inline-flex items-center gap-0.5"
        >
          <i
            class="mdi mdi-star text-[1rem] text-primary"
            aria-hidden="true"
          />{{ book.userAvgRating }}
        </span>
        <span
          v-if="book.avgRating"
          v-tooltip="t('labels.avg_rating', {rating : book.avgRating})"
          class="inline-flex items-center gap-0.5"
        >
          <i
            class="mdi mdi-star-outline text-[1rem]"
            aria-hidden="true"
          />{{ book.avgRating }}
        </span>
        <i
          v-if="book.owned"
          v-tooltip="t('book.owned')"
          class="mdi mdi-bookshelf text-[1rem]"
          :aria-label="t('book.owned')"
        />
        <i
          v-if="book.toRead"
          v-tooltip="t('book.in_read_list')"
          class="mdi mdi-bookmark-outline text-[1rem]"
          :aria-label="t('book.in_read_list')"
        />
        <i
          v-if="proposeAdd === true && book.id == null"
          v-tooltip="t('labels.book_not_yet_in_books')"
          class="mdi mdi-plus-circle-outline text-[1rem] text-primary"
          :aria-label="t('labels.not_in_your_books')"
        />
        <span
          v-if="$slots.icon"
          class="ml-auto inline-flex items-center"
        >
          <slot name="icon" />
        </span>
      </div>
      <slot name="date" />
    </div>
  </article>
</template>
