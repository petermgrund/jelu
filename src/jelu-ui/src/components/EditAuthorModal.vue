<script setup lang="ts">
import { computed, Ref, ref } from "vue";
import { Author } from "../model/Author";
import { WikipediaSearchResult, WikipediaSearchResultElement } from "../model/WikipediaSearchResult";
import dataService from "../services/DataService";
import { StringUtils } from "../utils/StringUtils";
import { useI18n } from 'vue-i18n'
import { useLocalStorage } from '@vueuse/core'
import MDialog from "./m3/MDialog.vue";
import MField from "./m3/MField.vue";
import MSegmentedButton from "./m3/MSegmentedButton.vue";
import MTextField from "./m3/MTextField.vue";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const props = defineProps<{
  author : Author
}>()

const currentAuthor: Ref<Author> = ref(props.author)
console.log(currentAuthor.value)
const progress: Ref<boolean> = ref(false)
const deleteImage: Ref<boolean> = ref(false)
const uploadFromWeb = ref(true);
const imageUrl = ref<string | null>(null);
const file = ref<File | null>(null);
const uploadPercentage = ref(0);

const storedLanguage = useLocalStorage("jelu_language", "en")
const searchlanguage = ref(storedLanguage.value);
const FORM = "FORM"
const SEARCH = "SEARCH"
const currentPhase = ref(FORM)
const searchResult: Ref<WikipediaSearchResult> = ref({pages: []})

const emit = defineEmits<{
  (e: 'close'): void
}>()

const hasImage = computed(() => {
  return StringUtils.isNotBlank(currentAuthor.value.image)
})

function toggleRemoveImage() {
  deleteImage.value = !deleteImage.value
}

const clearImageField = () => {
  imageUrl.value = "";
};

const handleFileUpload = (event: any) => {
  file.value = event.target.files[0];
};

const update = () => {
  progress.value = true
  if (imageUrl.value != null) {
    currentAuthor.value.image = imageUrl.value
  }
  dataService.updateAuthor(currentAuthor.value, file.value, (event: { loaded: number; total: number }) => {
          const percent = Math.round((100 * event.loaded) / event.total);
          console.log("percent " + percent);
          uploadPercentage.value = percent;
        })
    .then(res => {
      progress.value = false
      emit('close')
    })
    .catch(e => {
      progress.value = false
    })
}

const search = () => {
  progress.value = true
  dataService.wikipediaSearch(currentAuthor.value.name, searchlanguage.value)
  .then(res => {
    console.log(res)
    progress.value = false
    searchResult.value = res
  })
  .catch(e => {
    progress.value = false
  })
}

const fillFormWithEntry = (entry: WikipediaSearchResultElement) => {
  progress.value = true
  dataService.wikipediaPage(entry.title, searchlanguage.value)
  .then(res => {
    currentAuthor.value.biography = res.extractHtml
    currentAuthor.value.wikipediaPage = res.contentUrls.desktop.page
    imageUrl.value = res.thumbnail?.source
    // we have a picture in search result but not in page result...
    // so use the search result picture as final picture
    if ((imageUrl.value === null || imageUrl.value === undefined) && entry.thumbnail !== null && entry.thumbnail !== undefined && entry.thumbnail.url !== null) {
      imageUrl.value = 'https:' + entry.thumbnail?.url
    }
    progress.value = false
    currentPhase.value = FORM
  })
  .catch(e => {
    progress.value = false
  })
}

const uploadOptions = computed(() => [
  { value: true, label: t('labels.upload_from_web') },
  { value: false, label: t('labels.upload_from_file') },
])

