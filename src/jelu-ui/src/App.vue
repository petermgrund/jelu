<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next";
import { useLocalStorage, useWindowScroll } from '@vueuse/core';
import { useRegisterSW } from 'virtual:pwa-register/vue';
import { computed, Ref, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import MAvatar from './components/m3/MAvatar.vue';
import MDialog from './components/m3/MDialog.vue';
import MMenu from './components/m3/MMenu.vue';
import MModal from './components/m3/MModal.vue';
import NavItem from './components/NavItem.vue';
import ScanModal from "./components/ScanModal.vue";
import UserShelvesModal from './components/UserShelvesModal.vue';
import dataService from "./services/DataService";
import { key } from './store';
import { StringUtils } from './utils/StringUtils';

const {
  offlineReady,
  needRefresh,
  updateServiceWorker,
} = useRegisterSW()

const closePwaSnackbar = async () => {
  offlineReady.value = false
  needRefresh.value = false
}

const store = useStore(key)
const router = useRouter()
const route = useRoute()
const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const oruga = useOruga();
const initialLoad : Ref<boolean> = ref(false)

store.dispatch('setupStatus')
initialLoad.value = true
store.dispatch('getUser')
  .then(async () => {
    if (store.state.route != null) {
      await router.push(store.state.route)
    }
    initialLoad.value = false
    store.dispatch('getServerSettings')
  })
  .catch(() => {
    if (store.state.route != null && (
      store.state.route.name === "review-detail" ||
      store.state.route.name === "list-detail"||
      store.state.route.name === "book-reviews")) {
      router.push(store.state.route)
      initialLoad.value = false
      return
    }
    initialLoad.value = false
    router.push({ name: 'login' }).then(() => { console.log("ok nav") }).catch(() => { console.log("error nav") })
  })

const username = computed(() => {
  return store.getters.getUsername
})
const isLogged = computed(() => {
  return store.getters.getLogged
})

const logout = () => {
  dataService.logout()
    .then(() => {
      store.dispatch('logout')
    })
}

// navigation: rail on medium and larger windows, drawer and bar on compact ones
const railExpanded = useLocalStorage('jelu_nav_expanded', false)
const drawerOpen = ref(false)
const shortcutsOpen = ref(false)

watch(() => route.fullPath, () => {
  drawerOpen.value = false
})

const destinations = computed(() => [
  { name: 'home', label: t('nav.home'), icon: 'home-outline', activeIcon: 'home' },
  { name: 'my-books', label: t('nav.my_books'), icon: 'book-multiple-outline', activeIcon: 'book-multiple' },
  { name: 'on-my-shelf', label: t('nav.on_my_shelf'), icon: 'bookshelf', activeIcon: 'bookshelf' },
  { name: 'to-read', label: t('nav.to_read'), icon: 'bookmark-outline', activeIcon: 'bookmark' },
  { name: 'random', label: t('nav.random'), icon: 'dice-multiple-outline', activeIcon: 'dice-multiple' },
  { name: 'history', label: t('nav.history'), icon: 'calendar-month-outline', activeIcon: 'calendar-month' },
  { name: 'reviews', label: t('nav.activity'), icon: 'comment-text-multiple-outline', activeIcon: 'comment-text-multiple' },
  { name: 'authors', label: t('book.author', 2), icon: 'account-group-outline', activeIcon: 'account-group' },
  { name: 'search', label: t('nav.search'), icon: 'text-search', activeIcon: 'text-search' },
])

const barDestinations = computed(() => [
  { name: 'home', label: t('nav.home'), icon: 'home-outline', activeIcon: 'home' },
  { name: 'my-books', label: t('nav.my_books'), icon: 'book-multiple-outline', activeIcon: 'book-multiple' },
  { name: 'add-book', label: t('nav.add_book'), icon: 'plus-circle-outline', activeIcon: 'plus-circle' },
  { name: 'on-my-shelf', label: t('nav.on_my_shelf'), icon: 'bookshelf', activeIcon: 'bookshelf' },
  { name: 'to-read', label: t('nav.to_read'), icon: 'bookmark-outline', activeIcon: 'bookmark' },
])

// top app bar becomes a container once the page is scrolled
const { y: scrollY } = useWindowScroll()
const scrolled = computed(() => scrollY.value > 0)

const searchQuery = ref('')
const showSearchInput = computed(() => route.name !== 'search')

const search = () => {
  if (StringUtils.isNotBlank(searchQuery.value)) {
    router.push({ path: '/search', query: { q: searchQuery.value } })
  }
}

function toggleScanModal() {
  oruga.modal.open({
    component: ScanModal,
    trapFocus: true,
    active: true,
    cancelable: ['escape', 'outside'],
    events: {
      decoded: (barcode: string|null) => {
        if (barcode != null) {
          searchQuery.value = barcode
          search()
        }
      },
    },
  });
}

function toggleShelvesModal() {
  drawerOpen.value = false
  oruga.modal.open({
    component: UserShelvesModal,
    trapFocus: true,
    active: true,
    cancelable: ['escape', 'outside'],
  });
}
</script>

<template>
  <a
    href="#main-content"
    class="sr-only z-[80] rounded-full bg-primary px-4 py-2 text-label-large text-on-primary focus:not-sr-only focus:fixed focus:top-2 focus:left-2"
  >{{ t('labels.skip_to_content') }}</a>
  <div class="flex min-h-dvh">
    <!-- navigation rail -->
    <aside
      v-if="isLogged"
      class="sticky top-0 hidden h-dvh shrink-0 flex-col overflow-y-auto bg-surface px-3 pb-4 transition-[width] duration-300 ease-standard sm:flex"
      :class="railExpanded ? 'w-64' : 'w-24'"
    >
      <div
        class="flex flex-col gap-4 pt-2 pb-6"
        :class="railExpanded ? 'items-start' : 'items-center'"
      >
        <button
          v-tooltip="railExpanded ? t('labels.collapse_navigation') : t('labels.expand_navigation')"
          type="button"
          class="m3-icon-btn h-14 w-14"
          :class="{ 'ml-1': railExpanded }"
          :aria-label="railExpanded ? t('labels.collapse_navigation') : t('labels.expand_navigation')"
          :aria-expanded="railExpanded"
          @click="railExpanded = !railExpanded"
        >
          <i
            class="mdi"
            :class="railExpanded ? 'mdi-menu-open' : 'mdi-menu'"
            aria-hidden="true"
          />
        </button>
        <router-link
          v-tooltip="railExpanded ? undefined : t('nav.add_book')"
          :to="{ name: 'add-book' }"
          class="m3-fab m3-fab--lowered"
          :class="{ 'm3-fab--extended': railExpanded }"
          :aria-label="t('nav.add_book')"
        >
          <i
            class="mdi mdi-plus"
            aria-hidden="true"
          />
          <span v-if="railExpanded">{{ t('nav.add_book') }}</span>
        </router-link>
      </div>
      <nav
        class="flex flex-col gap-1"
        :aria-label="t('labels.main_navigation')"
      >
        <nav-item
          v-for="item in destinations"
          :key="item.name"
          :to="{ name: item.name }"
          :label="item.label"
          :icon="item.icon"
          :active-icon="item.activeIcon"
          :row="railExpanded"
        />
        <nav-item
          :label="t('nav.shelves')"
          icon="tag-multiple-outline"
          :row="railExpanded"
          @click="toggleShelvesModal"
        />
      </nav>
    </aside>

    <div class="flex min-w-0 flex-1 flex-col">
      <header
        class="m3-top-app-bar"
        :class="{ 'is-scrolled': scrolled }"
      >
        <button
          v-if="isLogged"
          type="button"
          class="m3-icon-btn sm:hidden"
          :aria-label="t('labels.open_navigation')"
          @click="drawerOpen = true"
        >
          <i
            class="mdi mdi-menu"
            aria-hidden="true"
          />
        </button>
        <router-link
          :to="{ name: 'home' }"
          class="flex shrink-0 items-center gap-3 rounded-full py-1 pr-3 pl-1"
          :class="{ 'hidden sm:flex': isLogged && showSearchInput }"
        >
          <img
            src="./assets/jelu_logo.svg"
            alt=""
            class="h-10 w-10 rounded-sm"
          >
          <span class="m3-top-app-bar__title">Jelu</span>
        </router-link>
        <div class="flex min-w-0 flex-1 justify-center px-1 sm:px-4">
          <form
            v-if="isLogged && showSearchInput"
            role="search"
            class="m3-search-bar"
            @submit.prevent="search"
          >
            <span class="m3-search-bar__leading">
              <i
                class="mdi mdi-magnify"
                aria-hidden="true"
              />
            </span>
            <input
              v-model="searchQuery"
              type="search"
              class="m3-search-bar__input"
              :placeholder="t('labels.search_query')"
              :aria-label="t('labels.search')"
            >
            <button
              v-tooltip="t('labels.scan_isbn')"
              type="button"
              class="m3-icon-btn"
              :aria-label="t('labels.scan_isbn')"
              @click="toggleScanModal"
            >
              <i
                class="mdi mdi-barcode-scan"
                aria-hidden="true"
              />
            </button>
            <router-link
              v-tooltip="t('labels.advanced_search')"
              :to="{ name: 'search', query: searchQuery ? { q: searchQuery } : {} }"
              class="m3-icon-btn hidden sm:inline-grid"
              :aria-label="t('labels.advanced_search')"
            >
              <i
                class="mdi mdi-tune-variant"
                aria-hidden="true"
              />
            </router-link>
          </form>
        </div>
        <m-menu
          v-if="isLogged"
          align="end"
          menu-class="min-w-64"
        >
          <template #trigger="{ toggle, open }">
            <button
              v-tooltip="username"
              type="button"
              class="m3-icon-btn h-12 w-12"
              :aria-label="t('labels.account')"
              :aria-expanded="open"
              aria-haspopup="menu"
              @click="toggle"
            >
              <m-avatar
                :name="username"
                size="md"
              />
            </button>
          </template>
          <div class="flex items-center gap-3 px-4 pt-2 pb-3">
            <m-avatar
              :name="username"
              size="lg"
            />
            <div class="min-w-0">
              <div class="truncate text-title-medium text-on-surface">
                {{ username }}
              </div>
              <div class="text-body-small text-on-surface-variant">
                {{ t('settings.my_profile') }}
              </div>
            </div>
          </div>
          <hr class="m3-divider">
          <router-link
            :to="{ path: '/profile/me' }"
            class="m3-menu-item"
            role="menuitem"
          >
            <i
              class="mdi mdi-account-circle-outline"
              aria-hidden="true"
            />{{ t('nav.dashboard') }}
          </router-link>
          <router-link
            :to="{ path: '/profile/settings' }"
            class="m3-menu-item"
            role="menuitem"
          >
            <i
              class="mdi mdi-cog-outline"
              aria-hidden="true"
            />{{ t('settings.settings') }}
          </router-link>
          <router-link
            :to="{ path: '/profile/messages' }"
            class="m3-menu-item"
            role="menuitem"
          >
            <i
              class="mdi mdi-message-text-outline"
              aria-hidden="true"
            />{{ t('settings.messages') }}
          </router-link>
          <button
            type="button"
            class="m3-menu-item"
            role="menuitem"
            @click="shortcutsOpen = true"
          >
            <i
              class="mdi mdi-keyboard-outline"
              aria-hidden="true"
            />{{ t('settings.shortcuts') }}
          </button>
          <hr class="m3-divider">
          <button
            type="button"
            class="m3-menu-item"
            role="menuitem"
            @click="logout"
          >
            <i
              class="mdi mdi-logout"
              aria-hidden="true"
            />{{ t('nav.logout') }}
          </button>
        </m-menu>
        <router-link
          v-else-if="route.name !== 'login'"
          :to="{ name: 'login' }"
          class="m3-btn m3-btn--filled"
        >
          <i
            class="mdi mdi-login"
            aria-hidden="true"
          />
          <span>{{ t('nav.login') }}</span>
        </router-link>
      </header>

      <main
        id="main-content"
        class="flex-1"
        :class="{ 'pb-[calc(5rem+env(safe-area-inset-bottom))] sm:pb-0': isLogged }"
      >
        <router-view />
      </main>
    </div>
  </div>

  <!-- compact windows: modal navigation drawer -->
  <Transition name="jl-fade">
    <div
      v-if="drawerOpen"
      class="m3-scrim sm:hidden"
      @click="drawerOpen = false"
    />
  </Transition>
  <Transition name="jl-drawer">
    <nav
      v-if="drawerOpen"
      class="m3-nav-drawer sm:hidden"
      :aria-label="t('labels.main_navigation')"
      @keydown.esc="drawerOpen = false"
    >
      <div class="m3-nav-drawer__headline">
        <img
          src="./assets/jelu_logo.svg"
          alt=""
          class="h-8 w-8 rounded-sm"
        >
        <span class="text-title-large text-on-surface">Jelu</span>
      </div>
      <nav-item
        v-for="item in destinations"
        :key="item.name"
        :to="{ name: item.name }"
        :label="item.label"
        :icon="item.icon"
        :active-icon="item.activeIcon"
        row
      />
      <nav-item
        :label="t('nav.shelves')"
        icon="tag-multiple-outline"
        row
        @click="toggleShelvesModal"
      />
      <hr class="m3-divider my-2 mx-4">
      <nav-item
        :to="{ path: '/profile/me' }"
        :label="t('nav.dashboard')"
        icon="account-circle-outline"
        active-icon="account-circle"
        row
      />
      <nav-item
        :label="t('nav.logout')"
        icon="logout"
        row
        @click="logout"
      />
    </nav>
  </Transition>

  <!-- compact windows: navigation bar -->
  <nav
    v-if="isLogged"
    class="m3-nav-bar sm:hidden"
    :aria-label="t('labels.main_navigation')"
  >
    <nav-item
      v-for="item in barDestinations"
      :key="item.name"
      :to="{ name: item.name }"
      :label="item.label"
      :icon="item.icon"
      :active-icon="item.activeIcon"
    />
  </nav>

  <m-modal v-model="shortcutsOpen">
    <m-dialog
      :headline="t('settings.shortcuts')"
      icon="keyboard-outline"
      size="sm"
      @close="shortcutsOpen = false"
    >
      <ul class="flex flex-col gap-4 text-body-large text-on-surface">
        <li class="flex items-center justify-between gap-4">
          <span>{{ t('shortcuts.toggle_bar') }}</span>
          <span class="flex shrink-0 items-center gap-1"><kbd>Shift</kbd>+<kbd>F</kbd></span>
        </li>
        <li class="flex items-center justify-between gap-4">
          <span>{{ t('shortcuts.page_previous') }}</span>
          <span class="flex shrink-0 items-center gap-1"><kbd>Shift</kbd>+<kbd>◀︎</kbd></span>
        </li>
        <li class="flex items-center justify-between gap-4">
          <span>{{ t('shortcuts.page_next') }}</span>
          <span class="flex shrink-0 items-center gap-1"><kbd>Shift</kbd>+<kbd>▶︎</kbd></span>
        </li>
      </ul>
      <template #actions>
        <button
          type="button"
          class="m3-btn m3-btn--text"
          @click="shortcutsOpen = false"
        >
          {{ t('labels.close') }}
        </button>
      </template>
    </m-dialog>
  </m-modal>

  <Transition name="jl-fade">
    <div
      v-if="offlineReady || needRefresh"
      class="fixed inset-x-0 bottom-24 z-[60] flex justify-center px-4 sm:bottom-6"
      role="status"
    >
      <div class="m3-snackbar">
        <span class="m3-snackbar__text">
          {{ offlineReady ? t('labels.app_offline_ready') : t('labels.app_update_available') }}
        </span>
        <button
          v-if="needRefresh"
          type="button"
          class="m3-btn m3-btn--text"
          @click="updateServiceWorker()"
        >
          {{ t('labels.reload') }}
        </button>
        <button
          type="button"
          class="m3-icon-btn"
          :aria-label="t('labels.close')"
          @click="closePwaSnackbar"
        >
          <i
            class="mdi mdi-close"
            aria-hidden="true"
          />
        </button>
      </div>
    </div>
  </Transition>

  <Transition name="jl-fade">
    <div
      v-if="initialLoad"
      class="fixed inset-0 z-[70] grid place-items-center bg-surface"
      role="progressbar"
      :aria-label="t('labels.loading')"
    >
      <span class="m3-spinner m3-spinner--lg" />
    </div>
  </Transition>
</template>

<style>
.jl-fade-enter-active,
.jl-fade-leave-active {
  transition: opacity var(--md-sys-motion-duration-medium2) linear;
}

.jl-fade-enter-from,
.jl-fade-leave-to {
  opacity: 0;
}

.jl-drawer-enter-active {
  transition: transform var(--md-sys-motion-duration-medium4) var(--md-sys-motion-easing-emphasized-decelerate);
}

.jl-drawer-leave-active {
  transition: transform var(--md-sys-motion-duration-short4) var(--md-sys-motion-easing-emphasized-accelerate);
}

.jl-drawer-enter-from,
.jl-drawer-leave-to {
  transform: translateX(-100%);
}
</style>
