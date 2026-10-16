<script setup lang="ts">
import { useLocalStorage, useTitle } from '@vueuse/core';
import { BarElement, CategoryScale, ChartData, ChartOptions, Chart as ChartJS, Legend, LinearScale, LineController, LineElement, PointElement, Title, Tooltip } from 'chart.js';
import dayjs from "dayjs";
import { computed, Ref, ref, watch } from "vue";
import { Bar } from 'vue-chartjs';
import { useI18n } from 'vue-i18n';
import useTheme, { cssColor } from '../composables/theme';
import { MonthStats, TotalsStats } from '../model/YearStats';
import dataService from "../services/DataService";
import { ObjectUtils } from '../utils/ObjectUtils';
import MSelect from './m3/MSelect.vue';

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

useTitle('Jelu | Stats')

let currency = localStorage.getItem("JL_CURRENCY")
if (currency == null) {
  currency = "EUR"
}

ChartJS.register(Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale, LineController, PointElement, LineElement)

const getYears = () => {
  dataService.yearsWithStats()
  .then(res => {
    years.value = res
    currentYear.value = res[res.length - 1]
    })
  .catch(e => {
    console.log(e)
  })

}

const storedLanguage = useLocalStorage("jelu_language", "en")

// dataset colors come from the theme, see themedChartData
const getAllStats = () => {
  loading.value = true
  dataService.yearStats()
  .then(res => {
    let labels = res.map(r => r.year)

    const updatedChartData = {
        labels: labels,
        datasets: [
        {
            type: 'line',
            label: t('stats.pages_read'),
            borderWidth: 2,
            yAxisID: 'y2',
            data: res.map(r => r.pageCount)
          },
          {
            label: t('stats.finished'),
            yAxisID: 'y1',
            data: res.map(r => r.finished)
          },
          {
            label: t('stats.dropped'),
            yAxisID: 'y1',
            data: res.map(r => r.dropped)
          },
          {
            label: t('book.price'),
            yAxisID: 'y1',
            data: res.map(r => r.price)
          },
        ]
      } as any // mixed charts typing is broken
      chartData.value = { ...updatedChartData }
      loading.value = false
  })
  .catch(e => {
    console.log(e)
    loading.value = false
  })
}

const monthStats: Ref<Array<MonthStats>> = ref([])

const getYearStats = () => {
  if (currentYear.value != null) {
    loading.value = true
    dataService.monthStatsForYear(currentYear.value)
    .then(res => {
      monthStats.value = res
      let labels = res.map(r => r.month).map(m => {
        const d = dayjs(`2020-${m}-1`).format('MMMM')
        return d
      })

      const updatedChartData = {
          labels: labels,
          datasets: [
          {
            type: 'line',
            label: t('stats.pages_read'),
            borderWidth: 2,
            yAxisID: 'y2',
            data: res.map(r => r.pageCount)
          },
            {
              label: t('stats.finished'),
              yAxisID: 'y1',
              data: res.map(r => r.finished)
            },
            {
              label: t('stats.dropped'),
              yAxisID: 'y1',
              data: res.map(r => r.dropped)
            },
            {
              label: t('book.price'),
              yAxisID: 'y1',
              data: res.map(r => r.price)
            },
          ]
        } as any // mixed charts typing is broken
        yearChartData.value = { ...updatedChartData }
        loading.value = false
    })
    .catch(e => {
      loading.value = false
      console.log(e)
    })
  }
}

const totalStats = () => {
  dataService.totalsStats()
  .then( data => totals.value = data)
  .catch(e => console.log(e))
}

const loading = ref(false)
const chartData = ref<ChartData<'bar'>>({
      datasets: []
})
const yearChartData = ref<ChartData<'bar'>>({
      datasets: []
})

// the charts are drawn on a canvas: read the theme color roles and read them again when the theme changes
const { settings, isDark } = useTheme()

const readPalette = () => ({
  primary: cssColor('primary'),
  secondary: cssColor('secondary'),
  tertiary: cssColor('tertiary'),
  error: cssColor('error'),
  grid: cssColor('outline-variant'),
  text: cssColor('on-surface-variant'),
  tooltip: cssColor('inverse-surface'),
  onTooltip: cssColor('inverse-on-surface'),
})

