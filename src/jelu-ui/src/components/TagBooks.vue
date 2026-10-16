<script setup lang="ts">
import { useThrottleFn, useTitle } from '@vueuse/core';
import { useRouteQuery } from "@vueuse/router";
import { computed, Ref, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import usePagination from '../composables/pagination';
import useSort from "../composables/sort";
import useBulkEdition from '../composables/bulkEdition';
import { Book } from '../model/Book';
import { LibraryFilter } from "../model/LibraryFilter";
import { Tag } from '../model/Tag';
import dataService from "../services/DataService";
import { asArray, ObjectUtils } from '../utils/ObjectUtils';
import BookCard from "./BookCard.vue";
import BookGridSkeleton from "./BookGridSkeleton.vue";
import BulkSelectionBar from "./BulkSelectionBar.vue";
import MChipSet from "./m3/MChipSet.vue";
import MEmptyState from "./m3/MEmptyState.vue";
import MPageHeader from "./m3/MPageHeader.vue";
import MRadioGroup from "./m3/MRadioGroup.vue";
import SortFilterBarVue from "./SortFilterBar.vue";
import { ReadingEventType } from '../model/ReadingEvent';

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const route = useRoute()

const tag: Ref<Tag> = ref({name: ""})
const tagBooks: Ref<Array<Book>> = ref([]);

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading, pageCount } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('title,asc')

const libraryFilter: Ref<LibraryFilter> = useRouteQuery('libraryFilter', 'ANY' as LibraryFilter)

const eventTypes: Ref<Array<ReadingEventType>> = useRouteQuery('lastEventTypes', [], { transform: (value: ReadingEventType | Array<ReadingEventType>) => asArray(value) })

const { showSelect, selectAll, checkedCards, cardChecked, toggleEdit } = useBulkEdition(modalClosed)

const open = ref(false)

const getBooksIsLoading: Ref<boolean> = ref(false)

watch([() => route.params.tagId, page, sortQuery, libraryFilter, eventTypes], (newVal, oldVal) => {
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal && route.params.tagId !== undefined) {
    throttledGetBooks()
  }
})

watch(() => route.params.tagId, (newVal, oldVal) => {
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal && route.params.tagId !== undefined) {
    getTag()
  }
})

const getTag = async () => {
  try {
    tag.value = await dataService.getTagById(route.params.tagId as string)
    useTitle('Jelu | #' + tag.value.name)
  } catch (error) {
    console.log("failed get tag : " + error);
  }
};

const getBooks = () => {
    getBooksIsLoading.value = true
    dataService.getTagBooksById(route.params.tagId as string,
      pageAsNumber.value - 1, perPage.value, sortQuery.value,
      libraryFilter.value, eventTypes.value)
      .then(res => {
        console.log(res)
            total.value = res.totalElements
            tagBooks.value = res.content
          if (! res.empty) {
            page.value =  (res.number + 1).toString(10)
          }
          else {
            page.value = "1"
          }
          getBooksIsLoading.value = false
          updatePageLoading(false)
      }
      )
      .catch(e => {
        getBooksIsLoading.value = false
        updatePageLoading(false)
      })
};

const throttledGetBooks = useThrottleFn(() => {
  getBooks()
}, 100, false)

const convertedBooks = computed(() => tagBooks.value?.map(b => ObjectUtils.unwrapUserBook(b)))

function modalClosed() {
  console.log("modal closed")
  throttledGetBooks()
}

const sortOptions = computed(() => [
  { value: 'title', label: t('sorting.title') },
  { value: 'publisher', label: t('sorting.publisher') },
  { value: 'series', label: t('sorting.series') },
  { value: 'publishedDate', label: t('sorting.publication_date') },
  { value: 'modificationDate', label: t('sorting.modification_date') },
  { value: 'pageCount', label: t('sorting.page_count') },
  { value: 'random', label: t('sorting.random') },
])

const libraryFilterOptions = computed(() => [
  { value: LibraryFilter.ANY, label: t('filtering.any') },
  { value: LibraryFilter.ONLY_USER_BOOKS, label: t('filtering.only_in_my_list') },
  { value: LibraryFilter.ONLY_NON_USER_BOOKS, label: t('filtering.only_not_in_my_list') },
])

