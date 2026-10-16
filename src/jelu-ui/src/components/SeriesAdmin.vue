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
import { Series } from "../model/Series"
import dataService from "../services/DataService"
import { ObjectUtils } from "../utils/ObjectUtils"
import MEmptyState from "./m3/MEmptyState.vue"
import MField from "./m3/MField.vue"

const oruga = useOruga()

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | Series page')

const { total, page, pageAsNumber, perPage, updatePage, updatePageLoading } = usePagination(12)

const { sortQuery } = useSort('name,desc')

watch([page, sortQuery], (newVal, oldVal) => {
  if (newVal !== oldVal) {
    getOrphanSeries()
  }
})

const orphanSeries: Ref<Array<Series>> = ref([]);
const isOrphanFetching = ref(false)

const filteredSeries: Ref<Array<Series>> = ref([]);
const isFetching = ref(false)

const series: Ref<Series> = ref({name: ""})
const seriesBooks: Ref<Page<Book>|null> = ref(null);
const getBooksIsLoading: Ref<boolean> = ref(false)

function getFilteredSeries(text: string) {
  isFetching.value = true
  dataService.findSeriesByCriteria(text).then((data) => filteredSeries.value = data.content)
  isFetching.value = false
}

function getOrphanSeries() {
  isOrphanFetching.value = true
  dataService.getOrphanSeries(pageAsNumber.value - 1, perPage.value, sortQuery.value)
  .then(
    (res) => {
      total.value = res.totalElements
      orphanSeries.value = res.content
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

const deleteSeries = async (target: Series) => {
  if (target.id) {
    dataService.deleteSeries(target.id)
    .then(res =>
      {
        series.value = {"name" : ""}
        ObjectUtils.toast(oruga, "success", t('labels.operation-success'), 4000);
        getOrphanSeries()
      }
    )
    .catch(err => {
        ObjectUtils.toast(oruga, "danger", t('labels.error_deleting', {msg : err.message}), 4000);
      })
  }
}

const promptDeleteSeries = async (series: Series, numberOfBooks: number|undefined) => {
  let abort = false
  await ObjectUtils.swalYesNoMixin.fire({
      html: `<p>${t('labels.delete_this_series', {nb: numberOfBooks})}</p>`,
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
  deleteSeries(series)
}

const getSeries = async (selected: Series) => {
  try {
    series.value = await dataService.getSeriesById(selected.id as string)
  } catch (error) {
    console.log("failed get series : " + error);
  }
}

const getBooks = (series: Series) => {
    getBooksIsLoading.value = true
    dataService.getSeriesBooksById(series.id as string,
      0, 2, "title:desc", LibraryFilter.ANY)
      .then(res => {
          seriesBooks.value = res
          getBooksIsLoading.value = false
      }
      )
      .catch(e => {
        getBooksIsLoading.value = false
      })
}

const selectSeries = (selected: Series) => {
  getSeries(selected)
  getBooks(selected)
}

getOrphanSeries()

const options = computed(() => {
  return filteredSeries.value.map(t => ObjectUtils.wrapForOptions(t))
})
</script>

<template>
  <div class="grid items-start gap-6 md:grid-cols-2">
    <section class="m3-card m3-card--outlined">
      <header class="flex items-start gap-3 px-6 pt-6 pb-2">
        <i
          class="mdi mdi-book-off-outline text-[1.5rem] text-on-surface-variant"
          aria-hidden="true"
        />
        <div class="flex flex-1 flex-col gap-1">
          <h2 class="text-title-large">
            {{ t('labels.orphan-series') }}
          </h2>
          <p class="text-body-medium text-on-surface-variant">
            {{ t('labels.orphans_description') }}
          </p>
        </div>
      </header>
      <template v-if="orphanSeries.length > 0">
        <ul class="m3-list px-2">
          <li
            v-for="orphanS in orphanSeries"
            :key="orphanS.id"
            class="m3-list-item"
          >
            <span class="m3-list-item__leading">
              <i
                class="mdi mdi-bookshelf"
                aria-hidden="true"
              />
            </span>
            <div class="m3-list-item__content">
              <span class="m3-list-item__headline">{{ orphanS.name }}</span>
            </div>
            <div class="m3-list-item__trailing">
              <button
                v-tooltip="t('labels.delete')"
                type="button"
                class="m3-icon-btn"
                :aria-label="`${t('labels.delete')} ${orphanS.name}`"
                @click="deleteSeries(orphanS)"
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
          class="mdi mdi-book-search-outline text-[1.5rem] text-on-surface-variant"
          aria-hidden="true"
        />
        <h2 class="flex-1 text-title-large">
          {{ t('labels.find-series') }}
        </h2>
      </header>
      <div class="flex flex-col gap-4 p-6 pt-4">
        <m-field leading-icon="magnify">
          <o-autocomplete
            :options="options"
            expanded
            clear-on-select
            backend-filtering
            :loading="isFetching"
            :debounce="100"
            :placeholder="t('labels.search')"
            :aria-label="t('labels.find-series')"
            @input="getFilteredSeries"
            @select="selectSeries"
          >
            <template #default="{ value }">
              <span>{{ value.name }}</span>
            </template>
          </o-autocomplete>
        </m-field>
        <div
          v-if="series.id != null"
          class="m3-card m3-card--filled"
        >
          <div class="flex flex-wrap items-center gap-4 p-4">
            <i
              class="mdi mdi-bookshelf text-[1.5rem] text-on-surface-variant"
              aria-hidden="true"
            />
            <div class="flex min-w-0 flex-1 flex-col">
              <router-link
                class="m3-link self-start text-title-medium"
                :to="{ name: 'series', params: { seriesId: series.id } }"
              >
                {{ series.name }}
              </router-link>
              <span
                v-if="!getBooksIsLoading && seriesBooks != null"
                class="text-body-medium text-on-surface-variant"
              >{{ t('labels.associated_book_count', seriesBooks.totalElements) }}</span>
            </div>
            <button
              type="button"
              class="m3-btn m3-btn--outlined m3-btn--error"
              @click="promptDeleteSeries(series, seriesBooks?.totalElements)"
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
