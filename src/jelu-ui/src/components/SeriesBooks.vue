<script setup lang="ts">
import { useThrottleFn, useTitle } from '@vueuse/core';
import { useRouteQuery } from "@vueuse/router";
import { computed, Ref, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import useBulkEdition from '../composables/bulkEdition';
import usePagination from '../composables/pagination';
import useSort from "../composables/sort";
import { Book } from '../model/Book';
import { LibraryFilter } from "../model/LibraryFilter";
import dataService from "../services/DataService";
import { ObjectUtils } from '../utils/ObjectUtils';
import BookCard from "./BookCard.vue";
import BookGridSkeleton from "./BookGridSkeleton.vue";
import BulkSelectionBar from "./BulkSelectionBar.vue";
import MEmptyState from "./m3/MEmptyState.vue";
import MPageHeader from "./m3/MPageHeader.vue";
import MRadioGroup from "./m3/MRadioGroup.vue";
import SortFilterBarVue from "./SortFilterBar.vue";
import { Series } from '../model/Series';
import SeriesModalVue from './SeriesModal.vue'
import { useOruga } from "@oruga-ui/oruga-next"
import { formatStars } from '../utils/rating'

const { t, locale } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const route = useRoute()
const oruga = useOruga();

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading, pageCount } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('numberInSeries,asc')

const libraryFilter: Ref<LibraryFilter> = useRouteQuery('libraryFilter', 'ANY' as LibraryFilter)

const { showSelect, selectAll, checkedCards, cardChecked, toggleEdit } = useBulkEdition(modalClosed)

const open = ref(false)

const series: Ref<Series> = ref({name: ""})
const books: Ref<Array<Book>> = ref([]);

const getBooksIsLoading: Ref<boolean> = ref(false)

watch([() => route.params.seriesId, page, sortQuery, libraryFilter], (newVal, oldVal) => {
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal && route.params.seriesId !== undefined) {
    throttledGetBooks()
  }
})

watch(() => route.params.seriesId, (newVal, oldVal) => {
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal && route.params.seriesId !== undefined) {
    getSeries()
  }
})

const getSeries = async () => {
  try {
    series.value = await dataService.getSeriesById(route.params.seriesId as string)
    useTitle('Jelu | ' + series.value.name)
  } catch (error) {
    console.log("failed get series : " + error);
  }
};

const getBooks = () => {
    getBooksIsLoading.value = true
    dataService.getSeriesBooksById(route.params.seriesId as string,
      pageAsNumber.value - 1, perPage.value, sortQuery.value,
      libraryFilter.value)
      .then(res => {
        console.log(res)
            total.value = res.totalElements
            books.value = res.content
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

const convertedBooks = computed(() => books.value?.map(b => ObjectUtils.toUserBook(b)))

function modalClosed() {
  console.log("modal closed")
  throttledGetBooks()
}

function seriesModalClosed() {
  console.log("series modal closed")
  getSeries()
}

function toggleSeriesModal(series: Series, edit: boolean) {
    oruga.modal.open({
      component: SeriesModalVue,
      trapFocus: true,
      active: true,
      canCancel: ['x', 'button', 'outside'],
      scroll: 'keep',
      props: {
        "series": series,
        "edit" : edit,
      },
      onClose: seriesModalClosed
    });
}

const sortOptions = computed(() => [
  { value: 'title', label: t('sorting.title') },
  { value: 'publisher', label: t('sorting.publisher') },
  { value: 'numberInSeries', label: t('sorting.series_number') },
  { value: 'publishedDate', label: t('sorting.publication_date') },
  { value: 'modificationDate', label: t('sorting.modification_date') },
  { value: 'pageCount', label: t('sorting.page_count') },
])

const libraryFilterOptions = computed(() => [
  { value: LibraryFilter.ANY, label: t('filtering.any') },
  { value: LibraryFilter.ONLY_USER_BOOKS, label: t('filtering.only_in_my_list') },
  { value: LibraryFilter.ONLY_NON_USER_BOOKS, label: t('filtering.only_not_in_my_list') },
])

const activeFilterCount = computed(() => libraryFilter.value != null && libraryFilter.value !== LibraryFilter.ANY ? 1 : 0)

getSeries()
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
      </template>
    </sort-filter-bar-vue>
    <m-page-header
      :title="series.name"
      :subtitle="total > 0 ? t('labels.book_count', total) : undefined"
    >
      <template #actions>
        <button
          v-tooltip="t('series.edit_series')"
          type="button"
          class="m3-icon-btn m3-icon-btn--outlined"
          :aria-label="t('series.edit_series')"
          @click="toggleSeriesModal(series, true)"
        >
          <i
            class="mdi mdi-pencil-outline"
            aria-hidden="true"
          />
        </button>
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
    <div
      v-if="series.description != null || series.avgRating != null || series.userRating != null"
      class="mb-6 flex max-w-3xl flex-col gap-3"
    >
      <v-md-preview
        v-if="series.description != null"
        class="text-body-large"
        :text="series.description"
      />
      <p
        v-if="series.avgRating != null || series.userRating != null"
        class="flex flex-wrap items-center gap-x-6 gap-y-1 text-body-large text-on-surface-variant"
      >
        <span
          v-if="series.avgRating != null"
          class="inline-flex items-center gap-1"
        >
          <i
            class="mdi mdi-star-outline text-[1.25rem]"
            aria-hidden="true"
          />
          {{ t('labels.avg_rating', { rating: formatStars(series.avgRating, locale) }) }}
        </span>
        <span
          v-if="series.userRating != null"
          class="inline-flex items-center gap-1"
        >
          <i
            class="mdi mdi-star text-[1.25rem] text-primary"
            aria-hidden="true"
          />
          {{ t('labels.user_rating', { rating: formatStars(series.userRating, locale, true) }) }}
        </span>
      </p>
    </div>
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
      position="centered"
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
        :series-id="series.id"
        @update:modal-closed="modalClosed"
        @update:checked="cardChecked"
      />
    </TransitionGroup>
    <book-grid-skeleton v-else-if="getBooksIsLoading" />
    <m-empty-state
      v-else
      icon="bookshelf"
      :title="t('labels.list_empty')"
    />
    <o-pagination
      v-if="convertedBooks.length > 0 && pageCount > 1"
      v-model:current="pageAsNumber"
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
