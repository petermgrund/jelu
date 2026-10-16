<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { computed } from 'vue'
import { useStore } from 'vuex'
import { key } from '../store'
import { useI18n } from 'vue-i18n'
import { Provider } from '../model/User'
import MAvatar from './m3/MAvatar.vue'
import MTabs from './m3/MTabs.vue'
import { MTab } from './m3/types'

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

useTitle('Jelu | User page')

const store = useStore(key)

const username = computed(() => store.getters.getUsername)

const tabs = computed<Array<MTab>>(() => {
  const items: Array<MTab> = [
    { label: t('settings.profile'), icon: 'account-outline', to: '/profile/me' },
    { label: t('settings.settings'), icon: 'cog-outline', to: '/profile/settings' },
    { label: t('nav.data-admin'), icon: 'database-outline', to: '/profile/data' },
    { label: t('settings.imports'), icon: 'swap-vertical', to: '/profile/imports' },
    { label: t('settings.messages'), icon: 'message-text-outline', to: '/profile/messages' },
    { label: t('settings.stats'), icon: 'chart-box-outline', to: '/profile/stats' },
    { label: t('settings.users'), icon: 'account-multiple-outline', to: '/profile/users' },
    { label: t('settings.api_tokens'), icon: 'key-outline', to: '/profile/api-tokens' },
  ]
  if (store.getters.isAdmin && store.getters.getUser != null && store.getters.getUser.provider !== Provider.PROXY) {
    items.push({ label: t('settings.add_users'), icon: 'account-plus-outline', to: '/profile/admin/users' })
  }
  return items
})
</script>

<template>
  <div class="m3-page">
    <header class="flex items-center gap-4 pt-2 pb-4">
      <m-avatar
        :name="username"
        size="lg"
      />
      <div class="min-w-0">
        <h1 class="truncate text-headline-small text-on-surface">
          {{ username }}
        </h1>
        <p class="text-body-medium text-on-surface-variant">
          {{ t('settings.my_profile') }}
        </p>
      </div>
    </header>
    <m-tabs
      :tabs="tabs"
      scrollable
      :aria-label="t('settings.settings')"
    />
    <div class="pt-6">
      <router-view />
    </div>
    <footer class="mt-12 flex justify-center">
      <a
        href="https://github.com/bayang/jelu"
        target="_blank"
        rel="noopener"
        class="m3-btn m3-btn--text"
      >
        <i
          class="mdi mdi-github"
          aria-hidden="true"
        />
        <span>Jelu {{ store.getters.getAppVersion }}</span>
      </a>
    </footer>
  </div>
</template>
