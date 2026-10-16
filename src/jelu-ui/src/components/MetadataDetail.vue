<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import useDates from '../composables/dates';
import { Metadata } from "../model/Metadata";
import BookCover from './BookCover.vue';

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })
const { formatDateString } = useDates()

defineProps<{
  metadata: Metadata,
}>()

</script>

<template>
  <section class="grid gap-6 sm:grid-cols-[8rem_minmax(0,1fr)]">
    <div class="mx-auto w-32 sm:w-full">
      <book-cover
        :image="metadata.image"
        :title="metadata.title"
        rounded
      />
    </div>
    <div class="flex min-w-0 flex-col gap-4">
      <h3
        v-if="metadata.title"
        class="text-title-large text-on-surface"
      >
        {{ metadata.title }}
      </h3>
      <dl class="m3-meta items-baseline">
        <template v-if="metadata.authors != null && metadata.authors.length > 0">
          <dt>{{ t('book.author', metadata.authors.length) }}</dt>
          <dd class="m3-chip-set">
            <span
              v-for="author in metadata.authors"
              :key="author"
              class="m3-chip m3-chip--assist m3-chip--static"
            >
              <i
                class="mdi mdi-account-outline"
                aria-hidden="true"
              />
              <span>{{ author }}</span>
            </span>
          </dd>
        </template>
        <template v-if="metadata.publisher">
          <dt>{{ t('book.publisher') }}</dt>
          <dd>{{ metadata.publisher }}</dd>
        </template>
        <template v-if="metadata.publishedDate">
          <dt>{{ t('book.published_date') }}</dt>
          <dd>{{ formatDateString(metadata.publishedDate) }}</dd>
        </template>
        <template v-if="metadata.pageCount">
          <dt>{{ t('book.page', 2) }}</dt>
          <dd>{{ metadata.pageCount }}</dd>
        </template>
        <template v-if="metadata.series">
          <dt>{{ t('book.series') }}</dt>
          <dd>
            {{ metadata.series }}<span v-if="metadata.numberInSeries">&nbsp;#{{ metadata.numberInSeries }}</span>
          </dd>
        </template>
        <template v-else-if="metadata.numberInSeries">
          <dt>{{ t('book.nb_in_series') }}</dt>
          <dd>{{ metadata.numberInSeries }}</dd>
        </template>
        <template v-if="metadata.language">
          <dt>{{ t('book.language') }}</dt>
          <dd>{{ metadata.language }}</dd>
        </template>
        <template v-if="metadata.isbn10">
          <dt>{{ t('book.isbn10') }}</dt>
          <dd class="tabular-nums">
            {{ metadata.isbn10 }}
          </dd>
        </template>
        <template v-if="metadata.isbn13">
          <dt>{{ t('book.isbn13') }}</dt>
          <dd class="tabular-nums">
            {{ metadata.isbn13 }}
          </dd>
        </template>
        <template v-if="metadata.goodreadsId">
          <dt>{{ t('book.goodreads_id') }}</dt>
          <dd>{{ metadata.goodreadsId }}</dd>
        </template>
        <template v-if="metadata.googleId">
          <dt>{{ t('book.google_id') }}</dt>
          <dd>{{ metadata.googleId }}</dd>
        </template>
        <template v-if="metadata.amazonId">
          <dt>{{ t('book.amazon_id') }}</dt>
          <dd>{{ metadata.amazonId }}</dd>
        </template>
        <template v-if="metadata.isfdbId">
          <dt>{{ t('book.isfdb_id') }}</dt>
          <dd>{{ metadata.isfdbId }}</dd>
        </template>
        <template v-if="metadata.openlibraryId">
          <dt>{{ t('book.openlibrary_id') }}</dt>
          <dd>{{ metadata.openlibraryId }}</dd>
        </template>
        <template v-if="metadata.noosfereId">
          <dt>{{ t('book.noosfere_id') }}</dt>
          <dd>{{ metadata.noosfereId }}</dd>
        </template>
        <template v-if="metadata.inventaireId">
          <dt>{{ t('book.inventaire_id') }}</dt>
          <dd>{{ metadata.inventaireId }}</dd>
        </template>
        <template v-if="metadata.tags != null && metadata.tags.length > 0">
          <dt>{{ t('book.tag', metadata.tags.length) }}</dt>
          <dd class="m3-chip-set">
            <span
              v-for="tag in metadata.tags"
              :key="tag"
              class="m3-chip m3-chip--assist m3-chip--static"
            >
              <i
                class="mdi mdi-tag-outline"
                aria-hidden="true"
              />
              <span>{{ tag }}</span>
            </span>
          </dd>
        </template>
        <template v-if="metadata.sourcePlugin">
          <dt>{{ t('metadata.source_plugin') }}</dt>
          <dd>{{ metadata.sourcePlugin }}</dd>
        </template>
      </dl>
    </div>
    <div
      v-if="metadata.summary"
      class="flex flex-col gap-2 sm:col-span-2"
    >
      <h4 class="text-title-small text-on-surface">
        {{ t('book.summary') }}
      </h4>
      <p class="whitespace-pre-line text-body-medium text-on-surface">
        {{ metadata.summary }}
      </p>
    </div>
  </section>
</template>
