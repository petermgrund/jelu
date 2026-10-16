<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { useRouteQuery } from '@vueuse/router'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import ProfileDetail from "./ProfileDetail.vue"
import UserShelves from "./UserShelves.vue"
import CustomLists from './CustomLists.vue'
import MTabs from './m3/MTabs.vue'
import { MTab } from './m3/types'

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | User page')

// kept in the url so the shelves dialog can link to the shelves tab
const currentView = useRouteQuery<string>('view', 'PROFILE')

const tabs = computed<Array<MTab>>(() => [
  { label: t('settings.profile'), value: 'PROFILE', icon: 'account-outline' },
  { label: t('settings.shelves'), value: 'SHELVES', icon: 'tag-multiple-outline' },
  { label: t('settings.custom_lists'), value: 'LISTS', icon: 'format-list-bulleted' },
])
</script>

<template>
  <div>
    <m-tabs
      v-model="currentView"
      variant="secondary"
      :tabs="tabs"
      :aria-label="t('settings.profile')"
      inline
      scrollable
    />
    <div class="mt-6">
      <ProfileDetail v-if="currentView == 'PROFILE'" />
      <UserShelves v-if="currentView == 'SHELVES'" />
      <CustomLists v-if="currentView == 'LISTS'" />
    </div>
  </div>
</template>
