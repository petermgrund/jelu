<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next"
import { useTitle } from '@vueuse/core'
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import { key } from '../store'
import dataService from "../services/DataService"
import { ObjectUtils } from "../utils/ObjectUtils"
import { ApiToken, TokenScope, CreateApiToken } from "../model/ApiToken"
import dayjs from "dayjs"
import MDialog from "./m3/MDialog.vue"
import MEmptyState from "./m3/MEmptyState.vue"
import MModal from "./m3/MModal.vue"
import MSelect from "./m3/MSelect.vue"
import MTextField from "./m3/MTextField.vue"

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | API Tokens')

const store = useStore(key)
const oruga = useOruga()

const tokens = ref<ApiToken[]>([])
const scopes = ref<TokenScope[]>([])
const loading = ref(false)
const showCreateModal = ref(false)
const showTokenModal = ref(false)
const createdRawToken = ref('')

const form = ref<CreateApiToken>({
  name: '',
  scopes: [],
  expiresAt: undefined
})

const expirationOption = ref('never')
const customExpiration = ref('')

onMounted(async () => {
  await loadTokens()
  await loadScopes()
})

async function loadTokens() {
  loading.value = true
  try {
    tokens.value = await dataService.getApiTokens()
  } catch (err: any) {
    console.log('error loading tokens', err)
    ObjectUtils.toast(oruga, "danger", t('api_tokens.load_error'), 4000)
  } finally {
    loading.value = false
  }
}

async function loadScopes() {
  try {
    scopes.value = await dataService.getApiTokenScopes()
  } catch (err: any) {
    console.log('error loading scopes', err)
  }
}

async function createToken() {
  try {
    // Handle expiration
    let expiresAt: string | undefined = undefined
    if (expirationOption.value === '30days') {
      expiresAt = dayjs().add(30, 'day').toISOString()
    } else if (expirationOption.value === '90days') {
      expiresAt = dayjs().add(90, 'day').toISOString()
    } else if (expirationOption.value === '1year') {
      expiresAt = dayjs().add(1, 'year').toISOString()
    } else if (expirationOption.value === 'custom' && customExpiration.value) {
      expiresAt = dayjs(customExpiration.value).toISOString()
    }

    const result = await dataService.createApiToken({
      name: form.value.name,
      scopes: form.value.scopes,
      expiresAt: expiresAt
    })

    // Show the raw token
    createdRawToken.value = result.rawToken
    showCreateModal.value = false
    showTokenModal.value = true

    // Reset form
    form.value = { name: '', scopes: [], expiresAt: undefined }
    expirationOption.value = 'never'
    customExpiration.value = ''

    // Reload tokens list
    await loadTokens()

    ObjectUtils.toast(oruga, "success", t('api_tokens.created'), 4000)
  } catch (err: any) {
    console.log('error creating token', err)
    ObjectUtils.toast(oruga, "danger", t('api_tokens.create_error'), 4000)
  }
}

async function revokeToken(token: ApiToken) {
  if (!confirm(t('api_tokens.revoke_confirm', { name: token.name }))) {
    return
  }

  try {
    await dataService.deleteApiToken(token.id)
    await loadTokens()
    ObjectUtils.toast(oruga, "success", t('api_tokens.revoked'), 4000)
  } catch (err: any) {
    console.log('error revoking token', err)
    ObjectUtils.toast(oruga, "danger", t('api_tokens.revoke_error'), 4000)
  }
}

async function toggleTokenActive(token: ApiToken) {
  try {
    await dataService.updateApiToken(token.id, { isActive: !token.isActive })
    await loadTokens()
    ObjectUtils.toast(oruga, "success", token.isActive ? t('api_tokens.deactivated') : t('api_tokens.activated'), 4000)
  } catch (err: any) {
    console.log('error updating token', err)
    ObjectUtils.toast(oruga, "danger", t('api_tokens.update_error'), 4000)
  }
}

function copyToken() {
  navigator.clipboard.writeText(createdRawToken.value)
  ObjectUtils.toast(oruga, "success", t('api_tokens.copied'), 2000)
}

function closeTokenModal() {
  showTokenModal.value = false
  createdRawToken.value = ''
}

function formatDate(dateStr: string | undefined) {
  if (!dateStr) return '-'
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm')
}

const isFormValid = computed(() => {
  return form.value.name.length >= 1 && form.value.scopes.length >= 1
})

const scopesByCategory = computed(() => {
  const grouped: Record<string, TokenScope[]> = {}
  for (const scope of scopes.value) {
    if (!grouped[scope.category]) {
      grouped[scope.category] = []
    }
    grouped[scope.category].push(scope)
  }
  return grouped
})

function toggleScope(scopeName: string) {
  const idx = form.value.scopes.indexOf(scopeName)
  if (idx >= 0) {
    form.value.scopes.splice(idx, 1)
  } else {
    form.value.scopes.push(scopeName)
  }
}

