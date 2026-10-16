<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { computed, reactive, Ref, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import { OAuth2ClientDto } from '../model/oauth-client-dto'
import dataService from "../services/DataService"
import { key } from '../store'
import urls from '../urls'
import { StringUtils } from '../utils/StringUtils'
import MTextField from './m3/MTextField.vue'

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const store = useStore(key)
const form = reactive({'login' : '', 'password' : ''})
const loginValidation = ref('')
const passwordValidation = ref('')
const errorMessage = ref('')
const progress: Ref<boolean> = ref(false)
const showPassword = ref(false)
const providers: Ref<Array<OAuth2ClientDto>> = ref([])

const getOauthproviders = () => {
  dataService.oauth2Providers().then(res => {
    providers.value = res
  })
}

useTitle('Jelu | Login')

watch(form, (oldVal, newVal) => {
  if (displayInitialSetup.value) {
    validateInput()
  }
  else {
    validateInputLight()
  }
})

watch(passwordValidation, (oldVal, newVal) => {
  validateInput()
})

watch(loginValidation, (oldVal, newVal) => {
  validateInput()
})

const displayInitialSetup = computed(() => {
  return store.getters.getDisplayInitialSetup
})

const logUser = async () => {
  if (validateInputLight()) {
    progress.value = true
    try {
      await store.dispatch('authenticate', {"user" : form.login, "password" : form.password})
      progress.value = false
    } catch (error: any) {
      progress.value = false
      console.log('failed to auth user ' + error)
      errorMessage.value = error.message
    }
  }
}

const validateInputLight = (): boolean => {
  let isValid = true;
  errorMessage.value = ''
  if (!StringUtils.isNotBlank(form.login)) {
    errorMessage.value = errorMessage.value + " " + t('login.login_not_empty')
    isValid = false
  }
  if (!StringUtils.isNotBlank(form.password)) {
    errorMessage.value = errorMessage.value + " " + t('login.password_not_empty')
    isValid = false
  }
  return isValid
}

const validateInput = (): boolean => {
  let isValid: boolean = validateInputLight()
  errorMessage.value = ''
  if (form.login !== loginValidation.value) {
    errorMessage.value = t('login.login_not_match')
    isValid = false;
  }
  if (form.password !== passwordValidation.value) {
    errorMessage.value = errorMessage.value + " " + t('login.password_not_match')
    isValid = false;
  }
  if (StringUtils.isNotBlank(form.login) && form.login.length < 3) {
    errorMessage.value = errorMessage.value + " " + t('login.login_length')
    isValid = false
  }
  if (StringUtils.isNotBlank(form.password) && form.password.length < 3) {
    errorMessage.value = errorMessage.value + " " + t('login.password_length')
    isValid = false
  }
  return isValid
}

const createInitialUser = async () => {
  if (validateInput()) {
    progress.value = true
    try {
      await store.dispatch('createInitialUser', {"user" : form.login, "password" : form.password})
      progress.value = false
    } catch (error: any) {
      progress.value = false
      console.log('failed to create user ' + error)
      console.log(`failed to create user ${error.message}`)

      errorMessage.value = error.message
    }
  }
}

const oauth2Login = (provider: OAuth2ClientDto) => {
const url = `${urls.BASE_URL}/oauth2/authorization/${provider.registrationId}`
      const height = 600
      const width = 600
      const y = window.top!.outerHeight / 2 + window.top!.screenY - (height / 2)
      const x = window.top!.outerWidth / 2 + window.top!.screenX - (width / 2)
      window.open(url, 'oauth2Login',
        `toolbar=no,
        location=off,
        status=no,
        menubar=no,
        scrollbars=yes,
        resizable=yes,
        top=${y},
        left=${x},
        width=${height},
        height=${width}`,
      )
}

const submitLogin = () => {
  if (displayInitialSetup.value) {
    createInitialUser()
  }
  else {
    logUser()
  }
}

getOauthproviders()
</script>

<template>
  <div class="m3-page flex min-h-[calc(100dvh-4rem)] items-center justify-center">
    <form
      class="m3-card m3-card--outlined w-full max-w-md"
      @submit.prevent="submitLogin"
    >
      <div class="m3-card__content gap-6 p-6 sm:p-8">
        <div class="flex flex-col items-center gap-3 text-center">
          <img
            src="../assets/jelu_logo.svg"
            alt=""
            class="h-16 w-16 rounded-md"
          >
          <h1 class="text-headline-small text-on-surface">
            {{ displayInitialSetup ? t('login.create_first_user') : t('login.login') }}
          </h1>
        </div>
        <div class="flex flex-col gap-4">
          <m-text-field
            v-model="form.login"
            :label="t('login.username')"
            leading-icon="account-outline"
            maxlength="50"
            autocomplete="username"
          />
          <m-text-field
            v-if="displayInitialSetup"
            v-model="loginValidation"
            :label="t('login.confirm_login')"
            leading-icon="account-check-outline"
            maxlength="50"
            autocomplete="username"
          />
          <m-text-field
            v-model="form.password"
            :label="t('login.password')"
            :type="showPassword ? 'text' : 'password'"
            leading-icon="lock-outline"
            maxlength="150"
            :autocomplete="displayInitialSetup ? 'new-password' : 'current-password'"
          >
            <template #trailing>
              <button
                v-tooltip="showPassword ? t('login.hide_password') : t('login.show_password')"
                type="button"
                class="m3-icon-btn"
                :aria-label="showPassword ? t('login.hide_password') : t('login.show_password')"
                :aria-pressed="showPassword"
                @click="showPassword = !showPassword"
              >
                <i
                  class="mdi"
                  :class="showPassword ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
                  aria-hidden="true"
                />
              </button>
            </template>
          </m-text-field>
          <m-text-field
            v-if="displayInitialSetup"
            v-model="passwordValidation"
            :label="t('login.confirm_password')"
            :type="showPassword ? 'text' : 'password'"
            leading-icon="lock-check-outline"
            maxlength="150"
            autocomplete="new-password"
          />
        </div>
        <p
          v-if="errorMessage"
          class="flex items-start gap-2 rounded-md bg-error-container p-3 text-body-medium text-on-error-container"
          role="alert"
        >
          <i
            class="mdi mdi-alert-circle-outline text-[1.25rem]"
            aria-hidden="true"
          />
          <span>{{ errorMessage }}</span>
        </p>
        <button
          type="submit"
          class="m3-btn m3-btn--filled m3-btn--block"
          :disabled="progress"
        >
          <span
            v-if="progress"
            class="m3-spinner"
            aria-hidden="true"
          />
          <span>{{ displayInitialSetup ? t('login.create_first_user') : t('login.login') }}</span>
        </button>
        <template v-if="!displayInitialSetup && providers.length > 0">
          <div class="flex items-center gap-4 text-label-medium text-on-surface-variant">
            <hr class="m3-divider flex-1">
            <span>{{ t('login.or') }}</span>
            <hr class="m3-divider flex-1">
          </div>
          <div class="flex flex-col gap-2">
            <button
              v-for="provider in providers"
              :key="provider.name"
              type="button"
              class="m3-btn m3-btn--outlined m3-btn--block"
              @click="oauth2Login(provider)"
            >
              <i
                :class="'mdi mdi-' + provider.registrationId"
                aria-hidden="true"
              />
              <span>{{ t("labels.social_login", {provider: provider.registrationId}) }}</span>
            </button>
          </div>
        </template>
      </div>
    </form>
  </div>
</template>

<style lang="scss" scoped>

</style>