const palette = ref(readPalette())

watch([settings, isDark], () => {
  palette.value = readPalette()
}, { deep: true, flush: 'post' })

// in the order the datasets are declared: pages read, finished, dropped, price
const datasetColors = computed(() => [palette.value.primary, palette.value.tertiary, palette.value.error, palette.value.secondary])

const withThemeColors = (data: ChartData<'bar'>): ChartData<'bar'> => ({
  ...data,
  datasets: data.datasets.map((dataset, index) => ({
    ...dataset,
    backgroundColor: datasetColors.value[index % datasetColors.value.length],
    borderColor: datasetColors.value[index % datasetColors.value.length],
  })),
})

const themedChartData = computed(() => withThemeColors(chartData.value))
const themedYearChartData = computed(() => withThemeColors(yearChartData.value))

const chartFont = {
  family: "'Roboto Variable', Roboto, system-ui, sans-serif",
  size: 12,
}

const numbers = ["0", "1","2", "3", "4", "5", "6", "7", "8", "9"]
const chartOptions = computed<ChartOptions<'bar'>>(() => ({
      responsive: true,
      maintainAspectRatio: false,
      elements: {
        bar: {
          borderRadius: 4,
        },
      },
      scales : {
        x: {
          grid: {
            display: false,
          },
          border: {
            color: palette.value.grid,
          },
          ticks: {
            color: palette.value.text,
            font: chartFont,
          },
        },
        y1: {
          position: 'left',
          grid: {
            color: palette.value.grid,
          },
          border: {
            color: palette.value.grid,
          },
          ticks: {
            color: palette.value.text,
            font: chartFont,
          },
        },
        y2: {
          position: 'right',
          grid: {
            drawOnChartArea: false,
          },
          border: {
            color: palette.value.grid,
          },
          ticks: {
            color: palette.value.text,
            font: chartFont,
          },
        }
      },
      plugins: {
        legend: {
          labels: {
            color: palette.value.text,
            font: chartFont,
            usePointStyle: true,
            boxWidth: 8,
            boxHeight: 8,
          },
        },
        tooltip: {
            backgroundColor: palette.value.tooltip,
            titleColor: palette.value.onTooltip,
            bodyColor: palette.value.onTooltip,
            titleFont: chartFont,
            bodyFont: chartFont,
            callbacks: {
                label: function(context) {
                    let label = context.dataset.label || '';
                    label += ': ';
                    if (context.dataset.label === t('book.price') && context.parsed.y !== null) {
                      label += ObjectUtils.amountInLocale(context.parsed.y, storedLanguage.value, currency)
                    } else {
                      label += context.parsed.y
                    }
                    return label;
                },
                title: function(ctx) {
                  let label = ctx.length > 0 ? ctx[0].label : ""
                  if (label.length > 0 && label[0] in numbers) {
                    return label
                  }
                  label = t(`date.${label}`) ?? label
                  return label
                }
            }
        }
    }
}))

const years: Ref<Array<number>> = ref([])
const currentYear: Ref<number|null> = ref(null)
const totals: Ref<TotalsStats> = ref({"read": 0, "unread": 0, "dropped": 0, "total" : 0, "price": 0})

watch(currentYear, (newVal, oldVal) => {
  console.log("year " + newVal + " " + oldVal)
  getYearStats()

})

const yearSums = computed(() => {
  let read = 0
  let dropped = 0
  let priceTotal = 0
  let pages = 0
  monthStats.value.forEach(e => {
    read += e.finished
    dropped += e.dropped
    pages += e.pageCount
    priceTotal += e.price
  })
  return {
    read,
    dropped,
    priceTotal,
    pages
  }
})

const loaderFullPage = ref(false)

getAllStats()
getYears()
totalStats()

const formatPrice = (amount: number) => ObjectUtils.amountInLocale(amount, storedLanguage.value, currency)

