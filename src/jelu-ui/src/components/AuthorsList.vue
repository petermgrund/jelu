<script setup lang="ts">
import { useThrottleFn, useTitle } from '@vueuse/core';
import { useRouteQuery } from '@vueuse/router';
import { computed, onMounted, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import usePagination from '../composables/pagination';
import useSort from "../composables/sort";
import { Author } from '../model/Author';
import { Role } from '../model/Role';
import dataService from "../services/DataService";
import MChipSet from './m3/MChipSet.vue';
import MEmptyState from './m3/MEmptyState.vue';
import MPageHeader from './m3/MPageHeader.vue';
import MSegmentedButton from './m3/MSegmentedButton.vue';
import MTextField from './m3/MTextField.vue';
import SortFilterBarVue from "./SortFilterBar.vue";

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | ' + t('book.author', 2))

const authors: Ref<Array<Author>> = ref([])

const search_query = ref('')

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading, pageCount } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('name,asc')

const open = ref(false)

const getBookIsLoading: Ref<boolean> = ref(false)

// Filters
const role: Ref<Role> = useRouteQuery('role', Role.ANY)

// --- LocalStorage keys ---
const SORT_BY_KEY = "authorSortBy";
const SORT_ORDER_KEY = "authorSortOrder";

// Restore saved settings
onMounted(() => {
  const savedSortBy = localStorage.getItem(SORT_BY_KEY);
  const savedSortOrder = localStorage.getItem(SORT_ORDER_KEY);
  if (savedSortBy) sortBy.value = savedSortBy;
  if (savedSortOrder) sortOrder.value = savedSortOrder;
});

// Persist changes
watch(sortBy, (newVal) => {
  localStorage.setItem(SORT_BY_KEY, newVal);
});
watch(sortOrder, (newVal) => {
  localStorage.setItem(SORT_ORDER_KEY, newVal);
});

watch([page, role, sortQuery, search_query], (newVal, oldVal) => {
  console.log("all " + newVal + " " + oldVal)
  if (newVal !== oldVal) {
    throttledGetAuthors()
  }
})

const getAuthors = () => {
  getBookIsLoading.value = true
  dataService.findAuthorByCriteria(role.value, search_query.value, pageAsNumber.value -1, perPage.value, sortQuery.value)
  .then(res => {
        console.log(res)
          total.value = res.totalElements
          authors.value = res.content
        if (! res.empty) {
          page.value =  (res.number + 1).toString(10)
        }
        else {
          page.value = "1"
        }
        getBookIsLoading.value = false
        updatePageLoading(false)
  })
  .catch(e => {
      getBookIsLoading.value = false
      updatePageLoading(false)
  })
}

// watches set above sometimes called twice
// so getBooks was sometimes called twice at the same instant
const throttledGetAuthors = useThrottleFn(() => {
  getAuthors()
}, 100, false)

onMounted(() => {
  console.log("Component is mounted!");
});

const sortOptions = computed(() => [
  { value: 'name', label: t('sorting.name') },
  { value: 'creationDate', label: t('sorting.date_added') },
  { value: 'random', label: t('sorting.random') },
])

const roleOptions = computed(() => [
  { value: Role.ANY, label: t('filtering.any') },
  { value: Role.AUTHOR, label: t('book.author', 1) },
  { value: Role.TRANSLATOR, label: t('book.translator', 1) },
  { value: Role.NARRATOR, label: t('book.narrator', 1) },
])

const activeFilterCount = computed(() => role.value != null && role.value !== Role.ANY ? 1 : 0)

try {
  getAuthors();
} catch (error) {
  console.log("failed get authors : " + error);
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
        <m-segmented-button
          v-model="sortBy"
          :options="sortOptions"
          :aria-label="t('sorting.sort_by')"
          block
        />
      </template>
      <template #filters>
        <section class="m3-sheet__section">
          <h3 class="m3-sheet__section-title">
            {{ t('filtering.role') }}
          </h3>
          <m-chip-set
            v-model="role"
            :options="roleOptions"
            :aria-label="t('filtering.role')"
            single
          />
        </section>
      </template>
    </sort-filter-bar-vue>
    <m-page-header :title="t('book.author', 2)">
      <template #actions>
        <m-text-field
          v-model="search_query"
          type="text"
          role="searchbox"
          :placeholder="t('filtering.type_to_filter')"
          :aria-label="t('filtering.filter')"
          leading-icon="magnify"
          enterkeyhint="search"
          clearable
          class="w-full sm:w-72"
        />
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
      </template>
    </m-page-header>
    <o-pagination
      v-if="authors.length > 0 && pageCount > 1"
      :current="pageAsNumber"
      :total="total"
      position="centered"
      :per-page="perPage"
      @change="updatePage"
    />
    <TransitionGroup
      v-if="authors.length > 0"
      name="list"
      tag="div"
      class="jl-book-grid"
    >
      <router-link
        v-for="author in authors"
        :key="author.id"
        :to="{ name: 'author-detail', params: { authorId: author.id } }"
        class="m3-card m3-card--elevated m3-card--interactive"
      >
        <img
          v-if="author.image"
          :src="'/files/' + author.image"
          alt=""
          loading="lazy"
          class="aspect-square w-full object-cover"
        >
        <div
          v-else
          class="grid aspect-square w-full place-items-center bg-surface-container-highest"
        >
          <i
            class="mdi mdi-account-outline text-[3rem] leading-none text-outline"
            aria-hidden="true"
          />
        </div>
        <div class="p-3">
          <span class="line-clamp-2 text-title-small text-on-surface">{{ author.name }}</span>
        </div>
      </router-link>
    </TransitionGroup>
    <div
      v-else-if="getBookIsLoading"
      class="jl-book-grid"
      aria-hidden="true"
    >
      <div
        v-for="index in 12"
        :key="index"
        class="flex flex-col gap-2"
      >
        <div class="m3-skeleton aspect-square w-full" />
        <div class="m3-skeleton h-4 w-3/4 rounded-xs" />
      </div>
    </div>
    <m-empty-state
      v-else
      icon="account-search-outline"
      :title="t('labels.list_empty')"
    />
    <o-pagination
      v-if="authors.length > 0 && pageCount > 1"
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
