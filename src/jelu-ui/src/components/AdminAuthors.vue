<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next"
import { useTitle } from '@vueuse/core'
import { computed, Ref, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Author } from "../model/Author"
import dataService from "../services/DataService"
import { StringUtils } from "../utils/StringUtils"
import dayjs from "dayjs";
import useDates from '../composables/dates'
import { ObjectUtils } from "../utils/ObjectUtils";
import { useI18n } from 'vue-i18n'
import { Role } from "../model/Role"
import MAvatar from "./m3/MAvatar.vue"
import MField from "./m3/MField.vue"
import MTextField from "./m3/MTextField.vue"

useTitle('Jelu | Authors admin')

const { t, locale, availableLocales } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const router = useRouter()
const oruga = useOruga();

const { formatDate, formatDateString } = useDates()

const isFetching = ref(false)

const isMerging = ref(false)

const activeStep = ref("left")

const filteredAuthors: Ref<Array<Author>> = ref([]);

function getFilteredAuthors(text: string) {
  isFetching.value = true
  dataService.findAuthorByCriteria(Role.ANY, text).then((data) => filteredAuthors.value = data.content)
  isFetching.value = false
}

const leftAuthor: Ref<Author> = ref({ "name": "" })
const rightAuthor: Ref<Author> = ref({ "name": "" })
const leftHasImage = computed(() => {
  return StringUtils.isNotBlank(leftAuthor.value.image)
})
const rightHasImage = computed(() => {
  return StringUtils.isNotBlank(rightAuthor.value.image)
})
const leftDeleteImage: Ref<boolean> = ref(false)


function dispatchAuthor(author: Author) {
  console.log(author)
  console.log(`step ${activeStep.value}`)
  // we receive from oruga weird events while nothing is selected
  // so try to get rid of those null data we receive
  if (author != null) {
    if (author.dateOfBirth != null) {
      author.dateOfBirth = dayjs(author.dateOfBirth).toDate()
    }
    if (author.dateOfDeath != null) {
      author.dateOfDeath = dayjs(author.dateOfDeath).toDate()
    }
    if (activeStep.value === "left") {
      leftAuthor.value = author
      activeStep.value = "right"
    }
    else if (activeStep.value === "right") {
      rightAuthor.value = author
      activeStep.value = "merge"
    }
  }
}

// one picker per author: dispatch the selection to its side, then go to the first step still missing an author
function selectAuthor(step: "left" | "right", author: Author | undefined) {
  if (author == null) {
    return
  }
  activeStep.value = step
  dispatchAuthor(author)
  if (leftAuthor.value.id == null) {
    activeStep.value = "left"
  } else if (rightAuthor.value.id == null) {
    activeStep.value = "right"
  } else {
    activeStep.value = "merge"
  }
}

function save() {
  if (leftAuthor.value.id != null && rightAuthor.value.id != null) {
    isMerging.value = true
    dataService.mergeAuthors(leftAuthor.value.id, rightAuthor.value.id, leftAuthor.value)
      .then(res => {
        console.log("merged author " + res)
        isMerging.value = false
        router.push({ name: 'author-detail', params: { authorId: leftAuthor.value.id } })
      })
      .catch(err => {
        console.log("merge failure " + err)
        isMerging.value = false
        ObjectUtils.toast(oruga, "danger", `Error ` + err.message, 4000)
      })
  }
  else {
    console.log("missing ids on authors")
  }
}

const rightDoB = computed(() => {
  if (rightAuthor.value.dateOfBirth != null) {
    return formatDate(rightAuthor.value.dateOfBirth)
  }
  return ""
})

const rightDoD = computed(() => {
  if (rightAuthor.value.dateOfDeath != null) {
    return formatDate(rightAuthor.value.dateOfDeath)
  }
  return ""
})

const options = computed(() => {
  return filteredAuthors.value.map(t => ObjectUtils.wrapForOptions(t))
})

