<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { ref, Ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { CustomList } from '../model/custom-list'
import { Tag } from "../model/Tag"
import dataService from "../services/DataService"
import ClosableBadge from "./ClosableBadge.vue"
import MEmptyState from './m3/MEmptyState.vue'
import MField from './m3/MField.vue'
import MTextField from './m3/MTextField.vue'

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | User page')

let filteredTags: Ref<Array<string>> = ref([]);
let customLists: Ref<Array<CustomList>> = ref([])
const isFetching = ref(false)

let currentListTags: Ref<Array<string>> = ref([])

watch(currentListTags, (oldVal, newVal) => {
  customList.value.tags = currentListTags.value.join()
})

const newList = (): CustomList => {
  return {
    name:"", tags: "", actionable: false, public: false
  }
}

let customList: Ref<CustomList> = ref(newList())

let editMode = ref(false)

function getFilteredTags(text: string) {
  isFetching.value = true
  dataService.findTagsByCriteria(text).then((data) => {
    filteredTags.value.splice(0, filteredTags.value.length)
    data.content.forEach(t => filteredTags.value.push(t.name))
  })
  isFetching.value = false
}

function getCustomLists() {
  isFetching.value = true
  dataService.findCustomLists(undefined, 0, 30, null).then(data => customLists.value = data.content)
  isFetching.value = false
}

function deleteList(list: CustomList) {
  dataService.deleteCustomList(list.id as string)
  .then(res => {
    console.log("deleted list " + list.id)
    getCustomLists()
    })
  .catch(err => console.log("failed to delete list " + list.id))
}

function createList() {
  dataService.saveCustomList(customList.value)
    .then(res => {
      resetList()
      getCustomLists()
    })
    .catch(err => {
      console.log("failed to create list")
      resetList()
    })
}

function beforeAddTag(item: Tag | string) {
  let shouldAdd = true
  if (item instanceof Object) {
    currentListTags.value.forEach(tag => {
      console.log(`tag ${tag}`)
      if (tag === item.name) {
        console.log(`tag ${tag} item ${item.name}`)
        shouldAdd = false;
      }
    });
  }
  else {
    currentListTags.value.forEach(tag => {
      console.log(`tag ${tag}`)
      if (tag === item) {
        console.log(`tag ${tag} item ${item}`)
        shouldAdd = false;
      }
    });
  }
  return shouldAdd
}

const resetList = () => {
    editMode.value = false
    currentListTags.value.splice(0)
    customList.value = newList()
}

const editList = (list: CustomList) => {
  if (editMode.value) {
  resetList()
  } else {
    editMode.value = true
    customList.value = list
    currentListTags.value = customList.value.tags.split(",")
  }
}

// tags are stored as a comma separated string
const listTags = (list: CustomList) => list.tags.split(",").filter(tag => tag.length > 0)

getCustomLists()
</script>

<template>
  <div class="flex max-w-3xl flex-col gap-6">
    <section class="m3-card m3-card--outlined">
      <header class="flex items-start gap-3 px-6 pt-6 pb-2">
        <i
          class="mdi mdi-format-list-bulleted text-[1.5rem] text-on-surface-variant"
          aria-hidden="true"
        />
        <h2 class="flex-1 text-title-large">
          {{ t('settings.custom_lists') }}
        </h2>
      </header>
      <ul
        v-if="customLists.length > 0"
        class="m3-list px-2"
      >
        <li
          v-for="list in customLists"
          :key="list.id"
          class="m3-list-item m3-list-item--interactive"
          :class="{ 'm3-list-item--three-line': list.public || list.actionable || listTags(list).length > 0 }"
        >
          <span class="m3-list-item__leading">
            <i
              class="mdi mdi-playlist-star"
              aria-hidden="true"
            />
          </span>
          <div class="m3-list-item__content gap-2">
            <router-link
              class="m3-list-item__headline after:absolute after:inset-0"
              :to="{ name: 'list-detail', params: { listId: list.id } }"
            >
              {{ list.name }}
            </router-link>
            <div
              v-if="list.public || list.actionable"
              class="flex flex-wrap gap-2"
            >
              <span
                v-if="list.public"
                class="m3-status m3-status--secondary"
              >
                <i
                  class="mdi mdi-earth"
                  aria-hidden="true"
                />
                <span>{{ t('lists.public') }}</span>
              </span>
              <span
                v-if="list.actionable"
                class="m3-status m3-status--tertiary"
              >
                <i
                  class="mdi mdi-gesture-tap"
                  aria-hidden="true"
                />
                <span>{{ t('lists.actionable') }}</span>
              </span>
            </div>
            <ul
              v-if="listTags(list).length > 0"
              class="m3-chip-set pointer-events-none"
              :aria-label="t('lists.tags')"
            >
              <li
                v-for="tag in listTags(list)"
                :key="tag"
                class="m3-chip m3-chip--static"
              >
                <i
                  class="mdi mdi-tag-outline"
                  aria-hidden="true"
                />
                <span>{{ tag }}</span>
              </li>
            </ul>
          </div>
          <div class="m3-list-item__trailing relative z-[1]">
            <button
              v-tooltip="editMode ? t('labels.cancel') : t('lists.edit_list')"
              type="button"
              class="m3-icon-btn"
              :aria-label="editMode ? t('labels.cancel') : `${t('lists.edit_list')} ${list.name}`"
              @click="editList(list)"
            >
              <i
                class="mdi"
                :class="editMode ? 'mdi-close' : 'mdi-pencil-outline'"
                aria-hidden="true"
              />
            </button>
            <button
              v-tooltip="t('labels.delete')"
              type="button"
              class="m3-icon-btn"
              :aria-label="`${t('labels.delete')} ${list.name}`"
              @click="deleteList(list)"
            >
              <i
                class="mdi mdi-delete-outline"
                aria-hidden="true"
              />
            </button>
          </div>
        </li>
      </ul>
      <m-empty-state
        v-else
        icon="playlist-star"
        :title="t('labels.list_empty')"
      />
    </section>

    <section class="m3-card m3-card--outlined overflow-visible">
      <div class="m3-card__content gap-6 p-6">
        <div class="flex items-center gap-3">
          <i
            class="mdi text-[1.5rem] text-on-surface-variant"
            :class="editMode ? 'mdi-playlist-edit' : 'mdi-playlist-plus'"
            aria-hidden="true"
          />
          <h2 class="text-title-large">
            {{ editMode ? t("lists.edit_list") : t("lists.create_list") }}
          </h2>
        </div>
        <m-text-field
          v-model="customList.name"
          :label="t('lists.name')"
          leading-icon="label-outline"
        />
        <m-field
          :label="t('lists.tags')"
          :supporting-text="t('lists.tags_desc')"
          leading-icon="tag-outline"
        >
          <o-taginput
            v-model="currentListTags"
            :options="filteredTags"
            :allow-autocomplete="true"
            autocomplete="off"
            :before-adding="beforeAddTag"
            :allow-new="false"
            :allow-duplicates="false"
            :open-on-focus="true"
            field="name"
            :placeholder="t('labels.add_tag')"
            :aria-label="t('lists.tags')"
            @input="getFilteredTags"
          >
            <template #default="{ value }">
              <span class="flex items-center gap-3">
                <i
                  class="mdi mdi-tag-outline text-[1.25rem] text-on-surface-variant"
                  aria-hidden="true"
                />
                <span>{{ value }}</span>
              </span>
            </template>
            <template #selected="{ removeItem, items }">
              <ClosableBadge
                v-for="(item, index) in items"
                :key="item"
                :content="item"
                @closed="removeItem(index, $event)"
              />
            </template>
          </o-taginput>
        </m-field>
        <div class="flex flex-col">
          <label class="m3-choice m3-choice--between">
            <span class="flex flex-col py-2">
              <span>{{ t("lists.actionable") }}</span>
              <span class="text-body-small text-on-surface-variant">{{ t('lists.actionable_desc') }}</span>
            </span>
            <input
              v-model="customList.actionable"
              type="checkbox"
              role="switch"
              class="m3-switch"
            >
          </label>
          <label class="m3-choice m3-choice--between">
            <span>{{ t("lists.public") }}</span>
            <input
              v-model="customList.public"
              type="checkbox"
              role="switch"
              class="m3-switch"
            >
          </label>
        </div>
        <div class="flex flex-wrap justify-end gap-2">
          <button
            v-if="editMode"
            type="button"
            class="m3-btn m3-btn--text"
            @click="resetList"
          >
            {{ t('labels.cancel') }}
          </button>
          <button
            type="button"
            class="m3-btn m3-btn--filled"
            :disabled="currentListTags.length < 1 || customList.name.length < 1"
            @click="createList"
          >
            <i
              class="mdi"
              :class="editMode ? 'mdi-content-save-outline' : 'mdi-plus'"
              aria-hidden="true"
            />
            <span>{{ editMode ? t("lists.update_list") : t("lists.create") }}</span>
          </button>
        </div>
      </div>
    </section>
  </div>
</template>
