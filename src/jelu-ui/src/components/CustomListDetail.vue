<script setup lang="ts">
import { useThrottleFn, useTitle } from '@vueuse/core';
import { computed, Ref, ref, watch } from "vue";
import { useRoute } from 'vue-router';
import usePagination from '../composables/pagination';
import useSort from "../composables/sort";
import { Book } from "../model/Book";
import { CustomList } from '../model/custom-list';
import dataService from "../services/DataService";
import { ObjectUtils } from '../utils/ObjectUtils';
import BookCard from "./BookCard.vue";
import BookGridSkeleton from "./BookGridSkeleton.vue";
import MEmptyState from "./m3/MEmptyState.vue";
import MPageHeader from "./m3/MPageHeader.vue";
import useBulkEdition from '../composables/bulkEdition';
import { useI18n } from 'vue-i18n';

const route = useRoute()
const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const getBookIsLoading: Ref<boolean> = ref(false)
const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading, pageCount } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('lastReadingEventDate,desc')
const { showSelect, selectAll, checkedCards, cardChecked, toggleEdit } = useBulkEdition(modalClosed)
const list: Ref<CustomList|null> = ref(null)
const books: Ref<Array<Book>> = ref([]);

function modalClosed() {
  console.log("modal closed")
}

watch(() => route.params.listId, (newVal, oldVal) => {
  if (newVal !== oldVal && route.params.listId !== undefined) {
    getList()
  }
})

watch([page, sortQuery], (newVal, oldVal) => {
  console.log("all " + newVal + " " + oldVal)
  if (newVal !== oldVal) {
    throttledGetBooks()
  }
})

const getBooks = (listId: string) => {
  getBookIsLoading.value = true
  dataService.booksForList(listId,
  pageAsNumber.value - 1, perPage.value, sortQuery.value)
  .then(res => {
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
    }
    )
    .catch(e => {
      getBookIsLoading.value = false
      updatePageLoading(false)
    })
};

// watches set above sometimes called twice
// so getBooks was sometimes called twice at the same instant
const throttledGetBooks = useThrottleFn(() => {
  getBooks(list.value?.id as string)
}, 100, false)

const getList = async () => {
  try {
    console.log("get list " + route.params.listId)
    list.value = await dataService.findCustomListById(route.params.listId as string)
    useTitle('Jelu | ' + list.value.name)
    getBooks(list.value.id as string)
  } catch (error) {
    console.log("failed get list : " + error);
  }
}

const remove = async () => {
  let tags = list.value?.tags.split(",")
  let tagsId: Array<string> = []
  tags?.forEach(t => {
    for (let ta of books?.value[0].tags) {
      if (ta.name == t) {
        tagsId.push(ta.id as string)
      }
    }
  })
  console.log("removing tags from books")
  console.log(tagsId)
  console.log(checkedCards.value)
  dataService.removeBooksFromList({tags: tagsId, books: checkedCards.value})
  .then(res => {
    getList()
  })
  .catch(err => console.log("failed to remove books from list"))
}

const convertedBooks = computed(() => books.value?.map(b => ObjectUtils.unwrapUserBook(b)))
getList()
</script>

<template>
  <div class="m3-page">
    <m-page-header :title="list?.name">
      <template
        v-if="total > 0 || list?.public || list?.actionable"
        #subtitle
      >
        <span class="flex flex-wrap items-center gap-2">
          <span v-if="total > 0">{{ t('labels.book_count', total) }}</span>
          <span
            v-if="list?.public"
            class="m3-status m3-status--secondary"
          ><i
            class="mdi mdi-earth"
            aria-hidden="true"
          /><span>{{ t('lists.public') }}</span></span>
          <span
            v-if="list?.actionable"
            v-tooltip="t('lists.actionable_desc')"
            class="m3-status m3-status--secondary"
          ><i
            class="mdi mdi-gesture-tap"
            aria-hidden="true"
          /><span>{{ t('lists.actionable') }}</span></span>
        </span>
      </template>
      <template
        v-if="list?.actionable"
        #actions
      >
        <button
          type="button"
          class="m3-btn m3-btn--tonal"
          :disabled="checkedCards.length < 1"
          @click="remove"
        >
          <i
            class="mdi mdi-playlist-remove"
            aria-hidden="true"
          />
          <span>{{ t("lists.remove") }}</span>
          <span
            v-if="checkedCards.length > 0"
            class="m3-badge"
          >{{ checkedCards.length }}</span>
        </button>
      </template>
    </m-page-header>
    <o-pagination
      v-if="books.length > 0 && pageCount > 1"
      v-model:current="pageAsNumber"
      :total="total"
      position="centered"
      :per-page="perPage"
      @change="updatePage"
    />
    <div
      v-if="books.length > 0"
      class="jl-book-grid"
    >
      <book-card
        v-for="book in convertedBooks"
        :key="book.book.id"
        :book="book"
        :force-select="false"
        :show-select="list?.actionable as boolean"
        :propose-add="false"
        :public="true"
        @update:checked="cardChecked"
      />
    </div>
    <book-grid-skeleton v-else-if="getBookIsLoading" />
    <m-empty-state
      v-else
      icon="format-list-bulleted"
      :title="t('lists.empty')"
    />
    <o-pagination
      v-if="books.length > 0 && pageCount > 1"
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
