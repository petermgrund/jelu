<script setup lang="ts">
import { useThrottleFn, useTitle } from '@vueuse/core';
import { useRouteQuery } from '@vueuse/router';
import { computed, onMounted, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import { isStatusFilter, statusQuery } from '../composables/bookStatus';
import useBulkEdition from '../composables/bulkEdition';
import { isOwnershipStatus, ownershipQuery } from '../composables/ownership';
import usePagination from '../composables/pagination';
import useSort from "../composables/sort";
import { UserBook } from "../model/Book";
import { OwnershipStatus } from "../model/Ownership";
import dataService from "../services/DataService";
import BookCard from "./BookCard.vue";
import BookGridSkeleton from "./BookGridSkeleton.vue";
import BulkSelectionBar from "./BulkSelectionBar.vue";
import MEmptyState from "./m3/MEmptyState.vue";
import MPageHeader from "./m3/MPageHeader.vue";
import MRadioGroup from "./m3/MRadioGroup.vue";
import OwnershipFilter from "./OwnershipFilter.vue";
import SortFilterBarVue from "./SortFilterBar.vue";
import StatusFilter from "./StatusFilter.vue";

/** The user's library, or with `shelf` only the books they own. */
const props = withDefaults(defineProps<{
  shelf?: boolean,
}>(), {
  shelf: false,
})

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | ' + (props.shelf ? t('nav.on_my_shelf') : t('nav.my_books')))

const books: Ref<Array<UserBook>> = ref([]);

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading, pageCount } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('lastReadingEventDate,desc')

const { showSelect, selectAll, checkedCards, cardChecked, toggleEdit } = useBulkEdition(modalClosed)

const open = ref(false)

const getBookIsLoading: Ref<boolean> = ref(false)

// Filters
const status: Ref<string> = useRouteQuery('status', 'ALL')
const ownership: Ref<string> = useRouteQuery('ownership', 'ALL')
const userId: Ref<string|null> = useRouteQuery('userId', null)
const username = ref("")

// --- LocalStorage keys, the shelf remembers its own settings ---
const KEY_PREFIX = props.shelf ? "shelf" : "book"
const SORT_BY_KEY = `${KEY_PREFIX}SortBy`;
const SORT_ORDER_KEY = `${KEY_PREFIX}SortOrder`;
const STATUS_KEY = `${KEY_PREFIX}Status`;
const OWNERSHIP_KEY = `${KEY_PREFIX}Ownership`;

// Restore saved settings
onMounted(() => {
  const savedSortBy = localStorage.getItem(SORT_BY_KEY);
  const savedSortOrder = localStorage.getItem(SORT_ORDER_KEY);
  if (savedSortBy) sortBy.value = savedSortBy;
  if (savedSortOrder) sortOrder.value = savedSortOrder;

  const savedStatus = localStorage.getItem(STATUS_KEY);
  if (savedStatus && status.value === 'ALL') status.value = savedStatus;

  const savedOwnership = localStorage.getItem(OWNERSHIP_KEY);
  if (savedOwnership && ownership.value === 'ALL' && !props.shelf) ownership.value = savedOwnership;
});

// Persist changes
watch(sortBy, (newVal) => {
  localStorage.setItem(SORT_BY_KEY, newVal);
});
watch(sortOrder, (newVal) => {
  localStorage.setItem(SORT_ORDER_KEY, newVal);
});
watch(status, (newVal) => {
  localStorage.setItem(STATUS_KEY, newVal ?? "ALL");
});
watch(ownership, (newVal) => {
  localStorage.setItem(OWNERSHIP_KEY, newVal ?? "ALL");
});

const getUsername = async () => {
  if (userId.value != null) {
    username.value = await dataService.usernameById(userId.value)
  }
}

getUsername()

watch([page, status, ownership, sortQuery], (newVal, oldVal) => {
  console.log("all " + newVal + " " + oldVal)
  if (newVal !== oldVal) {
    throttledGetBooks()
  }
})

const message = computed(() => {
  if (userId.value != null) {
    return t('labels.books_from_name', { name: username.value })
  } else if (props.shelf) {
    return t('nav.on_my_shelf')
  } else {
    return t('nav.my_books')
  }
} )

const statusParams = computed(() => statusQuery(isStatusFilter(status.value) ? status.value : null))

const ownershipParams = computed(() => {
  if (props.shelf) {
    return ownershipQuery(OwnershipStatus.OWNED)
  }
  return ownershipQuery(isOwnershipStatus(ownership.value) ? ownership.value : null)
})

const getBooks = () => {
  getBookIsLoading.value = true
  dataService.findUserBookByCriteria(statusParams.value.eventTypes, null, userId.value,
  statusParams.value.toRead, ownershipParams.value.owned, ownershipParams.value.borrowed,
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

const activeFilterCount = computed(() => (status.value !== 'ALL' ? 1 : 0)
  + (!props.shelf && ownership.value !== 'ALL' ? 1 : 0))

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
        <status-filter v-model="status" />
        <ownership-filter
          v-if="!props.shelf"
          v-model="ownership"
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
      :title="activeFilterCount > 0 ? t('labels.no_results') : (props.shelf ? t('labels.shelf_empty') : t('labels.library_empty'))"
      :text="props.shelf && activeFilterCount === 0 ? t('labels.shelf_empty_hint') : undefined"
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
