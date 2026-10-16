<script setup lang="ts">
import { useThrottleFn, useTitle } from '@vueuse/core';
import { useRouteQuery } from '@vueuse/router';
import { computed, Ref, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import usePagination from '../composables/pagination';
import useSort from '../composables/sort';
import useBulkEdition from '../composables/bulkEdition';
import { isStatusFilter, statusQuery } from '../composables/bookStatus';
import { isOwnershipStatus, ownershipQuery } from '../composables/ownership';
import { UserBook } from '../model/Book';
import dataService from "../services/DataService";
import BookCard from "./BookCard.vue";
import BookGridSkeleton from "./BookGridSkeleton.vue";
import BulkSelectionBar from "./BulkSelectionBar.vue";
import MEmptyState from "./m3/MEmptyState.vue";
import MPageHeader from "./m3/MPageHeader.vue";
import OwnershipFilter from "./OwnershipFilter.vue";
import SortFilterBarVue from "./SortFilterBar.vue";
import StatusFilter from "./StatusFilter.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

useTitle('Jelu | ' + t('nav.random'))

const books: Ref<Array<UserBook>> = ref([]);

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('random,desc')

const { showSelect, selectAll, checkedCards, cardChecked, toggleEdit } = useBulkEdition(modalClosed)

const status: Ref<string> = useRouteQuery('status', 'ALL')
const statusParams = computed(() => statusQuery(isStatusFilter(status.value) ? status.value : null))

const userId: Ref<string|null> = useRouteQuery('userId', null)
const username = ref("")

const getUsername = async () => {
  if (userId.value != null) {
    username.value = await dataService.usernameById(userId.value)
  }
}

getUsername()

const open = ref(false)

const ownership: Ref<string> = useRouteQuery('ownership', 'ALL')
const ownershipParams = computed(() => ownershipQuery(isOwnershipStatus(ownership.value) ? ownership.value : null))

const getRandomIsLoading: Ref<boolean> = ref(false)

const getRandom = async () => {
  getRandomIsLoading.value = true
  try {
    const res = await dataService.findUserBookByCriteria(
      statusParams.value.eventTypes, null, userId.value,
    statusParams.value.toRead, ownershipParams.value.owned, ownershipParams.value.borrowed,
    pageAsNumber.value - 1, perPage.value, sortQuery.value)
    total.value = res.totalElements
    books.value = res.content
    if (! res.empty) {
      page.value =  (res.number + 1).toString(10)
    }
    else {
      page.value = "1"
    }
    getRandomIsLoading.value = false
    updatePageLoading(false)
  } catch (error) {
    console.log("failed get books : " + error);
    getRandomIsLoading.value = false
    updatePageLoading(false)
  }
};

// watches set above sometimes called twice
// so getBooks was sometimes called twice at the same instant
const throttledGetRandom = useThrottleFn(() => {
  getRandom()
}, 100, false)

watch([page, status, sortQuery, ownership], (newVal, oldVal) => {
  console.log("all " + newVal + " " + oldVal)
  if (newVal !== oldVal) {
    throttledGetRandom()
  }
})

function modalClosed() {
  console.log("modal closed")
  throttledGetRandom()
}

const activeFilterCount = computed(() => (status.value !== 'ALL' ? 1 : 0) + (ownership.value !== 'ALL' ? 1 : 0))

getRandom()
</script>

<template>
  <div class="m3-page">
    <sort-filter-bar-vue
      :open="open"
      :order="sortOrder"
      @update:open="open = $event"
      @update:sort-order="sortOrderUpdated"
    >
      <template #filters>
        <status-filter v-model="status" />
        <ownership-filter v-model="ownership" />
      </template>
    </sort-filter-bar-vue>
    <m-page-header :title="t('nav.random')">
      <template #actions>
        <button
          type="button"
          class="m3-btn m3-btn--tonal"
          :disabled="getRandomIsLoading"
          @click="throttledGetRandom()"
        >
          <span
            v-if="getRandomIsLoading"
            class="m3-spinner"
          />
          <i
            v-else
            class="mdi mdi-dice-multiple-outline"
            aria-hidden="true"
          />
          <span>{{ t('labels.shuffle_again') }}</span>
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
    <bulk-selection-bar
      v-model:active="showSelect"
      v-model:select-all="selectAll"
      :count="checkedCards.length"
      @edit="toggleEdit(checkedCards)"
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
        :propose-add="true"
        @update:modal-closed="modalClosed"
        @update:checked="cardChecked"
      />
    </TransitionGroup>
    <book-grid-skeleton v-else-if="getRandomIsLoading" />
    <m-empty-state
      v-else
      icon="dice-multiple-outline"
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
    <o-loading
      v-model:active="getPageIsLoading"
      :full-page="true"
      :can-cancel="true"
    />
  </div>
</template>
