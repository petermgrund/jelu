<script setup lang="ts">
import { computed, Ref, ref } from "vue";
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

// An event marks one change of status, so it has a single date: when the reading started,
// finished or stopped.
const initialDate = (): Date => {
  if (props.edit) {
    const existing = props.readingEvent as ReadingEvent
    return existing.endDate ?? existing.startDate ?? new Date()
  }
  const created = props.readingEvent as CreateReadingEvent
  return created.eventDate ?? created.startDate ?? new Date()
}

const eventType: Ref<ReadingEventType> = ref(props.readingEvent.eventType)
const eventDate: Ref<Date> = ref(initialDate())

const progress: Ref<boolean> = ref(false)

const emit = defineEmits<{
  (e: 'close'): void
}>()

const create = () => {
  progress.value = true
  dataService.createReadingEvent({
    eventType: eventType.value,
    eventDate: eventDate.value,
    bookId: (props.readingEvent as CreateReadingEvent).bookId,
  })
    .then(() => {
      progress.value = false
      emit('close')
    })
    .catch(() => {
      progress.value = false
    })
}

const update = () => {
  progress.value = true
  const existing = props.readingEvent as ReadingEvent
  dataService.updateReadingEvent({
    id: existing.id,
    eventType: eventType.value,
    startDate: eventDate.value,
    endDate: eventDate.value,
  })
    .then(() => {
      progress.value = false
      emit('close')
    })
    .catch(() => {
      progress.value = false
    })
}

const deleteEvent = () => {
  const id = (props.readingEvent as ReadingEvent).id
  if (id != null) {
    progress.value = true
    dataService.deleteReadingEvent(id)
    .then(() => {
      progress.value = false
      emit('close')
    })
    .catch(() => {
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
        v-model="eventType"
        :options="eventTypeOptions"
        :label="t('reading_events.event_type')"
      />
      <m-field :label="t('reading_events.date')">
        <o-datepicker
          v-model="eventDate"
          :show-week-number="false"
          :locale="undefined"
          :placeholder="t('labels.click_to_select')"
          icon="calendar"
          trap-focus
          expanded
        />
      </m-field>
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
