<script setup lang="ts">
import { computed, Ref, ref, watch } from "vue";
import dataService from "../services/DataService";
import { useI18n } from 'vue-i18n'
import { Tag } from "../model/Tag";
import { ObjectUtils } from "../utils/ObjectUtils";
import { useOruga } from "@oruga-ui/oruga-next";
import { Wrapper } from "../model/autocomplete-wrapper";
import ClosableBadge from "./ClosableBadge.vue";
import MDialog from "./m3/MDialog.vue";
import MField from "./m3/MField.vue";
import MSegmentedButton from "./m3/MSegmentedButton.vue";

const oruga = useOruga()

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const props = defineProps<{
  ids: Array<string>,
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const toRead: Ref<boolean|null> = ref(null)
const owned: Ref<boolean|null> = ref(null)
const add: Ref<boolean> = ref(true)

const filteredTags: Ref<Array<Wrapper>> = ref([]);
const isFetching = ref(false)

function getFilteredTags(text: string) {
  isFetching.value = true
  filteredTags.value.splice(0, filteredTags.value.length)
  dataService.findTagsByCriteria(text).then((data) => data.content.forEach(t => filteredTags.value.push(ObjectUtils.wrapForOptions(t))))
  isFetching.value = false
}

function manageTag(tag: Tag) {
  console.log("tag")
  console.log(tag)
  if (tag != null && tag.id != null) {
    console.log("received tag : " + tag.id + "/" + tag.name)
    if (add.value) {
      if (addTags.value.findIndex(elem => tag.id === elem.id) === -1) {
        addTags.value.push(tag)
      }
    } else {
      if (removeTags.value.findIndex(elem => tag.id === elem.id) === -1) {
        removeTags.value.push(tag)
      }
    }
  }
}

function removeTag(tag: Tag, tagList: Array<Tag>) {
  const idx = tagList.findIndex(elem => tag.id === elem.id);
  if (idx !== -1) {
    tagList.splice(idx, 1)
  }
}

watch(owned, (newVal, oldVal) => {
  console.log("owned " + owned.value)
})

watch(toRead, (newVal, oldVal) => {
  console.log("toRead " + toRead.value)
})

const addTags: Ref<Array<Tag>> = ref([])
const removeTags: Ref<Array<Tag>> = ref([])

const submit = () => {
  if (owned.value != null || toRead.value != null || removeTags.value.length > 0 || addTags.value.length > 0) {
    dataService.bulkEditUserBooks({
      ids: props.ids,
      owned: owned.value != null ? owned.value : undefined,
      toRead: toRead.value != null ? toRead.value : undefined,
      addTags: addTags.value.map(tag => tag.id) as string[],
      removeTags: removeTags.value.map(tag => tag.id) as string[]
    }).then(res => {
      emit('close')
    }).catch(err => {
      console.log("error " + err)
      ObjectUtils.toast(oruga, "danger", t('labels.error_message', {msg : err.message}), 4000)
    })
  }
}

// same condition as submit, the apply button stays disabled while there is nothing to do
const hasChanges = computed(() => owned.value != null || toRead.value != null
  || removeTags.value.length > 0 || addTags.value.length > 0)

const choiceOptions = computed(() => [
  { value: null, label: t('labels.do_nothing') },
  { value: false, label: t('labels.no') },
  { value: true, label: t('labels.yes') },
])

const tagModeOptions = computed(() => [
  { value: true, label: t('bulk.add_tag'), icon: 'tag-plus-outline' },
  { value: false, label: t('bulk.remove_tag'), icon: 'tag-remove-outline' },
])
</script>

<template>
  <m-dialog
    :headline="t('bulk.bulk_edit_books')"
    size="md"
    @close="emit('close')"
  >
    <div class="flex flex-col gap-6">
      <p class="text-body-medium">
        {{ t('labels.selected_count', { count: props.ids.length }) }}
      </p>
      <section class="flex flex-col gap-3">
        <h3 class="text-title-small text-on-surface-variant">
          {{ t('bulk.owned') }}
        </h3>
        <m-segmented-button
          v-model="owned"
          :options="choiceOptions"
          :aria-label="t('bulk.owned')"
          class="*:min-w-fit"
          block
        />
      </section>
      <section class="flex flex-col gap-3">
        <h3 class="text-title-small text-on-surface-variant">
          {{ t('bulk.toRead') }}
        </h3>
        <m-segmented-button
          v-model="toRead"
          :options="choiceOptions"
          :aria-label="t('bulk.toRead')"
          class="*:min-w-fit"
          block
        />
      </section>
      <section class="flex flex-col gap-3">
        <h3 class="text-title-small text-on-surface-variant">
          {{ t('bulk.add_or_remove') }}
        </h3>
        <m-segmented-button
          v-model="add"
          :options="tagModeOptions"
          :aria-label="t('bulk.add_or_remove')"
          block
        />
        <m-field
          :label="t('labels.find-tag')"
          :supporting-text="t('bulk.choose_tag')"
          leading-icon="tag-search-outline"
        >
          <o-autocomplete
            :options="filteredTags"
            :clear-on-select="true"
            field="name"
            backend-filtering
            :loading="isFetching"
            :debounce="100"
            position="top"
            @input="getFilteredTags"
            @select="manageTag"
          >
            <template #default="{ value }">
              <span>{{ value.name }}</span>
            </template>
          </o-autocomplete>
        </m-field>
        <div
          v-if="addTags.length > 0"
          class="flex flex-col gap-2"
        >
          <h4 class="flex items-center gap-2 text-label-large text-on-surface-variant">
            <i
              class="mdi mdi-tag-plus-outline text-[1.125rem] text-primary"
              aria-hidden="true"
            />
            <span>{{ t('bulk.add') }}</span>
          </h4>
          <div class="m3-chip-set">
            <closable-badge
              v-for="tag in addTags"
              :key="tag.id"
              :content="tag.name"
              @closed="removeTag(tag, addTags)"
            />
          </div>
        </div>
        <div
          v-if="removeTags.length > 0"
          class="flex flex-col gap-2"
        >
          <h4 class="flex items-center gap-2 text-label-large text-on-surface-variant">
            <i
              class="mdi mdi-tag-remove-outline text-[1.125rem] text-error"
              aria-hidden="true"
            />
            <span>{{ t('bulk.remove') }}</span>
          </h4>
          <div class="m3-chip-set">
            <closable-badge
              v-for="tag in removeTags"
              :key="tag.id"
              :content="tag.name"
              @closed="removeTag(tag, removeTags)"
            />
          </div>
        </div>
      </section>
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
        :disabled="!hasChanges"
        @click="submit"
      >
        {{ t('labels.apply') }}
      </button>
    </template>
  </m-dialog>
</template>
