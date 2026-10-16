<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next"
import { useTitle } from '@vueuse/core'
import dayjs from 'dayjs'
import { computed, ref, Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import { LoginHistoryInfo, Provider } from "../model/User"
import dataService from "../services/DataService"
import { key } from '../store'
import UserModalVue from './UserModal.vue'
import MAvatar from './m3/MAvatar.vue'

const oruga = useOruga()

const { t, d } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | User page')

const store = useStore(key)

const showModal: Ref<boolean> = ref(false)

const user = computed(() => {
  return store.getters.getUser
})

let loginHistoryInfo: Ref<Array<LoginHistoryInfo>> = ref([])

function toggleUserModal() {
  showModal.value = !showModal.value
  oruga.modal.open({
    component: UserModalVue,
    trapFocus: true,
    active: true,
    canCancel: ['x', 'button', 'outside'],
    scroll: 'keep',
    props: {
      "currentUser": user.value
    },
    onClose: modalClosed
  });
}

function modalClosed() {
  console.log("modal closed")
  store.dispatch('getUser')
}

// only the most recent sessions are shown until the user asks for more
const HISTORY_PREVIEW = 5
const showAllHistory = ref(false)

const fetchHistoryInfo = async () => {
  dataService.userLoginHistory()
    .then( res => loginHistoryInfo.value = res)
    .catch(err => console.log(err))
}

fetchHistoryInfo()

// sign in provider, these are product names
const providerLabels: Record<string, string> = {
  [Provider.JELU_DB]: 'Jelu',
  [Provider.LDAP]: 'LDAP',
  [Provider.PROXY]: 'Proxy',
}

const providerLabel = computed(() => {
  const provider = user.value?.provider
  return provider != null ? providerLabels[provider] ?? provider : null
})

// dates come as zoned date times, ie 2026-10-16T03:24:53.359Z[Etc/UTC]
const parseDate = (date: string | undefined) => date == null ? null : dayjs(date.replace(/\[.*\]$/, ''))

const activityDate = (date: string | undefined) => {
  const parsed = parseDate(date)
  if (parsed == null) {
    return ''
  }
  return parsed.isValid() ? d(parsed.toDate(), 'long') : date
}

const sortedHistory = computed(() => {
  const time = (info: LoginHistoryInfo) => {
    const parsed = parseDate(info.date)
    return parsed != null && parsed.isValid() ? parsed.valueOf() : 0
  }
  return [...loginHistoryInfo.value].sort((a, b) => time(b) - time(a))
})

const visibleHistory = computed(() => showAllHistory.value ? sortedHistory.value : sortedHistory.value.slice(0, HISTORY_PREVIEW))
</script>

<template>
  <div class="flex max-w-3xl flex-col gap-6">
    <section class="m3-card m3-card--outlined">
      <div class="m3-card__content gap-6 p-6">
        <div class="flex items-center gap-3">
          <i
            class="mdi mdi-account-outline text-[1.5rem] text-on-surface-variant"
            aria-hidden="true"
          />
          <h2 class="text-title-large">
            {{ t('settings.profile') }}
          </h2>
        </div>
        <div class="flex items-center gap-4">
          <m-avatar
            :name="store.getters.getUsername ?? ''"
            size="lg"
          />
          <div class="flex min-w-0 flex-1 flex-col gap-2">
            <p class="truncate text-title-medium text-on-surface">
              <span class="sr-only">{{ t('settings.username') }}</span>
              {{ store.getters.getUsername }}
            </p>
            <ul
              class="flex flex-wrap gap-2"
              :aria-label="t('settings.role', 2)"
            >
              <li class="m3-status m3-status--secondary">
                <i
                  class="mdi mdi-account-outline"
                  aria-hidden="true"
                />
                <span>{{ t('profile.role_user') }}</span>
              </li>
              <li
                v-if="store.getters.isAdmin"
                class="m3-status m3-status--tertiary"
              >
                <i
                  class="mdi mdi-shield-account-outline"
                  aria-hidden="true"
                />
                <span>{{ t('admin_user.admin') }}</span>
              </li>
              <li
                v-if="providerLabel"
                v-tooltip="t('login_info.source')"
                class="m3-status m3-status--outlined"
              >
                <i
                  class="mdi mdi-key-outline"
                  aria-hidden="true"
                />
                <span>{{ providerLabel }}</span>
              </li>
            </ul>
          </div>
        </div>
        <div
          v-if="user != null && user.provider !== Provider.LDAP && user.provider !== Provider.PROXY"
          class="flex justify-end"
        >
          <button
            type="button"
            class="m3-btn m3-btn--tonal"
            @click="toggleUserModal"
          >
            <i
              class="mdi mdi-pencil-outline"
              aria-hidden="true"
            />
            <span>{{ t('profile.edit_user') }}</span>
          </button>
        </div>
      </div>
    </section>

    <section class="m3-card m3-card--outlined">
      <header class="flex items-center gap-3 px-6 pt-6 pb-2">
        <i
          class="mdi mdi-history text-[1.5rem] text-on-surface-variant"
          aria-hidden="true"
        />
        <h2 class="text-title-large">
          {{ t('login_info.activity') }}
        </h2>
      </header>
      <ul
        v-if="loginHistoryInfo.length > 0"
        class="m3-list px-2"
      >
        <li
          v-for="(line, index) in visibleHistory"
          :key="`${line.date}-${index}`"
          class="m3-list-item m3-list-item--two-line"
        >
          <span class="m3-list-item__leading">
            <i
              class="mdi mdi-login-variant"
              aria-hidden="true"
            />
          </span>
          <div class="m3-list-item__content">
            <span class="m3-list-item__headline">
              <span class="sr-only">{{ t('login_info.last_activity') }}</span>
              {{ activityDate(line.date) }}
            </span>
            <span
              class="m3-list-item__supporting truncate"
              :title="line.userAgent"
            >
              <span class="sr-only">{{ t('login_info.ip') }}</span>{{ line.ip }}<template v-if="line.source">
                · <span class="sr-only">{{ t('login_info.source') }}</span>{{ providerLabels[line.source] ?? line.source }}
              </template><template v-if="line.userAgent">
                · <span class="sr-only">{{ t('login_info.user_agent') }}</span>{{ line.userAgent }}
              </template>
            </span>
          </div>
        </li>
      </ul>
      <div
        v-if="sortedHistory.length > HISTORY_PREVIEW"
        class="flex justify-end px-4 pb-4"
      >
        <button
          type="button"
          class="m3-btn m3-btn--text"
          :aria-expanded="showAllHistory"
          @click="showAllHistory = !showAllHistory"
        >
          <i
            class="mdi"
            :class="showAllHistory ? 'mdi-chevron-up' : 'mdi-chevron-down'"
            aria-hidden="true"
          />
          <span>{{ showAllHistory ? t('labels.show_less') : t('labels.show_all', { count: sortedHistory.length }) }}</span>
        </button>
      </div>
      <p
        v-if="loginHistoryInfo.length === 0"
        class="px-6 pt-2 pb-6 text-body-medium text-on-surface-variant"
      >
        {{ t('labels.list_empty') }}
      </p>
    </section>
  </div>
</template>
