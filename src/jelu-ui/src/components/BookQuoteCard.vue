<script setup lang="ts">
import { ref } from "vue";
import { useI18n } from 'vue-i18n';
import { BookQuote } from "../model/BookQuote";
import dataService from "../services/DataService";
import MAvatar from "./m3/MAvatar.vue";

const { t, d } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const props = defineProps<{ 
  bookQuote: BookQuote,
  showDelete: boolean,
  showEdit: boolean,
}>();

const username = ref("")

const getUsername = async () => {
  username.value = await dataService.usernameById(props.bookQuote.user)
}

getUsername()

const emit = defineEmits<{
  (e: 'update:delete', bookQuoteId: string): void,
  (e: 'update:edit', bookQuoteId: string): void,
}>()

const deleteBookQuote = async (bookQuoteId: string) => {
  emit("update:delete", bookQuoteId)
}

const editBookQuote = async (bookQuoteId: string) => {
  emit("update:edit", bookQuoteId)
}

</script>

<template>
  <article class="m3-card m3-card--outlined h-full">
    <div class="m3-card__content gap-3">
      <i
        class="mdi mdi-format-quote-open text-[2rem] leading-none text-primary"
        aria-hidden="true"
      />
      <v-md-preview
        class="text-body-large italic"
        :text="props.bookQuote.text"
      />
      <footer class="mt-auto flex items-center gap-3 pt-2">
        <m-avatar
          :name="username"
          size="sm"
        />
        <div class="min-w-0 flex-1 text-body-small text-on-surface-variant">
          <span class="text-on-surface">{{ username }}</span>
          <span> · {{ d(props.bookQuote.creationDate, 'short') }}</span>
          <span v-if="bookQuote.position"> · {{ bookQuote.position }}</span>
        </div>
        <button
          v-if="props.showEdit"
          v-tooltip="t('book_quotes.edit_quote')"
          type="button"
          class="m3-icon-btn"
          :aria-label="t('book_quotes.edit_quote')"
          @click="editBookQuote(props.bookQuote.id)"
        >
          <i
            class="mdi mdi-pencil-outline"
            aria-hidden="true"
          />
        </button>
        <button
          v-if="props.showDelete"
          v-tooltip="t('labels.delete')"
          type="button"
          class="m3-icon-btn"
          :aria-label="t('labels.delete')"
          @click="deleteBookQuote(props.bookQuote.id)"
        >
          <i
            class="mdi mdi-delete-outline"
            aria-hidden="true"
          />
        </button>
      </footer>
    </div>
  </article>
</template>
