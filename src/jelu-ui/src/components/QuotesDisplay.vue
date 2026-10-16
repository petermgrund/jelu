<script setup lang="ts">
import { onMounted, Ref, ref } from 'vue'
import { Quote } from '../model/Quote'
import dataService from "../services/DataService"
import { Splide, SplideSlide } from '@splidejs/vue-splide';
import { useI18n } from 'vue-i18n'

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })
const quotes: Ref<Array<Quote>> = ref([]);

const getQuotes = async () => {
  try {
    let res = await dataService.quotes()
    if (res.length > 0) {
      quotes.value = res
    }
    else {
      quotes.value = await dataService.randomQuotes()
    }
  } catch (error) {
    console.log("failed get books : " + error);
  }
};

onMounted(() => {
  console.log("Component is mounted!");

});


try {
  getQuotes()
}
catch (err) {
  console.log("failed get quotes : " + err);
}

</script>

<template>
  <section
    v-if="quotes.length > 0"
    class="m3-section"
  >
    <h2 class="m3-section__title">
      {{ t('home.quotes') }}
    </h2>
    <div class="m3-card m3-card--filled px-6 py-8 sm:px-16">
      <Splide
        :options="{ rewind: true, autoplay: true, interval: 6000, arrows: quotes.length > 1, pagination: quotes.length > 1 }"
        :aria-label="t('home.quotes')"
      >
        <SplideSlide
          v-for="quote in quotes"
          :key="quote.content"
        >
          <figure class="flex flex-col items-center gap-4 pb-8 text-center">
            <i
              class="mdi mdi-format-quote-open text-[2.5rem] leading-none text-primary"
              aria-hidden="true"
            />
            <blockquote class="max-w-3xl text-title-large text-on-surface">
              {{ quote.content }}
            </blockquote>
            <figcaption
              v-if="quote.author || quote.origin"
              class="text-body-medium text-on-surface-variant"
            >
              <span v-if="quote.author">{{ quote.author }}</span>
              <template v-if="quote.origin">
                <span v-if="quote.author">, </span>
                <a
                  v-if="quote.link"
                  :href="quote.link"
                  target="_blank"
                  rel="noopener"
                  class="m3-link"
                >{{ quote.origin }}</a>
                <span v-else>{{ quote.origin }}</span>
              </template>
            </figcaption>
          </figure>
        </SplideSlide>
      </Splide>
    </div>
  </section>
</template>

<style lang="scss" scoped>
</style>
