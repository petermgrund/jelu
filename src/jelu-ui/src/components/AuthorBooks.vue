<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next";
import { useTitle } from '@vueuse/core';
import { useRouteQuery } from "@vueuse/router";
import { computed, Ref, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import useDates from '../composables/dates';
import usePagination from '../composables/pagination';
import useSort from "../composables/sort";
import { Author } from "../model/Author";
import { Book } from '../model/Book';
import { LibraryFilter } from "../model/LibraryFilter";
import dataService from "../services/DataService";
import { ObjectUtils } from '../utils/ObjectUtils';
import BookCard from "./BookCard.vue";
import BookGridSkeleton from "./BookGridSkeleton.vue";
import EditAuthorModalVue from "./EditAuthorModal.vue";
import MEmptyState from "./m3/MEmptyState.vue";
import MPageHeader from "./m3/MPageHeader.vue";
import MRadioGroup from "./m3/MRadioGroup.vue";
import SortFilterBarVue from "./SortFilterBar.vue";
import { useRoute } from 'vue-router';
import { Role } from "../model/Role";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const name = "author-detail"

const route = useRoute()

const { formatDate, formatDateString } = useDates()

const oruga = useOruga();

const author: Ref<Author> = ref({name: ""})
const authorBooks: Ref<Array<Book>> = ref([]);
const authorEdit: Ref<boolean> = ref(false)

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading, pageCount } = usePagination()

const { sortQuery, sortOrder, sortBy, sortOrderUpdated } = useSort('title,asc')

const libraryFilter: Ref<LibraryFilter> = useRouteQuery('libraryFilter', 'ANY' as LibraryFilter)
const roleFilter: Ref<Role> = useRouteQuery('roleFilter', 'ANY' as Role)

const open = ref(false)

const getBooksIsLoading: Ref<boolean> = ref(false)

watch([() => route.params.authorId, page, sortQuery, libraryFilter, roleFilter], (newVal, oldVal) => {
  console.log(page.value)
  console.log(newVal + " " + oldVal)
  console.log(route.name)
  if (newVal !== oldVal && route.name === name) {
    getBooks()
  }
})

watch(() => route.params.authorId, (newVal, oldVal) => {
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal && route.params.authorId !== undefined) {
    getAuthor()
  }
})

const getAuthor = async () => {
  try {
    author.value = await dataService.getAuthorById(route.params.authorId as string)
    useTitle('Jelu | ' + author.value.name)
  } catch (error) {
    console.log("failed get author : " + error);
  }
};

