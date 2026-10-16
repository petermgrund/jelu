<script setup lang="ts">
import { Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import { UserBookUpdate } from "../model/Book";
import dataService from "../services/DataService";
import { ObjectUtils } from "../utils/ObjectUtils";
import MDialog from "./m3/MDialog.vue";
import MSlider from "./m3/MSlider.vue";
import MTextField from "./m3/MTextField.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const props = defineProps<{
  userBookId: string,
  pageCount: number|null,
  currentProgress: number|null,
  currentPage: number|null,
}>()

const userBookUpdate: Ref<UserBookUpdate> = ref({id: props.userBookId, percentRead: props.currentProgress, currentPageNumber: props.currentPage})
const progress: Ref<boolean> = ref(false)

const emit = defineEmits<{
  (e: 'close'): void
}>()

const update = () => {
  progress.value = true
  // if user changed a finished event to a currently reading -> remove end date
  dataService.updateUserBook(userBookUpdate.value)
    .then(res => {
      progress.value = false
      emit('close')
    })
    .catch(e => {
      progress.value = false
    })
}

watch(() => [userBookUpdate.value.currentPageNumber, userBookUpdate.value.percentRead],(newVals, oldVals) => {
   if (props.pageCount != null) {
     ObjectUtils.computePages(newVals, oldVals, userBookUpdate.value, props.pageCount)
   }
 })
</script>

<template>
  <m-dialog
    :headline="t('labels.set_progress')"
    icon="progress-check"
    size="sm"
    @close="emit('close')"
  >
    <div class="flex flex-col gap-6">
      <m-slider
        v-model="userBookUpdate.percentRead"
        :label="t('book.percent_read')"
        :disabled="props.pageCount != null"
        show-value
        unit="%"
      />
      <m-text-field
        v-if="props.pageCount != null"
        v-model="userBookUpdate.currentPageNumber"
        type="number"
        :label="t('book.current_page_number')"
        :suffix="`/ ${props.pageCount}`"
        min="0"
        :max="props.pageCount"
        inputmode="numeric"
      />
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
        type="button"
        class="m3-btn m3-btn--filled"
        :disabled="progress"
        @click="update"
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