const totalTiles = computed(() => [
  { key: 'total', label: t('stats.total'), value: totals.value.total, icon: 'bookshelf', class: 'col-span-2 sm:col-span-1' },
  { key: 'read', label: t('stats.read'), value: totals.value.read, icon: 'check-circle-outline', class: '' },
  { key: 'unread', label: t('stats.unread'), value: totals.value.unread, icon: 'bookmark-outline', class: '' },
  { key: 'dropped', label: t('stats.dropped'), value: totals.value.dropped, icon: 'close-circle-outline', class: '' },
  { key: 'price', label: t('stats.prices_sum'), value: formatPrice(totals.value.price), icon: 'cash-multiple', class: '' },
])

const yearTiles = computed(() => [
  { key: 'read', label: t('stats.read'), value: yearSums.value.read, icon: 'check-circle-outline' },
  { key: 'dropped', label: t('stats.dropped'), value: yearSums.value.dropped, icon: 'close-circle-outline' },
  { key: 'price', label: t('stats.prices_sum'), value: formatPrice(yearSums.value.priceTotal), icon: 'cash-multiple' },
  { key: 'pages', label: t('stats.pages_read'), value: yearSums.value.pages, icon: 'book-open-page-variant-outline' },
])

const yearOptions = computed(() => years.value.map(year => ({ label: `${year}`, value: year })))
</script>

<template>
  <div class="flex max-w-5xl flex-col gap-6">
    <dl class="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-5">
      <div
        v-for="tile in totalTiles"
        :key="tile.key"
        class="m3-card m3-card--filled"
        :class="tile.class"
      >
        <div class="m3-card__content gap-1">
          <dt class="flex items-center gap-2 text-label-large text-on-surface-variant">
            <i
              class="mdi text-[1.25rem]"
              :class="`mdi-${tile.icon}`"
              aria-hidden="true"
            />
            <span class="truncate">{{ tile.label }}</span>
          </dt>
          <dd class="truncate text-headline-medium text-on-surface tabular-nums">
            {{ tile.value }}
          </dd>
        </div>
      </div>
    </dl>

    <section class="m3-card m3-card--outlined">
      <div class="m3-card__content gap-6 p-6">
        <div class="flex items-center gap-3">
          <i
            class="mdi mdi-chart-bar text-[1.5rem] text-on-surface-variant"
            aria-hidden="true"
          />
          <h2 class="text-title-large">
            {{ t('stats.all_time') }}
          </h2>
        </div>
        <div class="relative h-72 sm:h-96">
          <Bar
            :data="themedChartData"
            :options="chartOptions"
          />
        </div>
      </div>
    </section>

    <section
      v-if="years != null && years !== undefined && years.length > 0"
      class="m3-card m3-card--outlined"
    >
      <div class="m3-card__content gap-6 p-6">
        <div class="flex flex-wrap items-center gap-3">
          <i
            class="mdi mdi-calendar-month-outline text-[1.5rem] text-on-surface-variant"
            aria-hidden="true"
          />
          <h2 class="flex-1 text-title-large">
            {{ t('stats.yearly_stats') }}
          </h2>
          <m-select
            v-model="currentYear"
            :label="t('stats.choose_year')"
            :placeholder="t('stats.choose_year')"
            :options="yearOptions"
            class="w-full sm:w-48"
          />
        </div>
        <dl class="grid grid-cols-2 gap-4 md:grid-cols-4">
          <div
            v-for="tile in yearTiles"
            :key="tile.key"
            class="m3-card m3-card--filled"
          >
            <div class="m3-card__content gap-1">
              <dt class="flex items-center gap-2 text-label-large text-on-surface-variant">
                <i
                  class="mdi text-[1.25rem]"
                  :class="`mdi-${tile.icon}`"
                  aria-hidden="true"
                />
                <span class="truncate">{{ tile.label }}</span>
              </dt>
              <dd class="truncate text-headline-medium text-on-surface tabular-nums">
                {{ tile.value }}
              </dd>
            </div>
          </div>
        </dl>
        <div class="relative h-72 sm:h-96">
          <Bar
            :data="themedYearChartData"
            :options="chartOptions"
          />
        </div>
      </div>
    </section>
  </div>
  <o-loading
    v-model:active="loading"
    :full-page="true"
    :cancelable="true"
    :overlay="loaderFullPage"
  />
</template>
