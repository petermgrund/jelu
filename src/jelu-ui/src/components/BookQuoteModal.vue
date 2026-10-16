<script setup lang="ts">
import { computed, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import { Book } from "../model/Book";
import { Visibility } from "../model/Review";
import { BookQuote } from "../model/BookQuote"
import dataService from "../services/DataService";
import MDialog from "./m3/MDialog.vue";
import MSegmentedButton from "./m3/MSegmentedButton.vue";
import MTextField from "./m3/MTextField.vue";

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const props = defineProps<{
  book: Book,
  edit: boolean,
  bookQuote: BookQuote | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const progress: Ref<boolean> = ref(false)

const visibility: Ref<Visibility> = ref(props.edit != null && props.edit === true && props.bookQuote?.visibility != null ? props.bookQuote?.visibility : Visibility.PRIVATE)
const quoteText = ref(props.edit != null && props.edit === true && props.bookQuote?.text != null ? props.bookQuote?.text : "")
const position = ref(props.edit != null && props.edit === true && props.bookQuote?.position != null ? props.bookQuote?.position : "")

watch(visibility, (newVal, oldVal) => {
  console.log("visibilty " + visibility.value)
})

const submit = () => {
  console.log("submit ")
  if (props.book.id != null) {
    progress.value = true
    dataService.saveBookQuote({
      bookId: props.book.id,
      text: quoteText.value,
      visibility: visibility.value,
      position: position.value
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

const editBookQuote = () => {
  console.log("edit ")
  if (props.book.id != null && props.bookQuote?.id != null) {
    progress.value = true
    dataService.updateBookQuote(props.bookQuote.id, {
      text: quoteText.value,
      visibility: visibility.value,
      position: position.value
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

const visibilityOptions = computed(() => [
  { value: Visibility.PRIVATE, label: t('reviews.private'), icon: 'lock-outline' },
  { value: Visibility.PUBLIC, label: t('reviews.public'), icon: 'earth' },
])
</script>

<template>
  <m-dialog
    :headline="props.edit === true ? t('book_quotes.edit_quote') : t('book_quotes.create')"
    size="lg"
    fullscreen
    @close="emit('close')"
  >
    <div class="flex flex-col gap-6">
      <div class="flex flex-col gap-3">
        <h3 class="text-title-small text-on-surface-variant">
          {{ t('book_quotes.quote', 1) }}
        </h3>
        <v-md-editor
          v-model="quoteText"
          :disabled-menus="['image/upload-image', 'toc', 'save']"
          height="20rem"
        />
      </div>
      <m-text-field
        v-model="position"
        :label="t('book_quotes.position')"
        class="max-w-md"
      />
      <div class="flex flex-col gap-3">
        <h3 class="text-title-small text-on-surface-variant">
          {{ t('reviews.visibility') }}
        </h3>
        <m-segmented-button
          v-model="visibility"
          :options="visibilityOptions"
          :aria-label="t('reviews.visibility')"
          class="max-w-md"
          block
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
        <span>{{ t('labels.save') }}</span>
      </button>
      <button
        v-else
        type="button"
        class="m3-btn m3-btn--filled"
        :disabled="progress"
        @click="editBookQuote"
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