const imageUrlInvalid = computed(() => imageUrl.value != null && imageUrl.value.length > 0 && !/^https?:\/\//.test(imageUrl.value))
</script>

<template>
  <m-dialog
    :headline="currentPhase == FORM ? t('labels.edit_author') : t('labels.auto_fill')"
    size="lg"
    fullscreen
    @close="emit('close')"
  >
    <div
      v-if="currentPhase == FORM"
      class="flex flex-col gap-6"
    >
      <div class="flex flex-col gap-4">
        <m-text-field
          v-model="currentAuthor.name"
          :label="t('author.name')"
          leading-icon="account-outline"
          maxlength="1000"
          name="name"
        />
        <div class="grid gap-4 sm:grid-cols-2">
          <m-field :label="t('author.date_of_birth')">
            <o-datepicker
              v-model="currentAuthor.dateOfBirth"
              :show-week-number="false"
              :locale="undefined"
              :placeholder="t('labels.click_to_select')"
              expanded
              icon="calendar"
              :icon-right="currentAuthor.dateOfBirth ? 'close-circle-outline' : undefined"
              :icon-right-clickable="true"
              :mobile-native="false"
              :mobile-modal="false"
              trap-focus
              @icon-right-click="currentAuthor.dateOfBirth = undefined"
            />
          </m-field>
          <m-field :label="t('author.date_of_death')">
            <o-datepicker
              v-model="currentAuthor.dateOfDeath"
              :show-week-number="false"
              :locale="undefined"
              :placeholder="t('labels.click_to_select')"
              expanded
              icon="calendar"
              :icon-right="currentAuthor.dateOfDeath ? 'close-circle-outline' : undefined"
              :icon-right-clickable="true"
              :mobile-native="false"
              :mobile-modal="false"
              trap-focus
              @icon-right-click="currentAuthor.dateOfDeath = undefined"
            />
          </m-field>
        </div>
        <m-text-field
          v-model="currentAuthor.biography"
          :label="t('author.biography')"
          multiline
          :rows="6"
          maxlength="5000"
        />
        <m-text-field
          v-model="currentAuthor.notes"
          :label="t('author.personal_notes')"
          multiline
          :rows="3"
          maxlength="5000"
        />
      </div>

      <div class="grid gap-4 sm:grid-cols-2">
        <m-text-field
          v-model="currentAuthor.officialPage"
          type="url"
          :label="t('author.official_page')"
          leading-icon="web"
          maxlength="5000"
        />
        <m-text-field
          v-model="currentAuthor.wikipediaPage"
          type="url"
          :label="t('author.wikipedia_page')"
          leading-icon="wikipedia"
          maxlength="5000"
        />
        <m-text-field
          v-model="currentAuthor.goodreadsPage"
          type="url"
          :label="t('author.goodreads_page')"
          leading-icon="goodreads"
          maxlength="5000"
        />
        <m-text-field
          v-model="currentAuthor.twitterPage"
          type="url"
          :label="t('author.x_page')"
          leading-icon="alpha-x-box-outline"
          maxlength="5000"
        />
        <m-text-field
          v-model="currentAuthor.facebookPage"
          type="url"
          :label="t('author.facebook_page')"
          leading-icon="facebook"
          maxlength="5000"
        />
        <m-text-field
          v-model="currentAuthor.instagramPage"
          type="url"
          :label="t('author.instagram_page')"
          leading-icon="instagram"
          maxlength="5000"
        />
      </div>

      <div
        v-if="hasImage"
        class="flex items-center gap-4"
      >
        <img
          :src="'/files/' + props.author.image"
          :class="{ 'opacity-40 grayscale': deleteImage }"
          alt=""
          class="h-24 w-24 shrink-0 rounded-lg object-cover"
        >
        <div class="flex min-w-0 flex-1 flex-col gap-1">
          <span class="text-title-small text-on-surface">{{ t('labels.actual_cover') }}</span>
          <span class="text-body-small text-on-surface-variant">{{ deleteImage ? t('labels.refresh_to_restore') : t('labels.click_bin_to_remove') }}</span>
        </div>
        <button
          v-tooltip="deleteImage ? t('labels.refresh_to_restore') : t('labels.delete')"
          type="button"
          class="m3-icon-btn"
          :class="{ 'm3-icon-btn--error': !deleteImage }"
          :aria-label="deleteImage ? t('labels.refresh_to_restore') : t('labels.delete')"
          @click="toggleRemoveImage"
        >
          <i
            class="mdi"
            :class="deleteImage ? 'mdi-autorenew' : 'mdi-delete-outline'"
            aria-hidden="true"
          />
        </button>
      </div>

      <div
        v-if="!hasImage || deleteImage"
        class="flex flex-col gap-4"
      >
        <h3 class="text-title-small text-on-surface-variant">
          {{ t('labels.upload_image') }}
        </h3>
        <m-segmented-button
          v-model="uploadFromWeb"
          :options="uploadOptions"
          :aria-label="t('labels.upload_image')"
          class="max-w-lg"
          block
        />
        <m-text-field
          v-if="uploadFromWeb"
          v-model="imageUrl"
          type="url"
          :label="t('labels.enter_image_address')"
          leading-icon="link-variant"
          pattern="https?://.*"
          :supporting-text="t('labels.url_must_start')"
          :error="imageUrlInvalid ? t('labels.url_must_start') : false"
          clearable
          @clear="clearImageField"
        />
        <div
          v-else
          class="flex flex-col gap-3"
        >
          <div class="flex flex-wrap items-center gap-3">
            <label class="m3-btn m3-btn--outlined cursor-pointer focus-within:outline-3 focus-within:outline-secondary">
              <i
                class="mdi mdi-paperclip"
                aria-hidden="true"
              />
              <span>{{ t('labels.choose_file') }}</span>
              <input
                type="file"
                accept="image/*"
                class="sr-only"
                @change="handleFileUpload($event)"
              >
            </label>
            <span class="min-w-0 truncate text-body-medium text-on-surface-variant">
              {{ file == null ? t('labels.no_file_selected') : file.name }}
            </span>
          </div>
          <progress
            v-if="uploadPercentage > 0"
            max="100"
            :value.prop="uploadPercentage"
            class="m3-linear-progress"
          />
        </div>
      </div>

      <progress
        v-if="progress"
        class="m3-linear-progress"
      />
    </div>

    <div
      v-else-if="currentPhase == SEARCH"
      class="flex flex-col gap-4"
    >
      <m-text-field
        v-model="searchlanguage"
        :label="t('labels.short_language_code')"
        leading-icon="translate"
        maxlength="4"
        name="language"
        class="max-w-xs"
      />
      <ul
        v-if="searchResult.pages.length > 0"
        class="m3-list -mx-6"
      >
        <li
          v-for="res in searchResult.pages"
          :key="res.id"
        >
          <button
            v-tooltip="t('labels.click_to_import')"
            type="button"
            class="m3-list-item m3-list-item--two-line pl-6"
            @click="fillFormWithEntry(res)"
          >
            <span class="m3-list-item__leading">
              <img
                v-if="res.thumbnail?.url"
                :src="'https:' + res.thumbnail?.url"
                alt=""
                class="h-14 w-14 rounded-sm object-cover"
              >
              <span
                v-else
                class="grid h-14 w-14 place-items-center rounded-sm bg-surface-container-highest"
              >
                <i
                  class="mdi mdi-wikipedia"
                  aria-hidden="true"
                />
              </span>
            </span>
            <span class="m3-list-item__content">
              <span class="m3-list-item__headline">{{ res.title }}</span>
              <span
                v-if="res.description"
                class="m3-list-item__supporting"
              >{{ res.description }}</span>
            </span>
            <span class="m3-list-item__trailing">
              <i
                class="mdi mdi-import"
                aria-hidden="true"
              />
            </span>
          </button>
        </li>
      </ul>
      <p
        v-else
        class="flex items-center gap-3 rounded-md bg-tertiary-container p-4 text-body-medium text-on-tertiary-container"
        role="note"
      >
        <i
          class="mdi mdi-information-outline text-[1.25rem]"
          aria-hidden="true"
        />
        <span>{{ t('labels.author_import_help1') }} {{ t('labels.author_import_help2') }}</span>
      </p>
      <progress
        v-if="progress"
        class="m3-linear-progress"
      />
    </div>

    <template #actions>
      <template v-if="currentPhase == FORM">
        <button
          type="button"
          class="m3-btn m3-btn--text mr-auto"
          @click="currentPhase = SEARCH"
        >
          <i
            class="mdi mdi-auto-fix"
            aria-hidden="true"
          />
          <span>{{ t('labels.auto_fill') }}</span>
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
          @click="update"
        >
          <span
            v-if="progress"
            class="m3-spinner"
          />
          <span>{{ t('labels.save') }}</span>
        </button>
      </template>
      <template v-else>
        <button
          type="button"
          class="m3-btn m3-btn--text mr-auto"
          @click="currentPhase = FORM"
        >
          <i
            class="mdi mdi-arrow-left"
            aria-hidden="true"
          />
          <span>{{ t('labels.back') }}</span>
        </button>
        <button
          type="button"
          class="m3-btn m3-btn--filled"
          :disabled="progress"
          @click="search"
        >
          <span
            v-if="progress"
            class="m3-spinner"
          />
          <i
            v-else
            class="mdi mdi-magnify"
            aria-hidden="true"
          />
          <span>{{ t('labels.search') }}</span>
        </button>
      </template>
    </template>
  </m-dialog>
</template>
