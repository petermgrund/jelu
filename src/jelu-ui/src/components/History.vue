<script setup lang="ts">
import { useThrottleFn, useTitle } from '@vueuse/core';
import dayjs from "dayjs";
import { computed, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import usePagination from '../composables/pagination';
import { ReadingEventType, ReadingEventWithUserBook } from '../model/ReadingEvent';
import dataService from "../services/DataService";
import BookCard from "./BookCard.vue";
import BookGridSkeleton from "./BookGridSkeleton.vue";
import MEmptyState from "./m3/MEmptyState.vue";
import MPageHeader from "./m3/MPageHeader.vue";
import MSelect from "./m3/MSelect.vue";
import localizedFormat from 'dayjs/plugin/localizedFormat'

const { t, d } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

useTitle('Jelu | ' + t('nav.history'))

dayjs.extend(localizedFormat)

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading, pageCount } = usePagination()

const nonCurrentlyReadingEvents: Array<ReadingEventType> = [ReadingEventType.DROPPED, ReadingEventType.FINISHED]

const yearEvents: Ref<Array<ReadingEventWithUserBook>> = ref([])

// the empty state is only shown once the years have been fetched
const yearsLoaded = ref(false)

const getYears = () => {
  dataService.yearsWithStats()
  .then(res => {
    years.value = res
    currentYear.value = res[res.length - 1]
    yearsLoaded.value = true
    })
  .catch(e => {
    console.log(e)
    yearsLoaded.value = true
  })

}

const eventsForYear = () => {
  if (currentYear.value !== null) {
    updatePageLoading(true)
    dataService.myReadingEvents(nonCurrentlyReadingEvents, undefined, undefined, undefined, `${currentYear.value}-01-01`, `${currentYear.value + 1}-01-01`, pageAsNumber.value - 1, perPage.value, 'endDate,desc')
    .then(res => {
      total.value = res.totalElements
      yearEvents.value = res.content
      if (! res.empty) {
          page.value =  (res.number + 1).toString(10)
        }
        else {
          page.value = "1"
        }
        loading.value = false
        updatePageLoading(false)
    })
    .catch(e => {
      console.log(e)
      loading.value = false
      updatePageLoading(false)
    })
  }
}

const loading = ref(false)

const years: Ref<Array<number>> = ref([])
const currentYear: Ref<number|null> = ref(null)

watch(currentYear, (newVal, oldVal) => {
  console.log("year " + newVal + " " + oldVal)
  eventsForYear()
})

const loaderFullPage = ref(false)

// watches set above sometimes called twice
// so getBooks was sometimes called twice at the same instant
const throttledGetEvents = useThrottleFn(() => {
  eventsForYear()
}, 100, false)

watch(page, (newVal, oldVal) => {
  console.log("page " + newVal + " " + oldVal)
  if (newVal !== oldVal) {
    throttledGetEvents()
  }
})

const eventsByMonth: Ref<Map<number, Array<ReadingEventWithUserBook>>> = computed(() => {
  const monthEvents = new Map();
  yearEvents.value.forEach(ev => {
    let month = ev.endDate?.getMonth()
    if (month !== undefined) {
      month ++
    }
    if (!monthEvents.has(month)) {
      monthEvents.set(month, [ev])
    } else {
      const existing: Array<ReadingEventWithUserBook> = monthEvents.get(month)
      existing.push(ev)
      monthEvents.set(month, existing)
    }
  })
  console.log(monthEvents)
  return monthEvents
})

const yearOptions = computed(() => years.value.map(year => ({ label: `${year}`, value: year })))

getYears()

</script>

<template>
  <div class="m3-page">
    <m-page-header
      :title="t('nav.history')"
      :subtitle="total > 0 ? t('labels.book_count', total) : undefined"
    >
      <template
        v-if="years != null && years !== undefined && years.length > 0"
        #actions
      >
        <m-select
          v-model="currentYear"
          :options="yearOptions"
          :placeholder="t('stats.choose_year')"
          :aria-label="t('stats.choose_year')"
          leading-icon="calendar-month-outline"
          class="w-44"
        />
      </template>
    </m-page-header>
    <o-pagination
      v-if="yearEvents.length > 0 && pageCount > 1"
      :current="pageAsNumber"
      :total="total"
      position="centered"
      :per-page="perPage"
      @change="updatePage"
    />
    <div v-if="yearEvents.length > 0">
      <section
        v-for="[month, ev] in eventsByMonth"
        :key="month"
        class="m3-section first:mt-0"
      >
        <h2 class="m3-section__title">
          <i18n-d
            tag="span"
            :value="dayjs(`2020-${month}-1`).toDate()"
            :format="{month: 'long'}"
          >
            <template #month="props">
              <span class="inline-block first-letter:uppercase">{{ props.month }}</span>
            </template>
          </i18n-d>
        </h2>
        <div class="jl-book-grid">
          <book-card
            v-for="event in ev"
            :key="event.id"
            :book="event.userBook"
            :public="false"
            :force-select="false"
            :show-select="false"
            :propose-add="true"
          >
            <template #date>
              <span class="m3-status m3-status--outlined mt-1 self-start"><i
                class="mdi mdi-calendar-check-outline"
                aria-hidden="true"
              /><span>{{ d(event.endDate!!, 'short') }}</span></span>
            </template>
          </book-card>
        </div>
      </section>
    </div>
    <book-grid-skeleton v-else-if="!yearsLoaded || getPageIsLoading" />
    <m-empty-state
      v-else
      icon="calendar-month-outline"
      :title="t('history.empty')"
    />
    <o-pagination
      v-if="yearEvents.length > 0 && pageCount > 1"
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
      :overlay="loaderFullPage"
    />
  </div>
</template>
