<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next"
import { useTitle } from '@vueuse/core'
import { computed, ref, Ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import usePagination from "../composables/pagination"
import useSort from "../composables/sort"
import { Book } from "../model/Book"
import { LibraryFilter } from "../model/LibraryFilter"
import { Page } from "../model/Page"
import { Tag } from "../model/Tag"
import dataService from "../services/DataService"
import { ObjectUtils } from "../utils/ObjectUtils"
import MEmptyState from "./m3/MEmptyState.vue"
import MField from "./m3/MField.vue"

const oruga = useOruga()

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | Tags page')

const { total, page, pageAsNumber, perPage, updatePage, updatePageLoading } = usePagination(12)

const { sortQuery } = useSort('name,desc')

watch([page, sortQuery], (newVal, oldVal) => {
  if (newVal !== oldVal) {
    getOrphanTags()
  }
})

const orphanTags: Ref<Array<Tag>> = ref([]);
const isOrphanFetching = ref(false)

const filteredTags: Ref<Array<Tag>> = ref([]);
const isFetching = ref(false)

const tag: Ref<Tag> = ref({name: ""})
const tagBooks: Ref<Page<Book>|null> = ref(null);
const getBooksIsLoading: Ref<boolean> = ref(false)

function getFilteredTags(text: string) {
  isFetching.value = true
  dataService.findTagsByCriteria(text).then((data) => filteredTags.value = data.content)
  isFetching.value = false
}

function getOrphanTags() {
  isOrphanFetching.value = true
  dataService.getOrphanTags(pageAsNumber.value - 1, perPage.value, sortQuery.value)
  .then(
    (res) => {
      total.value = res.totalElements
      orphanTags.value = res.content
      if (! res.empty) {
          page.value =  (res.number + 1).toString(10)
        }
        else {
          page.value = "1"
        }
        isOrphanFetching.value = false
        updatePageLoading(false)
    }
  )
  .catch(e => {
    isOrphanFetching.value = false
      updatePageLoading(false)
    })
}

const deleteTag = async (target: Tag) => {
  if (target.id) {
    dataService.deleteTag(target.id)
    .then(res =>
      {
        tag.value = {"name" : ""}
        ObjectUtils.toast(oruga, "success", t('labels.operation-success'), 4000);
        getOrphanTags()
      }
    )
    .catch(err => {
        ObjectUtils.toast(oruga, "danger", t('labels.error_deleting', {msg : err.message}), 4000);
      })
  }
}

const promptDeleteTag = async (tag: Tag, numberOfBooks: number|undefined) => {
  let abort = false
  await ObjectUtils.swalYesNoMixin.fire({
      html: `<p>${t('labels.delete_this_tag', {nb: numberOfBooks})}</p>`,
      showCancelButton: true,
      showConfirmButton: true,
      showDenyButton: false,
      confirmButtonText: t('labels.delete'),
      cancelButtonText: t('labels.dont_delete'),
    }).then((result) => {
      if (result.isDismissed) {
        abort = true
      }
    })
    if (abort) {
    return
  }
  deleteTag(tag)
}

const getTag = async (selected: Tag) => {
  try {
    tag.value = await dataService.getTagById(selected.id as string)
  } catch (error) {
    console.log("failed get tag : " + error);
  }
}

const getBooks = (tag: Tag) => {
    getBooksIsLoading.value = true
    dataService.getTagBooksById(tag.id as string,
      0, 2, "title:desc", LibraryFilter.ANY)
      .then(res => {
          tagBooks.value = res
          getBooksIsLoading.value = false
      }
      )
      .catch(e => {
        getBooksIsLoading.value = false
      })
}

const selectTag = (selected: Tag) => {
  getTag(selected)
  getBooks(selected)
}

getOrphanTags()

const options = computed(() => {
  return filteredTags.value.map(t => ObjectUtils.wrapForOptions(t))
})
</script>

<template>
  <div class="grid items-start gap-6 md:grid-cols-2">
    <section class="m3-card m3-card--outlined">
      <header class="flex items-start gap-3 px-6 pt-6 pb-2">
        <i
          class="mdi mdi-tag-off-outline text-[1.5rem] text-on-surface-variant"
          aria-hidden="true"
        />
        <div class="flex flex-1 flex-col gap-1">
          <h2 class="text-title-large">
            {{ t('labels.orphan-tags') }}
          </h2>
          <p class="text-body-medium text-on-surface-variant">
            {{ t('labels.orphans_description') }}
          </p>
        </div>
      </header>
      <template v-if="orphanTags.length > 0">
        <ul class="m3-list px-2">
          <li
            v-for="orphanTag in orphanTags"
            :key="orphanTag.id"
            class="m3-list-item"
          >
            <span class="m3-list-item__leading">
              <i
                class="mdi mdi-tag-outline"
                aria-hidden="true"
              />
            </span>
            <div class="m3-list-item__content">
              <span class="m3-list-item__headline">{{ orphanTag.name }}</span>
            </div>
            <div class="m3-list-item__trailing">
              <button
                v-tooltip="t('labels.delete')"
                type="button"
                class="m3-icon-btn"
                :aria-label="`${t('labels.delete')} ${orphanTag.name}`"
                @click="deleteTag(orphanTag)"
              >
                <i
                  class="mdi mdi-delete-outline"
                  aria-hidden="true"
                />
              </button>
            </div>
          </li>
        </ul>
        <div class="px-4">
          <o-pagination
            :current="pageAsNumber"
            :total="total"
            position="centered"
            :per-page="perPage"
            @change="updatePage"
          />
        </div>
      </template>
      <m-empty-state
        v-else-if="!isOrphanFetching"
        icon="check-circle-outline"
        :title="t('labels.no_orphans')"
      />
    </section>

    <section class="m3-card m3-card--outlined overflow-visible">
      <header class="flex items-start gap-3 px-6 pt-6 pb-2">
        <i
          class="mdi mdi-tag-search-outline text-[1.5rem] text-on-surface-variant"
          aria-hidden="true"
        />
        <h2 class="flex-1 text-title-large">
          {{ t('labels.find-tag') }}
        </h2>
      </header>
      <div class="flex flex-col gap-4 p-6 pt-4">
        <m-field leading-icon="magnify">
          <o-autocomplete
            :options="options"
            expanded
            clear-on-select
            :loading="isFetching"
            backend-filtering
            :debounce="100"
            :placeholder="t('labels.search')"
            :aria-label="t('labels.find-tag')"
            @input="getFilteredTags"
            @select="selectTag"
          >
            <template #default="{ value }">
              <span>{{ value.name }}</span>
            </template>
          </o-autocomplete>
        </m-field>
        <div
          v-if="tag.id != null"
          class="m3-card m3-card--filled"
        >
          <div class="flex flex-wrap items-center gap-4 p-4">
            <i
              class="mdi mdi-tag-outline text-[1.5rem] text-on-surface-variant"
              aria-hidden="true"
            />
            <div class="flex min-w-0 flex-1 flex-col">
              <router-link
                class="m3-link self-start text-title-medium"
                :to="{ name: 'tag-detail', params: { tagId: tag.id } }"
              >
                {{ tag.name }}
              </router-link>
              <span
                v-if="!getBooksIsLoading && tagBooks != null"
                class="text-body-medium text-on-surface-variant"
              >{{ t('labels.associated_book_count', tagBooks.totalElements) }}</span>
            </div>
            <button
              type="button"
              class="m3-btn m3-btn--outlined m3-btn--error"
              @click="promptDeleteTag(tag, tagBooks?.totalElements)"
            >
              <i
                class="mdi mdi-delete-outline"
                aria-hidden="true"
              />
              <span>{{ t('labels.delete') }}</span>
            </button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
