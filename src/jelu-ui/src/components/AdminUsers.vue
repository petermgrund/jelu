<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next"
import { useTitle } from '@vueuse/core'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import dataService from "../services/DataService"
import { ObjectUtils } from "../utils/ObjectUtils"
import { Provider } from "../model/User"
import MTextField from "./m3/MTextField.vue"

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | Users admin')

const oruga = useOruga()

const form = ref({'login' : '', 'password' : '', 'password_confirm': '', 'admin': false})

async function createUser() {
  console.log("create user")
  console.log(form)
  try {
    await dataService.createUser({"login" : form.value.login, "password": form.value.password, "admin" : form.value.admin, "provider": Provider.JELU_DB})
    ObjectUtils.toast(oruga, "success", t('admin_user.user_saved', {name : form.value.login}), 4000)
    form.value.password = ''
    form.value.password_confirm = ''
    form.value.login = ''
    form.value.admin = false
  } catch (err: any) {
    console.log('error creating user')
    console.log(err)
  }
}

const isValid = computed(() => {
  return form.value.login.length >=3 && form.value.password.length >=3 && form.value.password_confirm.length >=3 && form.value.password === form.value.password_confirm
})

const passwordMismatch = computed(() => form.value.password_confirm.length > 0 && form.value.password !== form.value.password_confirm)
</script>

<template>
  <div class="flex max-w-3xl flex-col gap-6">
    <form
      class="m3-card m3-card--outlined"
      @submit.prevent="isValid && createUser()"
    >
      <div class="m3-card__content gap-6 p-6">
        <div class="flex items-center gap-3">
          <i
            class="mdi mdi-account-plus-outline text-[1.5rem] text-on-surface-variant"
            aria-hidden="true"
          />
          <h2 class="text-title-large">
            {{ t('admin_user.create_user') }}
          </h2>
        </div>
        <div class="grid gap-4 sm:grid-cols-2">
          <m-text-field
            v-model="form.login"
            :label="t('login.username')"
            leading-icon="account-outline"
            autocomplete="off"
            minlength="3"
            :supporting-text="t('login.login_length')"
            class="sm:col-span-2"
            required
          />
          <m-text-field
            v-model="form.password"
            :label="t('admin_user.password')"
            type="password"
            leading-icon="lock-outline"
            autocomplete="new-password"
            minlength="3"
            :supporting-text="t('login.password_length')"
            required
          />
          <m-text-field
            v-model="form.password_confirm"
            :label="t('admin_user.password_confirm')"
            type="password"
            leading-icon="lock-check-outline"
            autocomplete="new-password"
            minlength="3"
            :error="passwordMismatch ? t('login.password_not_match') : false"
            required
          />
        </div>
        <label class="m3-choice m3-choice--between">
          <span class="flex flex-col py-2">
            <span>{{ t('admin_user.admin') }}</span>
            <span class="text-body-small text-on-surface-variant">{{ t('admin_user.admin_help') }}</span>
          </span>
          <input
            v-model="form.admin"
            type="checkbox"
            role="switch"
            class="m3-switch"
          >
        </label>
        <div class="flex justify-end">
          <button
            type="submit"
            class="m3-btn m3-btn--filled"
            :disabled="! isValid"
          >
            <i
              class="mdi mdi-account-plus-outline"
              aria-hidden="true"
            />
            <span>{{ t('admin_user.create_user') }}</span>
          </button>
        </div>
      </div>
    </form>
  </div>
</template>