const getBooks = () => {
  getBooksIsLoading.value = true
  dataService.getAuthorBooksById(route.params.authorId as string,
    pageAsNumber.value - 1, perPage.value, sortQuery.value,
    libraryFilter.value, roleFilter.value)
    .then(res => {
        console.log(res)
          total.value = res.totalElements
          authorBooks.value = res.content
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

const convertedBooks = computed(() => authorBooks.value?.map(b => ObjectUtils.toUserBook(b)))

function modalClosed() {
  console.log("modal closed")
  getBooks()
}

function authorModalClosed() {
  console.log("author modal closed")
  getAuthor()
}

const editAuthor = () => {
  console.log(author.value)
  authorEdit.value = !authorEdit.value
  oruga.modal.open({
    parent: this,
    component: EditAuthorModalVue,
    trapFocus: true,
    active: true,
    canCancel: ['x', 'button', 'outside'],
    scroll: 'clip',
    props: {
      "author": author.value
    },
    onClose: authorModalClosed
  });
}

const hasLinks = computed(() => Boolean(author.value.officialPage || author.value.wikipediaPage
  || author.value.goodreadsPage || author.value.twitterPage
  || author.value.facebookPage || author.value.instagramPage))

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

const roleOptions = computed(() => [
  { value: Role.ANY, label: t('filtering.any') },
  { value: Role.AUTHOR, label: t('book.author') },
  { value: Role.TRANSLATOR, label: t('book.translator') },
  { value: Role.NARRATOR, label: t('book.narrator') },
])

const activeFilterCount = computed(() => (libraryFilter.value != null && libraryFilter.value !== LibraryFilter.ANY ? 1 : 0)
  + (roleFilter.value != null && roleFilter.value !== Role.ANY ? 1 : 0))

getAuthor()
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
            {{ t('filtering.role') }}
          </h3>
          <m-radio-group
            v-model="roleFilter"
            :options="roleOptions"
            :label="t('filtering.role')"
          />
        </section>
      </template>
    </sort-filter-bar-vue>
    <div class="grid gap-6 pt-2 sm:grid-cols-[minmax(0,12rem)_minmax(0,1fr)] md:grid-cols-[minmax(0,15rem)_minmax(0,1fr)] md:gap-10">
      <div class="mx-auto w-full max-w-[12rem] sm:max-w-none">
        <div class="aspect-square w-full overflow-hidden rounded-lg bg-surface-container-highest">
          <img
            v-if="author.image"
            :src="'/files/' + author.image"
            :alt="author.name"
            class="h-full w-full object-cover"
          >
          <div
            v-else
            class="grid h-full w-full place-items-center"
          >
            <i
              class="mdi mdi-account-outline text-[5rem] leading-none text-outline"
              aria-hidden="true"
            />
          </div>
        </div>
      </div>
      <div class="flex min-w-0 flex-col gap-5">
        <m-page-header
          :title="author.name"
          class="mb-0 min-h-0"
        >
          <template #actions>
            <button
              v-tooltip="t('labels.edit_author')"
              type="button"
              class="m3-icon-btn m3-icon-btn--outlined"
              :aria-label="t('labels.edit_author')"
              @click="editAuthor"
            >
              <i
                class="mdi mdi-pencil-outline"
                aria-hidden="true"
              />
            </button>
          </template>
        </m-page-header>
        <div
          v-if="hasLinks"
          class="flex flex-wrap gap-2"
        >
          <a
            v-if="author.officialPage"
            v-tooltip="t('author.official_page')"
            :href="author.officialPage"
            target="_blank"
            rel="noopener"
            class="m3-icon-btn m3-icon-btn--outlined"
            :aria-label="t('author.official_page')"
          ><i
            class="mdi mdi-web"
            aria-hidden="true"
          /></a>
          <a
            v-if="author.wikipediaPage"
            v-tooltip="t('author.wikipedia_page')"
            :href="author.wikipediaPage"
            target="_blank"
            rel="noopener"
            class="m3-icon-btn m3-icon-btn--outlined"
            :aria-label="t('author.wikipedia_page')"
          ><i
            class="mdi mdi-wikipedia"
            aria-hidden="true"
          /></a>
          <a
            v-if="author.goodreadsPage"
            v-tooltip="t('author.goodreads_page')"
            :href="author.goodreadsPage"
            target="_blank"
            rel="noopener"
            class="m3-icon-btn m3-icon-btn--outlined"
            :aria-label="t('author.goodreads_page')"
          ><i
            class="mdi mdi-goodreads"
            aria-hidden="true"
          /></a>
          <a
            v-if="author.twitterPage"
            v-tooltip="t('author.x_page')"
            :href="author.twitterPage"
            target="_blank"
            rel="noopener"
            class="m3-icon-btn m3-icon-btn--outlined"
            :aria-label="t('author.x_page')"
          ><i
            class="mdi mdi-alpha-x-box-outline"
            aria-hidden="true"
          /></a>
          <a
            v-if="author.facebookPage"
            v-tooltip="t('author.facebook_page')"
            :href="author.facebookPage"
            target="_blank"
            rel="noopener"
            class="m3-icon-btn m3-icon-btn--outlined"
            :aria-label="t('author.facebook_page')"
          ><i
            class="mdi mdi-facebook"
            aria-hidden="true"
          /></a>
          <a
            v-if="author.instagramPage"
            v-tooltip="t('author.instagram_page')"
            :href="author.instagramPage"
            target="_blank"
            rel="noopener"
            class="m3-icon-btn m3-icon-btn--outlined"
            :aria-label="t('author.instagram_page')"
          ><i
            class="mdi mdi-instagram"
            aria-hidden="true"
          /></a>
        </div>
        <dl
          v-if="author.dateOfBirth || author.dateOfDeath"
          class="m3-meta"
        >
          <template v-if="author.dateOfBirth">
            <dt>{{ t('author.date_of_birth') }}</dt>
            <dd>{{ formatDate(author.dateOfBirth) }}</dd>
          </template>
          <template v-if="author.dateOfDeath">
            <dt>{{ t('author.date_of_death') }}</dt>
            <dd>{{ formatDate(author.dateOfDeath) }}</dd>
          </template>
        </dl>
        <section
          v-if="author.biography"
          class="flex max-w-3xl flex-col gap-2"
        >
          <h2 class="text-title-medium text-on-surface">
            {{ t('author.biography') }}
          </h2>
          <div
            class="prose max-w-none text-body-large"
            v-html="author.biography"
          />
        </section>
        <section
          v-if="author.notes"
          class="flex max-w-3xl flex-col gap-2"
        >
          <h2 class="text-title-medium text-on-surface">
            {{ t('author.additional_notes') }}
          </h2>
          <div
            class="prose max-w-none text-body-large"
            v-html="author.notes"
          />
        </section>
      </div>
    </div>

    <section class="m3-section">
      <div class="mb-4 flex flex-wrap items-center gap-x-4 gap-y-2">
        <div class="flex min-w-0 flex-1 flex-col gap-1">
          <h2 class="m3-section__title m-0">
            {{ t('labels.books_from_name', { name: author.name }) }}
          </h2>
          <p
            v-if="total > 0"
            class="text-body-medium text-on-surface-variant"
          >
            {{ t('labels.book_count', total) }}
          </p>
        </div>
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
      </div>
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
          :force-select="false"
          :show-select="false"
          :propose-add="true"
          :public="false"
          @update:modal-closed="modalClosed"
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
        order="centered"
        :per-page="perPage"
        @change="updatePage"
      />
    </section>
    <o-loading
      v-model:active="getPageIsLoading"
      :full-page="true"
      :cancelable="true"
    />
  </div>
</template>
