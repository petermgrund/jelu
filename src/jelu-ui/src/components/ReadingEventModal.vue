<script setup lang="ts">
import { computed, Ref, ref, watch } from "vue";
import { CreateReadingEvent, ReadingEvent, ReadingEventType } from "../model/ReadingEvent";
import dataService from "../services/DataService";
import { useI18n } from 'vue-i18n'
import MDialog from "./m3/MDialog.vue";
import MField from "./m3/MField.vue";
import MRadioGroup from "./m3/MRadioGroup.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const props = defineProps<{
  readingEvent: ReadingEvent|CreateReadingEvent,
  edit: boolean
}>()

const currentEvent: Ref<ReadingEvent> = ref(props.readingEvent)
const currentCreateEvent: Ref<CreateReadingEvent> = ref(props.readingEvent)
console.log(currentEvent.value)
console.log(currentCreateEvent.value)

watch(() => currentCreateEvent.value.eventType, (newValue, oldValue) => {
  if (currentCreateEvent.value.eventType == ReadingEventType.CURRENTLY_READING) {
    currentCreateEvent.value.eventDate = undefined
    currentCreateEvent.value.startDate = new Date()
  } else {
    currentCreateEvent.value.startDate = undefined
    currentCreateEvent.value.eventDate = new Date()
  }
})

const progress: Ref<boolean> = ref(false)

const emit = defineEmits<{
  (e: 'close'): void
}>()

const create = () => {
  progress.value = true
  if (currentCreateEvent.value.eventType === ReadingEventType.CURRENTLY_READING) {
    console.log("cleaning event date if currently reading")
    currentCreateEvent.value.eventDate = undefined
  }
  dataService.createReadingEvent(currentCreateEvent.value)
    .then(res => {
      progress.value = false
      emit('close')
    })
    .catch(e => {
      progress.value = false
    })
}

const update = () => {
  progress.value = true
  // if user changed a finished event to a currently reading -> remove end date
  if (currentEvent.value.eventType === ReadingEventType.CURRENTLY_READING) {
    currentEvent.value.endDate = undefined
  }
  dataService.updateReadingEvent(currentEvent.value)
    .then(res => {
      progress.value = false
      emit('close')
    })
    .catch(e => {
      progress.value = false
    })
}

const deleteEvent = () => {
  if (currentEvent.value.id != null) {
    progress.value = true
    dataService.deleteReadingEvent(currentEvent.value.id)
    .then(res => {
      progress.value = false
      emit('close')
    })
    .catch(e => {
      progress.value = false
    })
  }
}

const eventTypeOptions = computed(() => [
  { value: ReadingEventType.FINISHED, label: t('reading_events.finished') },
  { value: ReadingEventType.CURRENTLY_READING, label: t('reading_events.currently_reading') },
  { value: ReadingEventType.DROPPED, label: t('reading_events.dropped') },
])
</script>

<template>
  <m-dialog
    :headline="props.edit ? t('reading_events.edit_event') : t('reading_events.choose_event')"
    icon="calendar-edit"
    size="sm"
    @close="emit('close')"
  >
    <div class="flex flex-col gap-4">
      <m-radio-group
        v-model="currentCreateEvent.eventType"
        :options="eventTypeOptions"
        :label="t('reading_events.event_type')"
      />
      <template v-if="props.edit">
        <m-field :label="t('reading_events.start_date')">
          <o-datepicker
            v-model="currentEvent.startDate"
            :show-week-number="false"
            :locale="undefined"
            :placeholder="t('labels.click_to_select')"
            icon="calendar"
            trap-focus
            expanded
          />
        </m-field>
        <m-field
          v-if="currentEvent.eventType !== ReadingEventType.CURRENTLY_READING"
          :label="t('reading_events.event_date')"
        >
          <o-datepicker
            v-model="currentEvent.endDate"
            :show-week-number="false"
            :locale="undefined"
            :placeholder="t('labels.click_to_select')"
            icon="calendar"
            trap-focus
            expanded
          />
        </m-field>
      </template>
      <template v-else>
        <m-field
          v-if="currentEvent.eventType === ReadingEventType.CURRENTLY_READING"
          :label="t('reading_events.start_date')"
        >
          <o-datepicker
            v-model="currentCreateEvent.startDate"
            :show-week-number="false"
            :locale="undefined"
            :placeholder="t('labels.click_to_select')"
            icon="calendar"
            trap-focus
            expanded
          />
        </m-field>
        <m-field
          v-if="currentCreateEvent.eventType != ReadingEventType.CURRENTLY_READING"
          :label="t('reading_events.event_date')"
        >
          <o-datepicker
            v-model="currentCreateEvent.eventDate"
            :show-week-number="false"
            :locale="undefined"
            :placeholder="t('labels.click_to_select')"
            icon="calendar"
            trap-focus
            expanded
          />
        </m-field>
      </template>
      <progress
        v-if="progress"
        class="m3-linear-progress"
      />
    </div>
    <template #actions>
      <button
        v-if="props.edit"
        type="button"
        class="m3-btn m3-btn--text m3-btn--error mr-auto"
        :disabled="progress"
        @click="deleteEvent"
      >
        <i
          class="mdi mdi-delete-outline"
          aria-hidden="true"
        />
        <span>{{ t('labels.delete') }}</span>
      </button>
      <button
        type="button"
        class="m3-btn m3-btn--text"
        @click="emit('close')"
      >
        {{ t('labels.cancel') }}
      </button>
      <button
        v-if="props.edit"
        type="button"
        class="m3-btn m3-btn--filled"
        :disabled="progress"
        @click="update"
      >
        {{ t('labels.save') }}
      </button>
      <button
        v-else
        type="button"
        class="m3-btn m3-btn--filled"
        :disabled="progress"
        @click="create"
      >
        {{ t('labels.create') }}
      </button>
    </template>
  </m-dialog>
</template>
