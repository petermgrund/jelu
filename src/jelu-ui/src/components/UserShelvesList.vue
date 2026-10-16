<script setup lang="ts">
import { computed, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import usePagination from "../composables/pagination";
import { Shelf } from "../model/Shelf";
import dataService from "../services/DataService";
import { ObjectUtils } from "../utils/ObjectUtils";
import { Tag } from "../model/Tag";
import { useOruga } from "@oruga-ui/oruga-next";
import { useTitle } from "@vueuse/core";
import MEmptyState from "./m3/MEmptyState.vue";
import MField from "./m3/MField.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })
useTitle('Jelu | User page')

const props = defineProps<{
  allowDelete: boolean,
  allowCreate: boolean
}>()
const oruga = useOruga()

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading, pageCount } = usePagination(12, false)

const sortQuery = ref('name,asc')
const filteredTags: Ref<Array<Tag>> = ref([]);
const isFetching = ref(false)

function getFilteredTags(text: string) {
  isFetching.value = true
  dataService.findTagsByCriteria(text).then((data) => filteredTags.value = data.content)
  isFetching.value = false
}

const shelves: Ref<Array<Shelf>> = ref([])

const emit = defineEmits<{
  (e: 'close'): void,
  (e: 'shelves-changed'): void
}>()

const getShelves = () => {
  getPageIsLoading.value = true
  dataService.shelves(undefined, undefined,
  pageAsNumber.value - 1, perPage.value, sortQuery.value)
  .then(res => {
        console.log(res)
          total.value = res.totalElements
          shelves.value = res.content
        if (! res.empty) {
          page.value =  (res.number + 1).toString(10)
        }
        else {
          page.value = "1"
        }
        getPageIsLoading.value = false
        updatePageLoading(false)
    }
    )
    .catch(e => {
      getPageIsLoading.value = false
      updatePageLoading(false)
    })
}

function deleteShelf(shelf: Shelf) {
  dataService.deleteShelf(shelf.id)
  .then(res => {
    console.log("deleted shelf " + shelf.id)
    emit('shelves-changed')
    getShelves()
    })
  .catch(err => console.log("failed to delete shelf " + shelf.id))
}

const options = computed(() => {
  return filteredTags.value.map(t => ObjectUtils.wrapForOptions(t))
})

function createShelfFromTag(tag: Tag) {
  console.log(tag)
  // we receive from oruga weird events while nothing is selected
  // so try to get rid of those null data we receive
  if (tag != null && tag.id != null) {
    dataService.saveShelf({name: tag.name, targetId: tag.id ?? ""})
      .then(res => {
        console.log("saved shelf " + res.name)
        // store.dispatch('getUserShelves')
        filteredTags.value = []
        getShelves()
      })
      .catch(err => {
        console.log("failed to save shelf " + tag.name + " " + err)
        ObjectUtils.toast(oruga, "danger", t('labels.error_message'), 4000)
    })
  }
}

// the tag picked in the autocomplete, the add button creates its shelf
const selectedTag: Ref<Tag | undefined> = ref()
const tagQuery = ref('')

function addShelf() {
  if (selectedTag.value != null) {
    createShelfFromTag(selectedTag.value)
    selectedTag.value = undefined
    tagQuery.value = ''
  }
}

watch([page, sortQuery], (newVal, oldVal) => {
  console.log("all " + newVal + " " + oldVal)
  if (newVal !== oldVal) {
    getShelves()
  }
})

getShelves()

const sortOptions = computed(() => [
  { value: 'name,asc', icon: 'sort-alphabetical-ascending', label: `${t('sorting.name')} · ${t('sorting.ascending')}` },
  { value: 'name,desc', icon: 'sort-alphabetical-descending', label: `${t('sorting.name')} · ${t('sorting.descending')}` },
  { value: 'modificationDate,asc', icon: 'sort-calendar-ascending', label: `${t('sorting.modification_date')} · ${t('sorting.ascending')}` },
  { value: 'modificationDate,desc', icon: 'sort-calendar-descending', label: `${t('sorting.modification_date')} · ${t('sorting.descending')}` },
])
</script>

