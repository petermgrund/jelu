<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { computed, ref } from "vue"
import { useI18n } from 'vue-i18n'
import SeriesAdmin from "./SeriesAdmin.vue"
import TagsAdmin from "./TagsAdmin.vue"
import AdminAuthors from './AdminAuthors.vue'
import AuthorsAdmin from './AuthorsAdmin.vue'
import MTabs from './m3/MTabs.vue'
import { MTab } from './m3/types'

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | Data page')

const currentView = ref("TAG")

const tabs = computed<Array<MTab>>(() => [
  { label: t("book.tag", 2), value: 'TAG', icon: 'tag-outline' },
  { label: t("book.series", 2), value: 'SERIES', icon: 'bookshelf' },
  { label: t("book.author", 2), value: 'AUTHOR', icon: 'account-outline' },
  { label: t("authors_merge.merge_authors"), value: 'MERGE', icon: 'account-multiple-outline' },
])

</script>

<template>
  <div>
    <m-tabs
      v-model="currentView"
      variant="secondary"
      :tabs="tabs"
      :aria-label="t('nav.data-admin')"
      inline
      scrollable
    />
    <div class="mt-6">
      <TagsAdmin v-if="currentView == 'TAG'" />
      <SeriesAdmin v-if="currentView == 'SERIES'" />
      <AuthorsAdmin v-if="currentView == 'AUTHOR'" />
      <AdminAuthors v-if="currentView == 'MERGE'" />
    </div>
  </div>
</template>
