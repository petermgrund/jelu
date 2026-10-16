<script setup lang="ts">
import { useThrottleFn, useTitle } from '@vueuse/core';
import { useRouteQuery } from '@vueuse/router';
import { computed, onMounted, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import useBulkEdition from '../composables/bulkEdition';
import usePagination from '../composables/pagination';
import useSort from "../composables/sort";
import { UserBook } from "../model/Book";
import { ReadingEventType } from "../model/ReadingEvent";
import dataService from "../services/DataService";
import BookCard from "./BookCard.vue";
import BookGridSkeleton from "./BookGridSkeleton.vue";
import BulkSelectionBar from "./BulkSelectionBar.vue";
import MChipSet from "./m3/MChipSet.vue";
import MEmptyState from "./m3/MEmptyState.vue";
import MPageHeader from "./m3/MPageHeader.vue";
import MRadioGroup from "./m3/MRadioGroup.vue";
import SortFilterBarVue from "./SortFilterBar.vue";
import { asArray } from "../utils/ObjectUtils";
import TriStateFilter from "./TriStateFilter.vue";

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | ' + t('nav.my_books'))

const books: Ref<Array<UserBook>> = ref([]);

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading, pageCount } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('lastReadingEventDate,desc')

const { showSelect, selectAll, checkedCards, cardChecked, toggleEdit } = useBulkEdition(modalClosed)

const open = ref(false)

const getBookIsLoading: Ref<boolean> = ref(false)

// Filters
const toRead: Ref<string|null> = useRouteQuery('toRead', "null")
const owned: Ref<string|null> = useRouteQuery('owned', "null")
const borrowed: Ref<string|null> = useRouteQuery('borrowed', "null")
const userId: Ref<string|null> = useRouteQuery('userId', null)
const eventTypes: Ref<Array<ReadingEventType>> = useRouteQuery('lastEventTypes', [], { transform: (value: ReadingEventType | Array<ReadingEventType>) => asArray(value) })
const username = ref("")

// --- LocalStorage keys ---
const SORT_BY_KEY = "bookSortBy";
const SORT_ORDER_KEY = "bookSortOrder";
const EVENT_TYPES_KEY = "bookEventTypes";
const TO_READ_KEY = "bookToRead";
const OWNED_KEY = "bookOwned";
const BORROWED_KEY = "bookBorrowed";

// Restore saved settings
onMounted(() => {
  const savedSortBy = localStorage.getItem(SORT_BY_KEY);
  const savedSortOrder = localStorage.getItem(SORT_ORDER_KEY);
  if (savedSortBy) sortBy.value = savedSortBy;
  if (savedSortOrder) sortOrder.value = savedSortOrder;

  const savedEventTypes = localStorage.getItem(EVENT_TYPES_KEY);
  if (savedEventTypes) {
    try {
      eventTypes.value = JSON.parse(savedEventTypes);
    } catch {}
  }

  const savedToRead = localStorage.getItem(TO_READ_KEY);
  if (savedToRead) toRead.value = savedToRead;

  const savedOwned = localStorage.getItem(OWNED_KEY);
  if (savedOwned) owned.value = savedOwned;

  const savedBorrowed = localStorage.getItem(BORROWED_KEY);
  if (savedBorrowed) borrowed.value = savedBorrowed;
});

// Persist changes
watch(sortBy, (newVal) => {
  localStorage.setItem(SORT_BY_KEY, newVal);
});
watch(sortOrder, (newVal) => {
  localStorage.setItem(SORT_ORDER_KEY, newVal);
});
watch(eventTypes, (newVal) => {
  localStorage.setItem(EVENT_TYPES_KEY, JSON.stringify(newVal));
}, { deep: true });
watch(toRead, (newVal) => {
  localStorage.setItem(TO_READ_KEY, newVal ?? "null");
});
watch(owned, (newVal) => {
  localStorage.setItem(OWNED_KEY, newVal ?? "null");
});
watch(borrowed, (newVal) => {
  localStorage.setItem(BORROWED_KEY, newVal ?? "null");
});


