<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import { useI18n } from 'vue-i18n'
import MDialog from './m3/MDialog.vue'
import UserShelvesList from './UserShelvesList.vue'

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | User page')
const emit = defineEmits<{
  (e: 'close'): void,
  (e: 'shelves-changed'): void
}>()

</script>

<template>
  <m-dialog
    :headline="t('settings.shelves')"
    icon="library-shelves"
    size="sm"
    @close="emit('close')"
  >
    <user-shelves-list
      :allow-delete="false"
      :allow-create="false"
      @close="emit('close')"
    />
    <template #actions>
      <router-link
        :to="{ path: '/profile/me', query: { view: 'SHELVES' } }"
        class="m3-btn m3-btn--text"
        @click="emit('close')"
      >
        <i
          class="mdi mdi-cog-outline"
          aria-hidden="true"
        />
        <span>{{ t('settings.manage_shelves') }}</span>
      </router-link>
    </template>
  </m-dialog>
</template>
