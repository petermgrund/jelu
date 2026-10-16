<script setup lang="ts">
import { Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import { Series } from "../model/Series";
import dataService from "../services/DataService";
import MDialog from "./m3/MDialog.vue";
import MStarRating from "./m3/MStarRating.vue";
import MTextField from "./m3/MTextField.vue";

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const props = defineProps<{
  series: Series,
  edit: boolean,
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const progress: Ref<boolean> = ref(false)

const seriesName = ref(props.edit != null && props.edit === true && props.series.name != null ? props.series.name : "")
const description = ref(props.edit != null && props.edit === true && props.series.description != null ? props.series.description : "")
const rating = ref(props.edit != null && props.edit === true && props.series.userRating != null ? props.series.userRating : undefined)

watch(rating, (newval, oldval) => {
    console.log("rating changed " + newval)
    if (newval != null && newval > 10) {
        rating.value = 10
    }
    if (newval != null && newval < 0) {
        rating.value = 0
    }
})

const editSeries = () => {
  console.log("submit ")
  if (props.series.id != null) {
    progress.value = true
    dataService.updateSeries(props.series.id, {
      description: description.value,
      name: seriesName.value,
      rating: rating.value
    })
      .then(res => {
        progress.value = false
        emit('close')
      })
      .catch(err => {
        progress.value = false
        console.log(err)
      })
  }
}

const submit = () => {
    console.log("not implemented yet")
}
</script>

<template>
  <m-dialog
    :headline="props.edit === true ? t('series.edit_series') : t('series.create_series')"
    size="lg"
    @close="emit('close')"
  >
    <div class="flex flex-col gap-6">
      <m-text-field
        v-model="seriesName"
        :label="t('series.name')"
        leading-icon="bookshelf"
      />
      <div class="flex flex-col gap-2">
        <h3 class="text-title-small text-on-surface-variant">
          {{ t('reviews.rating') }}
        </h3>
        <div class="flex flex-wrap items-center gap-2">
          <m-star-rating
            v-model="rating"
            :label="t('reviews.rating')"
            show-value
          />
          <button
            v-if="rating != null"
            v-tooltip="t('labels.clear')"
            type="button"
            class="m3-icon-btn"
            :aria-label="t('labels.clear')"
            @click="rating = undefined"
          >
            <i
              class="mdi mdi-close-circle-outline"
              aria-hidden="true"
            />
          </button>
        </div>
      </div>
      <div class="flex flex-col gap-2">
        <h3 class="text-title-small text-on-surface-variant">
          {{ t('labels.description') }}
        </h3>
        <v-md-editor
          v-model="description"
          :disabled-menus="['image/upload-image', 'toc', 'save']"
          height="18rem"
        />
      </div>
    </div>
    <template #actions>
      <button
        type="button"
        class="m3-btn m3-btn--text"
        @click="emit('close')"
      >
        {{ t('labels.cancel') }}
      </button>
      <button
        v-if="props.edit == null || props.edit === false"
        type="button"
        class="m3-btn m3-btn--filled"
        :disabled="progress"
        @click="submit"
      >
        <span
          v-if="progress"
          class="m3-spinner"
        />
        <span>{{ t('labels.submit') }}</span>
      </button>
      <button
        v-else
        type="button"
        class="m3-btn m3-btn--filled"
        :disabled="progress"
        @click="editSeries"
      >
        <span
          v-if="progress"
          class="m3-spinner"
        />
        <span>{{ t('labels.save') }}</span>
      </button>
    </template>
  </m-dialog>
</template>
