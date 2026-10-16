<script setup lang="ts">
import { useThrottleFn, useTitle } from '@vueuse/core';
import { useRouteQuery } from '@vueuse/router';
import { computed, Ref, ref, watch, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import usePagination from '../composables/pagination';
import useSort from '../composables/sort';
import useBulkEdition from '../composables/bulkEdition';
import { UserBook } from '../model/Book';
import { ReadingEventType } from '../model/ReadingEvent';
import dataService from "../services/DataService";
import { asArray } from "../utils/ObjectUtils";
import BookCard from "./BookCard.vue";
import BookGridSkeleton from "./BookGridSkeleton.vue";
import BulkSelectionBar from "./BulkSelectionBar.vue";
import MChipSet from "./m3/MChipSet.vue";
import MEmptyState from "./m3/MEmptyState.vue";
import MPageHeader from "./m3/MPageHeader.vue";
import MRadioGroup from "./m3/MRadioGroup.vue";
import SortFilterBarVue from "./SortFilterBar.vue";
import TriStateFilter from "./TriStateFilter.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

useTitle('Jelu | ' + t('nav.to_read'))

const books: Ref<Array<UserBook>> = ref([]);

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading, pageCount } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('creationDate,desc')

const { showSelect, selectAll, checkedCards, cardChecked, toggleEdit } = useBulkEdition(modalClosed)

const eventTypes: Ref<Array<ReadingEventType>> = useRouteQuery('lastEventTypes', [], { transform: (value: ReadingEventType | Array<ReadingEventType>) => asArray(value) })

const userId: Ref<string|null> = useRouteQuery('userId', null)
const username = ref("")

const getUsername = async () => {
  if (userId.value != null) {
    username.value = await dataService.usernameById(userId.value)
  }
}

getUsername()

const open = ref(false)

const owned: Ref<string|null> = useRouteQuery('owned', "null")
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

const getToReadIsLoading: Ref<boolean> = ref(false)

const getToRead = async () => {
  getToReadIsLoading.value = true
  try {
    const res = await dataService.findUserBookByCriteria(
      eventTypes.value, null, userId.value,
    true, ownedAsBool.value, null,
    pageAsNumber.value - 1, perPage.value, sortQuery.value)
    total.value = res.totalElements
    books.value = res.content
    if (! res.empty) {
      page.value =  (res.number + 1).toString(10)
    }
    else {
      page.value = "1"
    }
    getToReadIsLoading.value = false
    updatePageLoading(false)
    removeIds()
  } catch (error) {
    console.log("failed get books : " + error);
    getToReadIsLoading.value = false
    updatePageLoading(false)
  }
};

const removeIds = () => {
  if (userId.value != null) {
    books.value.forEach(b => b.id = undefined)
  }
}

// --- LocalStorage keys for ToReadList ---
const SORT_BY_KEY = "toReadSortBy";
const SORT_ORDER_KEY = "toReadSortOrder";
const EVENT_TYPES_KEY = "toReadEventTypes";
const OWNED_KEY = "toReadOwned";

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

  const savedOwned = localStorage.getItem(OWNED_KEY);
  if (savedOwned) owned.value = savedOwned;
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
watch(owned, (newVal) => {
  localStorage.setItem(OWNED_KEY, newVal ?? "null");
});



// watches set above sometimes called twice
// so getBooks was sometimes called twice at the same instant
const throttledGetToRead = useThrottleFn(() => {
  getToRead()
}, 100, false)

watch([page, eventTypes, sortQuery, owned], (newVal, oldVal) => {
  console.log("all " + newVal + " " + oldVal)
  if (newVal !== oldVal) {
    throttledGetToRead()
  }
})

function modalClosed() {
  console.log("modal closed")
  throttledGetToRead()
}

const message = computed(() => {
  if (userId.value != null) {
    return t('labels.reading_list_from_name', { name: username.value })
  } else {
    return t('nav.to_read')
  }
} )

const sortOptions = computed(() => [
  { value: 'creationDate', label: t('sorting.date_added_to_list') },
  { value: 'lastReadingEventDate', label: t('sorting.last_reading_event_date') },
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
])

const activeFilterCount = computed(() => eventTypes.value.length
  + (owned.value != null && owned.value.toLowerCase() !== 'null' ? 1 : 0))

getToRead()
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
          v-model="owned"
          :label="t('filtering.owned')"
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
      v-model:current="pageAsNumber"
      :total="total"
      order="centered"
      :per-page="perPage"
      @change="updatePage"
    />
    <TransitionGroup
      v-if="books.length > 0"
      name="jl-fade"
      tag="div"
      class="jl-book-grid"
    >
      <book-card
        v-for="book in books"
        :key="book.id"
        :book="book"
        :force-select="selectAll"
        :public="false"
        :show-select="showSelect"
        :propose-add="userId == null"
        @update:modal-closed="modalClosed"
        @update:checked="cardChecked"
      />
    </TransitionGroup>
    <book-grid-skeleton v-else-if="getToReadIsLoading" />
    <m-empty-state
      v-else
      icon="bookmark-multiple-outline"
      :title="t('labels.nothing_to_read')"
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
