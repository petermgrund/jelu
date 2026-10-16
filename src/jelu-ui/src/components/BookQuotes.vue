<script setup lang="ts">
import { useTitle } from '@vueuse/core';
import { Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import { Book } from "../model/Book";
import { BookQuote } from "../model/BookQuote";
import dataService from "../services/DataService";
import BookDataCard from "./BookDataCard.vue";
import BookQuoteCard from "./BookQuoteCard.vue";
import MEmptyState from "./m3/MEmptyState.vue";

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const route = useRoute()

useTitle('Jelu | Quotes')
const quotes: Ref<Array<BookQuote>> = ref([])
const book: Ref<Book> = ref({title:""})
// the empty state is only shown once the quotes request is over
const quotesLoaded = ref(false)

watch(() => route.params.bookId, (newVal, oldVal) => {
  console.log(newVal + " " + oldVal)
  if (newVal !== oldVal && route.params.bookId !== undefined) {
    getBook()
    getBookQuotes()
  }
})

const getBook = async () => {
  try {
    book.value = await dataService.findBookById(route.params.bookId as string)
  } catch (error) {
    console.log("failed get book : " + error);
  }
};

const getBookQuotes = async () => {
  dataService.findBookQuotes(undefined,
    route.params.bookId as string, null, 0, 50, null)
    .then(res => {
        if (!res.empty) {
            quotes.value = res.content
        }
    })
    .catch(err => console.log(err))
    .finally(() => {
      quotesLoaded.value = true
    })
};

getBook()
getBookQuotes()

</script>

<template>
  <div class="m3-page m3-page--medium">
    <book-data-card
      :book="book"
      :owned="null"
      :to-read="null"
      :borrowed="null"
      :book-link="true"
      :links="true"
      :add-book="true"
    />
    <section class="m3-section">
      <h2 class="m3-section__title">
        {{ t('book_quotes.quote', 2) }}
      </h2>
      <div
        v-if="quotes.length > 0"
        class="grid gap-4 lg:grid-cols-2"
      >
        <book-quote-card
          v-for="quote in quotes"
          :key="quote.id"
          :book-quote="quote"
          :show-delete="false"
          :show-edit="false"
        />
      </div>
      <m-empty-state
        v-else-if="quotesLoaded"
        icon="format-quote-open-outline"
        :title="t('book_quotes.no_quotes')"
      />
    </section>
  </div>
</template>
