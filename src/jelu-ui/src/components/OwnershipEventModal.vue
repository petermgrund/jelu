<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next";
import dayjs from "dayjs";
import { computed, Ref, ref } from "vue";
import { useI18n } from 'vue-i18n';
import useOwnership, { OWNERSHIP_STATUSES } from "../composables/ownership";
import { OwnershipEvent, OwnershipStatus } from "../model/Ownership";
import dataService from "../services/DataService";
import { ObjectUtils } from "../utils/ObjectUtils";
import MDialog from "./m3/MDialog.vue";
import MField from "./m3/MField.vue";
import MRadioGroup from "./m3/MRadioGroup.vue";
import MTextField from "./m3/MTextField.vue";

/**
 * Records a change of ownership (ie bought, borrowed from the library, given away)
 * or edits an entry of the history.
 */
const props = defineProps<{
  userBookId: string,
  current: OwnershipStatus,
  event?: OwnershipEvent | null,
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const oruga = useOruga()
const { ownershipLabel } = useOwnership()

const editing = props.event?.id != null

// a new entry most likely records a different state than the current one
const nextStatus = (current: OwnershipStatus): OwnershipStatus =>
  current === OwnershipStatus.OWNED ? OwnershipStatus.NOT_OWNED : OwnershipStatus.OWNED

const status: Ref<OwnershipStatus> = ref(props.event?.status ?? nextStatus(props.current))
const date: Ref<Date | null> = ref(editing
  ? (props.event?.eventDate != null ? new Date(props.event.eventDate) : null)
  : new Date())
const note: Ref<string> = ref(props.event?.note ?? '')
const progress = ref(false)

const options = computed(() => OWNERSHIP_STATUSES.map(value => ({ value, label: ownershipLabel(value) })))

// noon keeps the day the same in every time zone
const toIsoDate = (value: Date | null) => value == null ? null : dayjs(value).hour(12).minute(0).second(0).millisecond(0).toISOString()

const save = async () => {
  progress.value = true
  const input = { status: status.value, eventDate: toIsoDate(date.value), note: note.value.trim() || null }
  try {
    if (editing) {
      await dataService.updateOwnershipEvent(props.userBookId, props.event?.id as string, input)
    } else {
      await dataService.createOwnershipEvent(props.userBookId, input)
    }
    emit('close')
  } catch (error) {
    ObjectUtils.toast(oruga, "danger", t('labels.error_message', { msg: (error as Error).message }), 4000)
  } finally {
    progress.value = false
  }
}

const remove = async () => {
  const result = await ObjectUtils.swalYesNoMixin.fire({
    html: `<p>${t('ownership.delete_entry')}</p>`,
    showCancelButton: true,
    showConfirmButton: true,
    showDenyButton: false,
    confirmButtonText: t('labels.delete'),
    cancelButtonText: t('labels.dont_delete'),
  })
  if (!result.isConfirmed) {
    return
  }
  progress.value = true
  try {
    await dataService.deleteOwnershipEvent(props.userBookId, props.event?.id as string)
    emit('close')
  } catch (error) {
    ObjectUtils.toast(oruga, "danger", t('labels.error_message', { msg: (error as Error).message }), 4000)
  } finally {
    progress.value = false
  }
}
</script>

<template>
  <m-dialog
    :headline="editing ? t('ownership.edit_entry') : t('ownership.update')"
    icon="bookshelf"
    size="md"
    @close="emit('close')"
  >
    <div class="flex flex-col gap-5">
      <m-radio-group
        v-model="status"
        :options="options"
        :label="t('ownership.ownership')"
      />
      <m-field
        :label="t('ownership.date')"
        :supporting-text="date == null ? t('ownership.date_unknown') : undefined"
      >
        <o-datepicker
          v-model="date"
          :show-week-number="false"
          :locale="undefined"
          :placeholder="t('labels.click_to_select')"
          icon="calendar"
          trap-focus
          expanded
        />
      </m-field>
      <m-text-field
        v-model="note"
        :label="t('ownership.note')"
        :supporting-text="t('ownership.note_hint')"
        maxlength="1000"
      />
      <progress
        v-if="progress"
        class="m3-linear-progress"
      />
    </div>
    <template #actions>
      <button
        v-if="editing"
        type="button"
        class="m3-btn m3-btn--text m3-btn--error mr-auto"
        :disabled="progress"
        @click="remove"
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
        type="button"
        class="m3-btn m3-btn--filled"
        :disabled="progress"
        @click="save"
      >
        {{ t('labels.save') }}
      </button>
    </template>
  </m-dialog>
</template>
