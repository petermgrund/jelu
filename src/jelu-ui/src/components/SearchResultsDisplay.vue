<script setup lang="ts">
import { useTitle } from '@vueuse/core';
import { useRouteQuery } from '@vueuse/router';
import { computed, Ref, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import usePagination from '../composables/pagination';
import useSort from '../composables/sort';
import useBulkEdition from '../composables/bulkEdition';
import { Book } from '../model/Book';
import { LibraryFilter } from '../model/LibraryFilter';
import dataService from "../services/DataService";
import { asArray, ObjectUtils } from '../utils/ObjectUtils';
import BookCard from "./BookCard.vue";
import BookGridSkeleton from "./BookGridSkeleton.vue";
import BulkSelectionBar from "./BulkSelectionBar.vue";
import MChipSet from "./m3/MChipSet.vue";
import MDialog from "./m3/MDialog.vue";
import MEmptyState from "./m3/MEmptyState.vue";
import MModal from "./m3/MModal.vue";
import MPageHeader from "./m3/MPageHeader.vue";
import MRadioGroup from "./m3/MRadioGroup.vue";
import MTextField from "./m3/MTextField.vue";
import SortFilterBarVue from "./SortFilterBar.vue";
import TriStateFilter from "./TriStateFilter.vue";
import { ReadingEventType } from '../model/ReadingEvent';

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

useTitle('Jelu | Search')

const searchQuery: Ref<string|undefined> = useRouteQuery('q', undefined)

const books: Ref<Array<Book>> = ref([]);

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading, pageCount } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('title,asc')

const { showSelect, selectAll, checkedCards, cardChecked, toggleEdit } = useBulkEdition(modalClosed)

const libraryFilter: Ref<LibraryFilter> = useRouteQuery('libraryFilter', 'ANY' as LibraryFilter)

const open = ref(false)

const progress: Ref<boolean> = ref(false)

const eventTypes: Ref<Array<ReadingEventType>> = useRouteQuery('lastEventTypes', [], { transform: (value: ReadingEventType | Array<ReadingEventType>) => asArray(value) })
const toRead: Ref<string|null> = useRouteQuery('toRead', "null")
const owned: Ref<string|null> = useRouteQuery('owned', "null")
const borrowed: Ref<string|null> = useRouteQuery('borrowed', "null")

const toReadAsBool = computed(() => {
  if (toRead.value?.toLowerCase() === "null") {
    return null
  } else if (toRead.value?.toLowerCase() === "true") {
    return true
  } else {
    return false
  }
  }
)

const ownedAsBool = computed(() => {
  if (owned.value?.toLowerCase() === "null") {
    return null
  } else if (owned.value?.toLowerCase() === "true") {
    return true
  } else {
    return false
  }
  }
)

const borrowedAsBool = computed(() => {
  if (borrowed.value?.toLowerCase() === "null") {
    return null
  } else if (borrowed.value?.toLowerCase() === "true") {
    return true
  } else {
    return false
  }
  }
)

const search = () => {
    progress.value = true
    updatePageLoading(true)
      dataService.findBooks(
        searchQuery.value,
      pageAsNumber.value - 1, perPage.value,
      sortQuery.value, libraryFilter.value,
      eventTypes.value, toReadAsBool.value,
      ownedAsBool.value, borrowedAsBool.value
      )
    .then(res => {
      progress.value = false
      updatePageLoading(false)
        console.log(res)
          total.value = res.totalElements
          books.value = res.content
        if (! res.empty) {
          page.value =  (res.number + 1).toString(10)
        }
        else {
          page.value = "1"
        }
    }
    )
    .catch(e => {
      progress.value = false
      updatePageLoading(false)
    })
}

watch([page, sortQuery, libraryFilter, eventTypes, toRead, owned, borrowed], (newVal, oldVal) => {
  console.log(page.value)
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal) {
    search()
  }
})

const convertedBooks = computed(() => books.value?.map(b => ObjectUtils.toUserBook(b)))

function modalClosed() {
  console.log("modal closed")
  search()
}

const helperOpen = ref(false)

const append = (text: string) => {
  if (searchQuery.value == null) {
    searchQuery.value = ''
  }
  searchQuery.value += text
  helperOpen.value = false
}