<template>
  <div
    class="flex flex-col"
    :class="{ 'max-w-3xl': props.allowCreate }"
  >
    <section
      class="flex flex-col"
      :class="{ 'm3-card m3-card--outlined overflow-visible': props.allowCreate }"
    >
      <template v-if="props.allowCreate">
        <div class="m3-card__content gap-6 p-6">
          <div class="flex items-start gap-3">
            <i
              class="mdi mdi-library-shelves text-[1.5rem] text-on-surface-variant"
              aria-hidden="true"
            />
            <div class="flex flex-1 flex-col gap-1">
              <h2 class="text-title-large">
                {{ t('settings.shelves') }}
              </h2>
              <p class="text-body-medium text-on-surface-variant">
                {{ t('settings.shelf_choose_tag') }}
              </p>
            </div>
          </div>
          <div class="flex flex-col gap-3 sm:flex-row sm:items-start">
            <m-field
              :label="t('labels.find-tag')"
              leading-icon="tag-outline"
              class="flex-1"
            >
              <o-autocomplete
                v-model="selectedTag"
                v-model:input="tagQuery"
                :loading="isFetching"
                open-on-focus
                backend-filtering
                :debounce="100"
                :options="options"
                :aria-label="t('labels.find-tag')"
                @input="getFilteredTags"
              >
                <template #default="{ value }">
                  <span class="flex items-center gap-3">
                    <i
                      class="mdi mdi-tag-outline text-[1.25rem] text-on-surface-variant"
                      aria-hidden="true"
                    />
                    <span>{{ value.name }}</span>
                  </span>
                </template>
              </o-autocomplete>
            </m-field>
            <button
              type="button"
              class="m3-btn m3-btn--filled self-end sm:mt-2 sm:self-auto"
              :disabled="selectedTag == null"
              @click="addShelf"
            >
              <i
                class="mdi mdi-plus"
                aria-hidden="true"
              />
              <span>{{ t('labels.add') }}</span>
            </button>
          </div>
        </div>
        <hr class="m3-divider">
      </template>

      <div
        v-if="shelves.length > 0"
        class="flex items-center gap-2"
        :class="props.allowCreate ? 'px-6 pt-3' : 'pb-1'"
      >
        <span class="flex-1 text-title-small text-on-surface-variant">{{ t('sorting.sort_by') }}</span>
        <div
          class="flex items-center gap-1"
          role="radiogroup"
          :aria-label="t('sorting.sort_by')"
        >
          <button
            v-for="option in sortOptions"
            :key="option.value"
            v-tooltip="option.label"
            type="button"
            role="radio"
            class="m3-icon-btn m3-icon-btn--sm"
            :class="{ 'is-selected bg-secondary-container text-on-secondary-container': sortQuery === option.value }"
            :aria-checked="sortQuery === option.value"
            :aria-label="option.label"
            @click="sortQuery = option.value"
          >
            <i
              class="mdi"
              :class="`mdi-${option.icon}`"
              aria-hidden="true"
            />
          </button>
        </div>
      </div>

      <progress
        v-if="getPageIsLoading"
        class="m3-linear-progress mt-2"
      />
      <ul
        v-if="shelves.length > 0"
        class="m3-list"
        :class="props.allowCreate ? 'px-2' : '-mx-6'"
      >
        <li
          v-for="shelf in shelves"
          :key="shelf.id"
          class="m3-list-item m3-list-item--two-line m3-list-item--interactive"
          :class="{ 'px-6': !props.allowCreate }"
        >
          <span class="m3-list-item__leading">
            <i
              class="mdi mdi-library-shelves"
              aria-hidden="true"
            />
          </span>
          <div class="m3-list-item__content">
            <router-link
              class="m3-list-item__headline whitespace-nowrap after:absolute after:inset-0"
              :to="{ name: 'tag-detail', params: { tagId: shelf.targetId }, query: {sort: 'modificationDate,desc'} }"
              @click="emit('close')"
            >
              {{ shelf.name }}
            </router-link>
            <span class="m3-list-item__supporting truncate">
              {{ t('labels.books_tagged_name', { tag: shelf.name }) }}
            </span>
          </div>
          <div
            v-if="props.allowDelete"
            class="m3-list-item__trailing"
          >
            <button
              v-tooltip="t('labels.delete')"
              type="button"
              class="m3-icon-btn relative z-[1]"
              :aria-label="`${t('labels.delete')} ${shelf.name}`"
              @click="deleteShelf(shelf)"
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
        v-else-if="!getPageIsLoading"
        icon="library-shelves"
        :title="t('labels.list_empty')"
      />
      <div
        v-if="shelves.length > 0 && pageCount > 1"
        :class="props.allowCreate ? 'px-4 pb-4' : 'pt-2'"
      >
        <o-pagination
          :current="pageAsNumber"
          :total="total"
          position="centered"
          :per-page="perPage"
          @change="updatePage"
        />
      </div>
    </section>
  </div>
</template>
