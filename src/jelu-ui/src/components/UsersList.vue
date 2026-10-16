<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { Ref, computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import { User } from '../model/User'
import dataService from "../services/DataService"
import { key } from '../store'
import { ObjectUtils } from '../utils/ObjectUtils'
import MAvatar from './m3/MAvatar.vue'
import MEmptyState from './m3/MEmptyState.vue'

useTitle('Jelu | Users')

const store = useStore(key)
const isAdmin = computed(() => {
  return store !== undefined && store.getters.isAdmin
})
const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const users: Ref<Array<User>> = ref([]);
// avoids showing the empty state before the first answer
const usersLoaded = ref(false)

const getUsers = async () => {
  try {
    users.value = await dataService.getUsers()
  } catch (error) {
    console.log("failed get users : " + error);
  }
  usersLoaded.value = true
};

const filtered = computed(() => {
  return users.value.filter(u => u.id != store.getters.getUser.id).sort((a, b) => a.login.toLowerCase() < b.login.toLowerCase() ? -1 : 1)
})

const deleteUser = async (user: User) => {
    let abort = false
    await ObjectUtils.swalYesNoMixin.fire({
      html: `<p>${t('admin_user.delete_user')} ?</p>`,
      showCancelButton: true,
      showConfirmButton: true,
      confirmButtonText: t('labels.delete'),
      cancelButtonText: t('labels.dont_delete')
    }).then((result) => {
      if (result.isDismissed) {
        abort = true
        return;
      }
    })
    if (abort) {
      return
    }
    if (user.id != null) {
        dataService.deleteUser(user.id)
        .then(_ => {
            getUsers()
    })
    .catch(err => {
      console.log(err)
    })
    }
}

getUsers()

</script>

<template>
  <div class="flex max-w-3xl flex-col gap-6">
    <section class="m3-card m3-card--outlined">
      <header class="flex items-center gap-3 px-6 pt-6 pb-2">
        <i
          class="mdi mdi-account-multiple-outline text-[1.5rem] text-on-surface-variant"
          aria-hidden="true"
        />
        <h2 class="text-title-large">
          {{ t('settings.users') }}
        </h2>
      </header>
      <TransitionGroup
        v-if="filtered.length > 0"
        name="list"
        tag="ul"
        class="m3-list px-2"
      >
        <li
          v-for="user in filtered"
          :key="user.id"
          class="m3-list-item m3-list-item--interactive"
        >
          <span class="m3-list-item__leading">
            <m-avatar :name="user.login" />
          </span>
          <div class="m3-list-item__content">
            <div class="flex min-w-0 items-center gap-2">
              <router-link
                class="m3-list-item__headline whitespace-nowrap after:absolute after:inset-0"
                :to="{ name: 'user-detail', params: { userId: user.id } }"
              >
                {{ user.login }}
              </router-link>
              <span
                v-if="user.admin"
                class="m3-status m3-status--tertiary"
              >
                <i
                  class="mdi mdi-shield-account-outline"
                  aria-hidden="true"
                />
                <span>{{ t('admin_user.admin') }}</span>
              </span>
            </div>
          </div>
          <div
            v-if="isAdmin"
            class="m3-list-item__trailing"
          >
            <button
              v-tooltip="t('admin_user.delete_user')"
              type="button"
              class="m3-icon-btn relative z-[1]"
              :aria-label="`${t('admin_user.delete_user')} ${user.login}`"
              @click="deleteUser(user)"
            >
              <i
                class="mdi mdi-delete-outline"
                aria-hidden="true"
              />
            </button>
          </div>
        </li>
      </TransitionGroup>
      <m-empty-state
        v-else-if="usersLoaded"
        icon="account-multiple-outline"
        :title="t('labels.list_empty')"
      />
    </section>
  </div>
</template>