const terms = ["tag", "author", "translator", "narrator", "series", "language",
"published_date", "publisher", "summary", "googleId", "goodreadsId", "amazonId",
"librarythingId", "noosfereId", "isfdbId", "inventaireId", "openlibraryId"]

const operators = ["AND", "OR", "NOT"]

// "no results" is only reported once a search has completed
const searched = ref(false)

watch(progress, (busy) => {
  if (!busy) {
    searched.value = true
  }
})

const sortOptions = computed(() => [
  { value: 'title', label: t('sorting.title') },
  { value: 'publisher', label: t('sorting.publisher') },
  { value: 'series', label: t('sorting.series') },
  { value: 'publishedDate', label: t('sorting.publication_date') },
  { value: 'pageCount', label: t('sorting.page_count') },
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
  { value: ReadingEventType.NONE, label: t('reading_events.none'), icon: 'circle-outline' },
])

const activeFilterCount = computed(() => eventTypes.value.length
  + (libraryFilter.value != null && libraryFilter.value !== LibraryFilter.ANY ? 1 : 0)
  + [toRead.value, owned.value, borrowed.value].filter(v => v != null && v.toLowerCase() !== 'null').length)

if (searchQuery.value != null) {
    search()
  }

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
        <tri-state-filter
          v-model="toRead"
          :label="t('filtering.book_in_list')"
        />
        <tri-state-filter
          v-model="owned"
          :label="t('filtering.owned')"
        />
        <tri-state-filter
          v-model="borrowed"
          :label="t('filtering.borrowed')"
        />
      </template>
    </sort-filter-bar-vue>
    <m-page-header
      :title="t('nav.search')"
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
    <div
      class="mb-6 flex max-w-3xl flex-wrap items-center gap-2"
      role="search"
    >
      <m-text-field
        v-model="searchQuery"
        type="search"
        leading-icon="magnify"
        clearable
        :placeholder="t('labels.search_query')"
        :aria-label="t('labels.search')"
        class="min-w-[min(100%,16rem)] flex-1"
        @keyup.enter="search"
      >
        <template #trailing>
          <button
            v-tooltip="t('labels.query_helper')"
            type="button"
            class="m3-icon-btn"
            :aria-label="t('labels.query_helper')"
            @click="helperOpen = true"
          >
            <i
              class="mdi mdi-code-braces"
              aria-hidden="true"
            />
          </button>
        </template>
      </m-text-field>
      <div class="flex items-center gap-1">
        <button
          type="button"
          class="m3-btn m3-btn--filled"
          @click="search"
        >
          <i
            class="mdi mdi-magnify"
            aria-hidden="true"
          />
          <span>{{ t('labels.search') }}</span>
        </button>
        <a
          v-tooltip="t('labels.search_documentation')"
          href="https://bayang.github.io/jelu-web/usage/search/"
          target="_blank"
          rel="noopener"
          class="m3-icon-btn"
          :aria-label="t('labels.search_documentation')"
        >
          <i
            class="mdi mdi-help-circle-outline"
            aria-hidden="true"
          />
        </a>
      </div>
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
    <book-grid-skeleton v-else-if="progress" />
    <m-empty-state
      v-else-if="searched"
      icon="book-search-outline"
      :title="t('labels.no_results')"
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
    <m-modal v-model="helperOpen">
      <m-dialog
        :headline="t('labels.query_helper')"
        icon="code-braces"
        @close="helperOpen = false"
      >
        <p class="mb-4">
          {{ t('labels.query_helper_text') }}
        </p>
        <div class="m3-chip-set">
          <button
            v-for="term in terms"
            :key="term"
            type="button"
            class="m3-chip m3-chip--assist"
            @click="append(` ${term}:`)"
          >
            <span>{{ term }}</span>
          </button>
        </div>
        <hr class="m3-divider my-4">
        <div class="m3-chip-set">
          <button
            v-for="op in operators"
            :key="op"
            type="button"
            class="m3-chip m3-chip--assist"
            @click="append(` ${op} `)"
          >
            <span>{{ op }}</span>
          </button>
        </div>
        <template #actions>
          <button
            type="button"
            class="m3-btn m3-btn--text"
            @click="helperOpen = false"
          >
            {{ t('labels.close') }}
          </button>
        </template>
      </m-dialog>
    </m-modal>
  </div>
</template>