type MergeableField = 'biography' | 'officialPage' | 'wikipediaPage' | 'goodreadsPage' | 'twitterPage' | 'facebookPage' | 'instagramPage' | 'notes'

// text fields compared side by side in the merge step
const mergeFields = computed<Array<{ key: MergeableField, label: string, type?: string, multiline?: boolean }>>(() => [
  { key: 'biography', label: t('author.biography'), multiline: true },
  { key: 'officialPage', label: t('author.official_page'), type: 'url' },
  { key: 'wikipediaPage', label: t('author.wikipedia_page'), type: 'url' },
  { key: 'goodreadsPage', label: t('author.goodreads_page'), type: 'url' },
  { key: 'twitterPage', label: t('author.x_page'), type: 'url' },
  { key: 'facebookPage', label: t('author.facebook_page'), type: 'url' },
  { key: 'instagramPage', label: t('author.instagram_page'), type: 'url' },
  { key: 'notes', label: t('author.personal_notes'), multiline: true },
])
</script>

<template>
  <div class="flex flex-col gap-6">
    <div class="grid items-start gap-6 md:grid-cols-2">
      <section class="m3-card m3-card--outlined overflow-visible">
        <div class="flex flex-col gap-4 p-6">
          <header class="flex items-start gap-4">
            <span
              class="grid h-8 w-8 shrink-0 place-items-center rounded-full text-label-large"
              :class="leftAuthor.id != null ? 'bg-primary text-on-primary' : 'bg-secondary-container text-on-secondary-container'"
            >
              <i
                v-if="leftAuthor.id != null"
                class="mdi mdi-check text-[1.125rem]"
                aria-hidden="true"
              />
              <template v-else>1</template>
            </span>
            <div class="flex flex-1 flex-col gap-1">
              <h2 class="text-title-large">
                {{ t('authors_merge.author_left_subtitle') }}
              </h2>
              <p class="text-body-medium text-on-surface-variant">
                {{ t('authors_merge.author_left_description') }}
              </p>
            </div>
          </header>
          <m-field
            :label="t('authors_merge.author_nb', { nb: 1 })"
            leading-icon="magnify"
          >
            <o-autocomplete
              :options="options"
              expanded
              clear-on-select
              :loading="isFetching"
              :debounce="100"
              :placeholder="t('authors_merge.search_message')"
              :aria-label="t('authors_merge.author_nb', { nb: 1 })"
              @input="getFilteredAuthors"
              @select="selectAuthor('left', $event)"
            >
              <template #default="{ value }">
                <span>{{ value.name }}</span>
              </template>
            </o-autocomplete>
          </m-field>
          <div
            v-if="leftAuthor.id != null"
            class="flex items-center gap-3 rounded-md bg-surface-container p-3"
          >
            <img
              v-if="leftHasImage"
              :src="'/files/' + leftAuthor.image"
              alt=""
              class="h-10 w-10 shrink-0 rounded-full object-cover"
            >
            <m-avatar
              v-else
              :name="leftAuthor.name"
            />
            <span class="min-w-0 flex-1 text-title-medium text-on-surface">{{ leftAuthor.name }}</span>
          </div>
        </div>
      </section>

      <section class="m3-card m3-card--outlined overflow-visible">
        <div class="flex flex-col gap-4 p-6">
          <header class="flex items-start gap-4">
            <span
              class="grid h-8 w-8 shrink-0 place-items-center rounded-full text-label-large"
              :class="rightAuthor.id != null ? 'bg-primary text-on-primary' : 'bg-secondary-container text-on-secondary-container'"
            >
              <i
                v-if="rightAuthor.id != null"
                class="mdi mdi-check text-[1.125rem]"
                aria-hidden="true"
              />
              <template v-else>2</template>
            </span>
            <div class="flex flex-1 flex-col gap-1">
              <h2 class="text-title-large">
                {{ t('authors_merge.author_right_subtitle') }}
              </h2>
              <p class="text-body-medium text-on-surface-variant">
                {{ t('authors_merge.author_right_description') }}
              </p>
            </div>
          </header>
          <m-field
            :label="t('authors_merge.author_nb', { nb: 2 })"
            leading-icon="magnify"
          >
            <o-autocomplete
              :options="options"
              expanded
              clear-on-select
              :loading="isFetching"
              :debounce="100"
              :placeholder="t('authors_merge.search_message')"
              :aria-label="t('authors_merge.author_nb', { nb: 2 })"
              @input="getFilteredAuthors"
              @select="selectAuthor('right', $event)"
            >
              <template #default="{ value }">
                <span>{{ value.name }}</span>
              </template>
            </o-autocomplete>
          </m-field>
          <div
            v-if="rightAuthor.id != null"
            class="flex items-center gap-3 rounded-md bg-surface-container p-3"
          >
            <img
              v-if="rightHasImage"
              :src="'/files/' + rightAuthor.image"
              alt=""
              class="h-10 w-10 shrink-0 rounded-full object-cover"
            >
            <m-avatar
              v-else
              :name="rightAuthor.name"
            />
            <span class="min-w-0 flex-1 text-title-medium text-on-surface">{{ rightAuthor.name }}</span>
          </div>
        </div>
      </section>
    </div>

    <section class="m3-card m3-card--outlined overflow-visible">
      <div class="flex flex-col gap-6 p-6">
        <header class="flex items-start gap-4">
          <span
            class="grid h-8 w-8 shrink-0 place-items-center rounded-full text-label-large"
            :class="activeStep === 'merge' ? 'bg-primary text-on-primary' : 'bg-secondary-container text-on-secondary-container'"
          >3</span>
          <div class="flex flex-1 flex-col gap-1">
            <h2 class="text-title-large">
              {{ t('authors_merge.authors_merge_subtitle') }}
            </h2>
            <p class="text-body-medium text-on-surface-variant">
              {{ t('authors_merge.authors_merge_description') }}
            </p>
          </div>
        </header>

        <div
          v-if="activeStep === 'merge'"
          class="flex flex-col gap-4"
        >
          <div class="hidden grid-cols-[minmax(0,1fr)_2.5rem_minmax(0,1fr)] gap-4 md:grid">
            <span class="flex items-center gap-2 text-title-small text-on-surface-variant">
              <i
                class="mdi mdi-numeric-1-circle-outline text-[1.25rem]"
                aria-hidden="true"
              />{{ t('authors_merge.author_left_subtitle') }}
            </span>
            <span />
            <span class="flex items-center gap-2 text-title-small text-on-surface-variant">
              <i
                class="mdi mdi-numeric-2-circle-outline text-[1.25rem]"
                aria-hidden="true"
              />{{ t('authors_merge.author_right_subtitle') }}
            </span>
          </div>

          <div class="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-2 md:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] md:gap-4">
            <m-text-field
              v-model="leftAuthor.name"
              :label="t('author.name')"
              class="col-span-2 md:col-span-1"
            >
              <template #leading>
                <i
                  class="m3-field__icon mdi mdi-numeric-1-circle-outline md:hidden"
                  aria-hidden="true"
                />
              </template>
            </m-text-field>
            <button
              v-tooltip="t('authors_merge.use_value')"
              type="button"
              class="m3-icon-btn order-last md:order-none"
              :class="{ invisible: !rightAuthor.name }"
              :aria-label="`${t('authors_merge.use_value')}: ${t('author.name')}`"
              @click="leftAuthor.name = rightAuthor.name"
            >
              <i
                class="mdi mdi-arrow-up md:hidden"
                aria-hidden="true"
              />
              <i
                class="mdi mdi-arrow-left hidden md:inline-block"
                aria-hidden="true"
              />
            </button>
            <m-text-field
              :model-value="rightAuthor.name"
              :label="t('author.name')"
              readonly
            >
              <template #leading>
                <i
                  class="m3-field__icon mdi mdi-numeric-2-circle-outline md:hidden"
                  aria-hidden="true"
                />
              </template>
            </m-text-field>
          </div>

          <div class="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-2 md:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] md:gap-4">
            <m-field
              :label="t('author.date_of_birth')"
              class="col-span-2 md:col-span-1"
            >
              <o-datepicker
                v-model="leftAuthor.dateOfBirth"
                :show-week-number="false"
                :locale="undefined"
                :placeholder="t('labels.click_to_select')"
                :expanded="true"
                icon="calendar"
                :icon-right="leftAuthor.dateOfBirth ? 'close-circle-outline' : undefined"
                :icon-right-clickable="true"
                :mobile-native="false"
                mobile-modal="false"
                trap-focus
                @icon-right-click="leftAuthor.dateOfBirth = undefined"
              />
            </m-field>
            <button
              v-tooltip="t('authors_merge.use_value')"
              type="button"
              class="m3-icon-btn order-last md:order-none"
              :class="{ invisible: !rightAuthor.dateOfBirth }"
              :aria-label="`${t('authors_merge.use_value')}: ${t('author.date_of_birth')}`"
              @click="leftAuthor.dateOfBirth = rightAuthor.dateOfBirth"
            >
              <i
                class="mdi mdi-arrow-up md:hidden"
                aria-hidden="true"
              />
              <i
                class="mdi mdi-arrow-left hidden md:inline-block"
                aria-hidden="true"
              />
            </button>
            <m-text-field
              :model-value="rightDoB"
              :label="t('author.date_of_birth')"
              readonly
            >
              <template #leading>
                <i
                  class="m3-field__icon mdi mdi-numeric-2-circle-outline md:hidden"
                  aria-hidden="true"
                />
              </template>
            </m-text-field>
          </div>

          <div class="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-2 md:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] md:gap-4">
            <m-field
              :label="t('author.date_of_death')"
              class="col-span-2 md:col-span-1"
            >
              <o-datepicker
                v-model="leftAuthor.dateOfDeath"
                :show-week-number="false"
                :locale="undefined"
                :placeholder="t('labels.click_to_select')"
                :expanded="true"
                icon="calendar"
                :icon-right="leftAuthor.dateOfDeath ? 'close-circle-outline' : undefined"
                :icon-right-clickable="true"
                mobile-native="false"
                mobile-modal="false"
                trap-focus
                @icon-right-click="leftAuthor.dateOfDeath = undefined"
              />
            </m-field>
            <button
              v-tooltip="t('authors_merge.use_value')"
              type="button"
              class="m3-icon-btn order-last md:order-none"
              :class="{ invisible: !rightAuthor.dateOfDeath }"
              :aria-label="`${t('authors_merge.use_value')}: ${t('author.date_of_death')}`"
              @click="leftAuthor.dateOfDeath = rightAuthor.dateOfDeath"
            >
              <i
                class="mdi mdi-arrow-up md:hidden"
                aria-hidden="true"
              />
              <i
                class="mdi mdi-arrow-left hidden md:inline-block"
                aria-hidden="true"
              />
            </button>
            <m-text-field
              :model-value="rightDoD"
              :label="t('author.date_of_death')"
              readonly
            >
              <template #leading>
                <i
                  class="m3-field__icon mdi mdi-numeric-2-circle-outline md:hidden"
                  aria-hidden="true"
                />
              </template>
            </m-text-field>
          </div>

          <div
            v-for="field in mergeFields"
            :key="field.key"
            class="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-2 md:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] md:gap-4"
          >
            <m-text-field
              v-model="leftAuthor[field.key]"
              :label="field.label"
              :type="field.type"
              :multiline="field.multiline"
              :rows="3"
              maxlength="5000"
              class="col-span-2 md:col-span-1"
            >
              <template #leading>
                <i
                  class="m3-field__icon mdi mdi-numeric-1-circle-outline md:hidden"
                  aria-hidden="true"
                />
              </template>
            </m-text-field>
            <button
              v-tooltip="t('authors_merge.use_value')"
              type="button"
              class="m3-icon-btn order-last md:order-none"
              :class="{ invisible: !rightAuthor[field.key] }"
              :aria-label="`${t('authors_merge.use_value')}: ${field.label}`"
              @click="leftAuthor[field.key] = rightAuthor[field.key]"
            >
              <i
                class="mdi mdi-arrow-up md:hidden"
                aria-hidden="true"
              />
              <i
                class="mdi mdi-arrow-left hidden md:inline-block"
                aria-hidden="true"
              />
            </button>
            <m-text-field
              :model-value="rightAuthor[field.key]"
              :label="field.label"
              :type="field.type"
              :multiline="field.multiline"
              :rows="3"
              readonly
            >
              <template #leading>
                <i
                  class="m3-field__icon mdi mdi-numeric-2-circle-outline md:hidden"
                  aria-hidden="true"
                />
              </template>
            </m-text-field>
          </div>

          <div
            v-if="leftHasImage || rightHasImage"
            class="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-2 md:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] md:gap-4"
          >
            <div class="col-span-2 flex items-center gap-2 md:col-span-1">
              <img
                v-if="leftHasImage"
                :src="'/files/' + leftAuthor.image"
                :class="{ 'opacity-40 grayscale': leftDeleteImage }"
                alt=""
                class="h-24 w-24 rounded-lg object-cover"
              >
              <span
                v-else
                class="grid h-24 w-24 place-items-center rounded-lg bg-surface-container-highest text-on-surface-variant"
              >
                <i
                  class="mdi mdi-image-off-outline text-[2rem]"
                  aria-hidden="true"
                />
              </span>
              <button
                v-if="leftHasImage"
                v-tooltip="t('labels.delete')"
                type="button"
                class="m3-icon-btn"
                :aria-label="t('labels.delete')"
                @click="leftAuthor.image = undefined"
              >
                <i
                  class="mdi mdi-delete-outline"
                  aria-hidden="true"
                />
              </button>
            </div>
            <button
              v-tooltip="t('authors_merge.use_value')"
              type="button"
              class="m3-icon-btn order-last md:order-none"
              :class="{ invisible: !rightHasImage }"
              :aria-label="t('authors_merge.use_value')"
              @click="leftAuthor.image = rightAuthor.image"
            >
              <i
                class="mdi mdi-arrow-up md:hidden"
                aria-hidden="true"
              />
              <i
                class="mdi mdi-arrow-left hidden md:inline-block"
                aria-hidden="true"
              />
            </button>
            <div class="flex items-center">
              <img
                v-if="rightHasImage"
                :src="'/files/' + rightAuthor.image"
                :class="{ 'opacity-40 grayscale': leftDeleteImage }"
                alt=""
                class="h-24 w-24 rounded-lg object-cover"
              >
              <span
                v-else
                class="grid h-24 w-24 place-items-center rounded-lg bg-surface-container-highest text-on-surface-variant"
              >
                <i
                  class="mdi mdi-image-off-outline text-[2rem]"
                  aria-hidden="true"
                />
              </span>
            </div>
          </div>
        </div>

        <div class="flex justify-end">
          <button
            type="button"
            class="m3-btn m3-btn--filled"
            :disabled="activeStep !== 'merge'"
            @click="save"
          >
            <i
              class="mdi mdi-call-merge"
              aria-hidden="true"
            />
            <span>{{ t('author.merge_changes') }}</span>
          </button>
        </div>
      </div>
    </section>
  </div>
  <o-loading
    v-model:active="isMerging"
    :full-page="true"
    :cancelable="true"
  />
</template>