const eventTypeOptions = computed(() => [
  { value: ReadingEventType.FINISHED, label: t('reading_events.finished'), icon: 'check-circle-outline' },
  { value: ReadingEventType.CURRENTLY_READING, label: t('reading_events.currently_reading'), icon: 'book-open-page-variant-outline' },
  { value: ReadingEventType.DROPPED, label: t('reading_events.dropped'), icon: 'close-circle-outline' },
])

const activeFilterCount = computed(() => eventTypes.value.length
  + (libraryFilter.value != null && libraryFilter.value !== LibraryFilter.ANY ? 1 : 0))

getTag()
getBooks()
</script>

<template>
  <div class="m3-page">
    <sort-filter-bar-vue
      :open="open"
      :order="sortOrder"
      @update:open="open = $event"
      @update:sort-order="sortOrderUpdated"
    >
      <template #sort-fields>
        <m-radio-group
          v-model="sortBy"
          :options="sortOptions"
          :label="t('sorting.sort_by')"
        />
      </template>
      <template #filters>
        <section class="m3-sheet__section">
          <h3 class="m3-sheet__section-title">
            {{ t('filtering.books_type') }}
          </h3>
          <m-radio-group
            v-model="libraryFilter"
            :options="libraryFilterOptions"
            :label="t('filtering.books_type')"
          />
        </section>
        <section class="m3-sheet__section">
          <h3 class="m3-sheet__section-title">
            {{ t('reading_events.last_event_type') }}
          </h3>
          <m-chip-set
            v-model="eventTypes"
            :options="eventTypeOptions"
            :aria-label="t('reading_events.last_event_type')"
          />
        </section>
      </template>
    </sort-filter-bar-vue>
    <m-page-header
      :title="t('labels.books_tagged_name', { tag : tag.name})"
      :subtitle="total > 0 ? t('labels.book_count', total) : undefined"
    >
      <template #actions>
        <button
          type="button"
          class="m3-btn m3-btn--tonal"
          @click="open = !open"
        >
          <i
            class="mdi mdi-tune-variant"
            aria-hidden="true"
          />
          <span>{{ t('labels.sort_and_filter') }}</span>
          <span
            v-if="activeFilterCount > 0"
            class="m3-badge"
          >{{ activeFilterCount }}</span>
        </button>
        <button
          v-tooltip="t('bulk.toggle')"
          type="button"
          class="m3-icon-btn m3-icon-btn--outlined"
          :class="{ 'is-selected': showSelect }"
          :aria-pressed="showSelect"
          :aria-label="t('bulk.toggle')"
          @click="showSelect = !showSelect"
        >
          <i
            class="mdi mdi-checkbox-multiple-marked-outline"
            aria-hidden="true"
          />
        </button>
      </template>
    </m-page-header>
    <bulk-selection-bar
      v-model:active="showSelect"
      v-model:select-all="selectAll"
      :count="checkedCards.length"
      @edit="toggleEdit(checkedCards)"
    />
    <o-pagination
      v-if="convertedBooks.length > 0 && pageCount > 1"
      v-model:current="pageAsNumber"
      :total="total"
      order="centered"
      :per-page="perPage"
      @change="updatePage"
    />
    <TransitionGroup
      v-if="convertedBooks.length > 0"
      name="jl-fade"
      tag="div"
      class="jl-book-grid"
    >
      <book-card
        v-for="book in convertedBooks"
        :key="book.book.id"
        :book="book"
        :force-select="selectAll"
        :public="false"
        :show-select="showSelect"
        :propose-add="true"
        @update:modal-closed="modalClosed"
        @update:checked="cardChecked"
      />
    </TransitionGroup>
    <book-grid-skeleton v-else-if="getBooksIsLoading" />
    <m-empty-state
      v-else
      icon="tag-outline"
      :title="t('labels.list_empty')"
    />
    <o-pagination
      v-if="convertedBooks.length > 0 && pageCount > 1"
      v-model:current="pageAsNumber"
      :total="total"
      order="centered"
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