const expirationOptions = computed(() => [
  { value: 'never', label: t('api_tokens.never_expires') },
  { value: '30days', label: t('api_tokens.expires_30days') },
  { value: '90days', label: t('api_tokens.expires_90days') },
  { value: '1year', label: t('api_tokens.expires_1year') },
  { value: 'custom', label: t('api_tokens.expires_custom') },
])

const isExpired = (token: ApiToken) => token.expiresAt != null && dayjs(token.expiresAt).isBefore(dayjs())

// closing the dialog in any way (done, close button, escape, scrim) forgets the raw token
const tokenDialogOpen = computed({
  get: () => showTokenModal.value,
  set: (open: boolean) => {
    if (!open) {
      closeTokenModal()
    }
  },
})
</script>

<template>
  <div class="flex max-w-3xl flex-col gap-6">
    <section class="m3-card m3-card--outlined">
      <div class="m3-card__content gap-4 p-6">
        <div class="flex items-start gap-3">
          <i
            class="mdi mdi-key-outline text-[1.5rem] text-on-surface-variant"
            aria-hidden="true"
          />
          <div class="flex flex-1 flex-col gap-1">
            <h2 class="text-title-large">
              {{ t('api_tokens.title') }}
            </h2>
            <p class="text-body-medium text-on-surface-variant">
              {{ t('api_tokens.description') }}
            </p>
          </div>
        </div>
        <div class="flex justify-end">
          <button
            type="button"
            class="m3-btn m3-btn--filled"
            @click="showCreateModal = true"
          >
            <i
              class="mdi mdi-plus"
              aria-hidden="true"
            />
            <span>{{ t('api_tokens.create') }}</span>
          </button>
        </div>
      </div>
      <hr class="m3-divider">

      <div
        v-if="loading"
        class="flex justify-center p-6"
      >
        <span
          class="m3-spinner m3-spinner--lg"
          role="progressbar"
          :aria-label="t('labels.loading')"
        />
      </div>

      <m-empty-state
        v-else-if="tokens.length === 0"
        icon="key-outline"
        :title="t('api_tokens.no_tokens')"
      />

      <ul
        v-else
        class="m3-list px-2"
      >
        <li
          v-for="token in tokens"
          :key="token.id"
          class="m3-list-item m3-list-item--three-line"
        >
          <span
            class="m3-list-item__leading h-10 w-10 rounded-full"
            :class="token.isActive && !isExpired(token) ? 'bg-secondary-container text-on-secondary-container' : 'bg-surface-container-highest text-on-surface-variant'"
          >
            <i
              class="mdi mdi-key-variant text-[1.25rem]"
              aria-hidden="true"
            />
          </span>
          <div class="m3-list-item__content gap-2">
            <div class="flex flex-wrap items-center gap-2">
              <span class="m3-list-item__headline">{{ token.name }}</span>
              <span
                v-if="!token.isActive"
                class="m3-status"
              >
                <i
                  class="mdi mdi-pause-circle-outline"
                  aria-hidden="true"
                />
                <span>{{ t('api_tokens.inactive') }}</span>
              </span>
              <span
                v-if="isExpired(token)"
                class="m3-status m3-status--error"
              >
                <i
                  class="mdi mdi-clock-alert-outline"
                  aria-hidden="true"
                />
                <span>{{ t('api_tokens.expired') }}</span>
              </span>
            </div>
            <ul
              class="flex flex-wrap gap-1"
              :aria-label="t('api_tokens.scopes')"
            >
              <li
                v-for="scope in token.scopes"
                :key="scope"
                class="m3-status m3-status--outlined"
              >
                <span>{{ scope }}</span>
              </li>
            </ul>
            <dl class="flex flex-wrap gap-x-4 gap-y-1 text-body-small">
              <div class="flex gap-1">
                <dt class="text-on-surface-variant">
                  {{ t('api_tokens.created_at') }}
                </dt>
                <dd class="text-on-surface tabular-nums">
                  {{ formatDate(token.createdAt) }}
                </dd>
              </div>
              <div class="flex gap-1">
                <dt class="text-on-surface-variant">
                  {{ t('api_tokens.last_used') }}
                </dt>
                <dd class="text-on-surface tabular-nums">
                  {{ formatDate(token.lastUsedAt) }}
                </dd>
              </div>
              <div class="flex gap-1">
                <dt class="text-on-surface-variant">
                  {{ t('api_tokens.usage_count') }}
                </dt>
                <dd class="text-on-surface tabular-nums">
                  {{ token.usageCount }}
                </dd>
              </div>
              <div
                v-if="token.expiresAt"
                class="flex gap-1"
              >
                <dt class="text-on-surface-variant">
                  {{ t('api_tokens.expires_at') }}
                </dt>
                <dd class="text-on-surface tabular-nums">
                  {{ formatDate(token.expiresAt) }}
                </dd>
              </div>
            </dl>
          </div>
          <div class="m3-list-item__trailing">
            <button
              v-tooltip="token.isActive ? t('api_tokens.deactivate') : t('api_tokens.activate')"
              type="button"
              class="m3-icon-btn"
              :aria-label="`${token.isActive ? t('api_tokens.deactivate') : t('api_tokens.activate')} ${token.name}`"
              @click="toggleTokenActive(token)"
            >
              <i
                class="mdi"
                :class="token.isActive ? 'mdi-pause-circle-outline' : 'mdi-play-circle-outline'"
                aria-hidden="true"
              />
            </button>
            <button
              v-tooltip="t('api_tokens.revoke')"
              type="button"
              class="m3-icon-btn"
              :aria-label="`${t('api_tokens.revoke')} ${token.name}`"
              @click="revokeToken(token)"
            >
              <i
                class="mdi mdi-delete-outline"
                aria-hidden="true"
              />
            </button>
          </div>
        </li>
      </ul>
    </section>
  </div>

  <!-- Create Token Modal -->
  <m-modal v-model="showCreateModal">
    <m-dialog
      :headline="t('api_tokens.create_new')"
      icon="key-plus"
      size="md"
      :fullscreen="true"
      @close="showCreateModal = false"
    >
      <div class="flex flex-col gap-6 pt-2">
        <m-text-field
          v-model="form.name"
          :label="t('api_tokens.token_name')"
          :placeholder="t('api_tokens.name_placeholder')"
          leading-icon="label-outline"
        />

        <fieldset class="flex min-w-0 flex-col gap-4">
          <legend class="mb-2 text-title-small text-on-surface">
            {{ t('api_tokens.select_scopes') }}
          </legend>
          <div
            v-for="(categoryScopes, category) in scopesByCategory"
            :key="category"
            class="flex flex-col gap-1"
          >
            <h3 class="text-label-large text-on-surface-variant">
              {{ category }}
            </h3>
            <div class="grid sm:grid-cols-2 sm:gap-x-4">
              <label
                v-for="scope in categoryScopes"
                :key="scope.name"
                class="m3-choice items-start py-2"
              >
                <input
                  type="checkbox"
                  class="m3-checkbox mt-0.5"
                  :checked="form.scopes.includes(scope.name)"
                  @change="toggleScope(scope.name)"
                >
                <span class="flex min-w-0 flex-col">
                  <span class="text-body-medium text-on-surface">{{ scope.name }}</span>
                  <span class="text-body-small text-on-surface-variant">{{ scope.description }}</span>
                </span>
              </label>
            </div>
          </div>
        </fieldset>

        <div class="grid gap-4 sm:grid-cols-2">
          <m-select
            v-model="expirationOption"
            :label="t('api_tokens.expiration')"
            :options="expirationOptions"
            leading-icon="calendar-clock-outline"
          />
          <m-text-field
            v-if="expirationOption === 'custom'"
            v-model="customExpiration"
            :label="t('api_tokens.expires_custom')"
            type="date"
          />
        </div>
      </div>
      <template #actions>
        <button
          type="button"
          class="m3-btn m3-btn--text"
          @click="showCreateModal = false"
        >
          {{ t('labels.cancel') }}
        </button>
        <button
          type="button"
          class="m3-btn m3-btn--filled"
          :disabled="!isFormValid"
          @click="createToken"
        >
          {{ t('api_tokens.create') }}
        </button>
      </template>
    </m-dialog>
  </m-modal>

  <!-- Token Created Modal (shows raw token once) -->
  <m-modal v-model="tokenDialogOpen">
    <m-dialog
      :headline="t('api_tokens.token_created')"
      icon="key-variant"
      size="md"
      @close="closeTokenModal"
    >
      <div class="flex flex-col gap-4 pt-2">
        <p
          class="flex items-center gap-3 rounded-md bg-primary-container p-4 text-body-medium text-on-primary-container"
          role="alert"
        >
          <i
            class="mdi mdi-alert-outline text-[1.25rem]"
            aria-hidden="true"
          />
          <span>{{ t('api_tokens.copy_warning') }}</span>
        </p>
        <m-text-field
          :model-value="createdRawToken"
          :label="t('api_tokens.your_token')"
          class="[&_input]:font-mono"
          spellcheck="false"
          readonly
        >
          <template #trailing>
            <button
              v-tooltip="t('labels.copy')"
              type="button"
              class="m3-icon-btn"
              :aria-label="t('labels.copy')"
              @click="copyToken"
            >
              <i
                class="mdi mdi-content-copy"
                aria-hidden="true"
              />
            </button>
          </template>
        </m-text-field>
      </div>
      <template #actions>
        <button
          type="button"
          class="m3-btn m3-btn--filled"
          @click="closeTokenModal"
        >
          {{ t('api_tokens.done') }}
        </button>
      </template>
    </m-dialog>
  </m-modal>
</template>