const getUsername = async () => {
  if (userId.value != null) {
    username.value = await dataService.usernameById(userId.value)
  }
}

getUsername()

watch([page, eventTypes, toRead, owned, borrowed, sortQuery], (newVal, oldVal) => {
  console.log("all " + newVal + " " + oldVal)
  if (newVal !== oldVal) {
    throttledGetBooks()
  }
})

const message = computed(() => {
  if (userId.value != null) {
    return t('labels.books_from_name', { name: username.value })
  } else {
    return t('nav.my_books')
  }
} )

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

const getBooks = () => {
  getBookIsLoading.value = true
  dataService.findUserBookByCriteria(eventTypes.value, null, userId.value,
  toReadAsBool.value, ownedAsBool.value, borrowedAsBool.value,
  pageAsNumber.value - 1, perPage.value, sortQuery.value)
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
        getBookIsLoading.value = false
        updatePageLoading(false)
        removeIds()
    }
    )
    .catch(e => {
      getBookIsLoading.value = false
      updatePageLoading(false)
    })

};

const removeIds = () => {
  if (userId.value != null) {
    books.value.forEach(b => b.id = undefined)
  }
}

// watches set above sometimes called twice
// so getBooks was sometimes called twice at the same instant
const throttledGetBooks = useThrottleFn(() => {
  getBooks()
}, 100, false)

onMounted(() => {
  console.log("Component is mounted!");
});

function modalClosed() {
  console.log("modal closed")
  throttledGetBooks()
}

const sortOptions = computed(() => [
  { value: 'lastReadingEventDate', label: t('sorting.last_reading_event_date') },
  { value: 'creationDate', label: t('sorting.date_added') },
  { value: 'title', label: t('sorting.title') },
  { value: 'publisher', label: t('sorting.publisher') },
  { value: 'pageCount', label: t('sorting.page_count') },
  { value: 'usrAvgRating', label: t('sorting.user_avg_rating') },
  { value: 'avgRating', label: t('sorting.avg_rating') },
  { value: 'random', label: t('sorting.random') },
])

const eventTypeOptions = computed(() => [
  { value: ReadingEventType.FINISHED, label: t('reading_events.finished'), icon: 'check-circle-outline' },
  { value: ReadingEventType.CURRENTLY_READING, label: t('reading_events.currently_reading'), icon: 'book-open-page-variant-outline' },
  { value: ReadingEventType.DROPPED, label: t('reading_events.dropped'), icon: 'close-circle-outline' },
  { value: ReadingEventType.NONE, label: t('reading_events.none'), icon: 'circle-outline' },
])

const activeFilterCount = computed(() => eventTypes.value.length
  + [toRead.value, owned.value, borrowed.value].filter(v => v != null && v.toLowerCase() !== 'null').length)

try {
  getBooks();
} catch (error) {
  console.log("failed get books : " + error);
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
      :title="message"
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
      v-if="books.length > 0 && pageCount > 1"
      :current="pageAsNumber"
      :total="total"
      position="centered"
      :per-page="perPage"
      @change="updatePage"
    />
    <TransitionGroup
      v-if="books.length > 0"
      name="list"
      tag="div"
      class="jl-book-grid"
    >
      <book-card
        v-for="book in books"
        :key="book.id"
        :book="book"
        :force-select="selectAll"
        :show-select="showSelect"
        :public="false"
        :propose-add="userId == null"
        @update:modal-closed="modalClosed"
        @update:checked="cardChecked"
      />
    </TransitionGroup>
    <book-grid-skeleton v-else-if="getBookIsLoading" />
    <m-empty-state
      v-else
      icon="bookshelf"
      :title="t('labels.library_empty')"
    >
      <router-link
        :to="{ name: 'add-book' }"
        class="m3-btn m3-btn--filled"
      >
        <i
          class="mdi mdi-plus"
          aria-hidden="true"
        />
        <span>{{ t('nav.add_book') }}</span>
      </router-link>
    </m-empty-state>
    <o-pagination
      v-if="books.length > 0 && pageCount > 1"
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
