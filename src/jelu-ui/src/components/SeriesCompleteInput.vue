<script setup lang="ts">
import { computed, Ref, ref } from "vue";
import { useI18n } from 'vue-i18n';
import { Series, SeriesOrder } from "../model/Series";
import dataService from "../services/DataService";
import { ObjectUtils } from "../utils/ObjectUtils";
import MField from "./m3/MField.vue";
import MTextField from "./m3/MTextField.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })
const model = defineModel<Array<SeriesOrder>>()

console.log("init")
console.log(model.value)

let currentInput = ref("")

let filteredSeries: Ref<Array<Series>> = ref([]);

function getFilteredSeries(text: string) {
  currentInput.value = text
  dataService.findSeriesByCriteria(text).then((data) => filteredSeries.value = data.content)
}

function onSelect(series: Series) {
  // we receive from oruga weird events while nothing is selected
  // so try to get rid of those null data we receive
  if (series != null) {
    model.value?.push({"name": series.name, "seriesId": series.id})
    filteredSeries.value = []
  }
}

const addNew = () => {
  if (currentInput.value != null && currentInput.value.length > 0) {
    model.value?.push({"name": currentInput.value})
    filteredSeries.value = []
  }
}

const isFetching = ref(false)
const options = computed(() => {
  return filteredSeries.value.map(t => ObjectUtils.wrapForOptions(t))
})

</script>

<template>
  <div class="flex w-full flex-col gap-3">
    <m-field :label="t('book.series')">
      <o-autocomplete
        :loading="isFetching"
        open-on-focus
        clear-on-select
        backend-filtering
        :debounce="150"
        :options="options"
        :placeholder="t('labels.find-series')"
        expanded
        @input="getFilteredSeries"
        @select="onSelect"
      >
        <template #default="{ value }">
          <div class="jl-taginput-item">
            {{ value.name }}
          </div>
        </template>
        <template #empty="">
          <div class="jl-taginput-empty">
            <button
              type="button"
              class="m3-btn m3-btn--text pointer-events-auto"
              @click="addNew"
            >
              <i
                class="mdi mdi-plus"
                aria-hidden="true"
              />
              <span>{{ t('series.create_series') }}</span>
            </button>
          </div>
        </template>
      </o-autocomplete>
    </m-field>
    <ul
      v-if="model != null && model.length > 0"
      class="m3-list rounded-md border border-outline-variant py-1"
    >
      <li
        v-for="ser,idx in model"
        :key="ser.name"
        class="m3-list-item gap-3 py-2 pr-2 pl-4"
      >
        <span class="m3-list-item__leading">
          <i
            class="mdi mdi-bookshelf"
            aria-hidden="true"
          />
        </span>
        <span class="m3-list-item__content">
          <span class="m3-list-item__headline">{{ ser.name }}</span>
        </span>
        <m-text-field
          v-model="ser.numberInSeries"
          type="number"
          :label="t('book.nb_in_series')"
          min="0"
          step="0.1"
          class="w-32 shrink-0"
        />
        <button
          v-tooltip="t('labels.delete')"
          type="button"
          class="m3-icon-btn"
          :aria-label="`${t('labels.delete')} ${ser.name}`"
          @click="model?.splice(idx, 1)"
        >
          <i
            class="mdi mdi-close"
            aria-hidden="true"
          />
        </button>
      </li>
    </ul>
  </div>
</template>
