<script setup lang="ts">
import { useThrottleFn, useTitle } from '@vueuse/core';
import { onMounted, Ref, ref, watch } from "vue";
import { useI18n } from 'vue-i18n';
import usePagination from '../composables/pagination';
import { MessageCategory, UserMessage } from "../model/UserMessage";
import dataService from "../services/DataService";
import MEmptyState from './m3/MEmptyState.vue';

const { t, d } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

useTitle('Jelu | Messages')

const { total, page, pageAsNumber, perPage, updatePage, getPageIsLoading, updatePageLoading, pageCount } = usePagination()
perPage.value = 10

const read: Ref<boolean> = ref(false)

const categories: Ref<Array<MessageCategory>> = ref([])

const messages: Ref<Array<UserMessage>> = ref([]);

const getMessagesIsLoading: Ref<boolean> = ref(false)

const getMessages = () => {
  getMessagesIsLoading.value = true
  dataService.messages(categories.value, read.value, pageAsNumber.value - 1, perPage.value, undefined)
  .then(res => {
    console.log(res)
    total.value = res.totalElements
    messages.value = res.content
    if (! res.empty) {
          page.value =  (res.number + 1).toString(10)
        }
        else {
          page.value = "1"
        }
        getMessagesIsLoading.value = false
        updatePageLoading(false)
  })
  .catch(e => {
      getMessagesIsLoading.value = false
      updatePageLoading(false)
    })
}

const updateMessage = (message: UserMessage, read: boolean) => {
  if (message.id !== undefined) {
    dataService.updateUserMessage(message.id, {read: read, message: "Message 5 updated"})
    .then(res => {
      console.log(`message ${message.id} read : ${read}`)
      getMessages()
    })
    .catch(e => {
      console.log(`error : message ${message.id} read : ${read}`)
    })
  }
}

// tonal container behind the icon of a message
function categoryClass(cat: MessageCategory) {
  if (cat === MessageCategory.ERROR) {
    return 'bg-error-container text-on-error-container'
  } else if (cat === MessageCategory.SUCCESS) {
    return 'bg-tertiary-container text-on-tertiary-container'
  } else if (cat === MessageCategory.WARNING) {
    return 'bg-primary-container text-on-primary-container'
  }
  return 'bg-secondary-container text-on-secondary-container'
}

function categoryIcon(cat: MessageCategory) {
  if (cat === MessageCategory.ERROR) {
    return 'mdi-alert-circle-outline'
  } else if (cat === MessageCategory.SUCCESS) {
    return 'mdi-check-circle-outline'
  } else if (cat === MessageCategory.WARNING) {
    return 'mdi-alert-outline'
  }
  return 'mdi-information-outline'
}

watch(page, (newVal, oldVal) => {
  console.log("all " + newVal + " " + oldVal)
  if (newVal !== oldVal) {
    throttledGetMessages()
  }
})

// watches set above sometimes called twice
const throttledGetMessages = useThrottleFn(() => {
  getMessages()
}, 100, false)

onMounted(() => {
  console.log("Component is mounted!");
  try {
    getMessages();
  } catch (error) {
    console.log("failed get messages : " + error);
  }
});
</script>

<template>
  <div class="flex max-w-3xl flex-col gap-6">
    <section class="m3-card m3-card--outlined">
      <header class="flex items-center gap-3 px-6 pt-6 pb-2">
        <i
          class="mdi mdi-message-text-outline text-[1.5rem] text-on-surface-variant"
          aria-hidden="true"
        />
        <h2 class="text-title-large">
          {{ t('settings.messages') }}
        </h2>
      </header>
      <ul
        v-if="messages.length > 0"
        class="m3-list px-2"
      >
        <li
          v-for="message in messages"
          :key="message.id"
          class="m3-list-item items-start py-3"
        >
          <span
            class="m3-list-item__leading h-10 w-10 rounded-full"
            :class="categoryClass(message.category)"
          >
            <i
              class="mdi text-[1.25rem]"
              :class="categoryIcon(message.category)"
              aria-hidden="true"
            />
          </span>
          <div class="m3-list-item__content gap-1 pt-0.5">
            <p
              class="text-body-medium break-words"
              :class="message.read ? 'text-on-surface-variant' : 'font-medium text-on-surface'"
            >
              {{ message.message }}
            </p>
            <p
              v-if="message.creationDate"
              class="text-body-small text-on-surface-variant"
            >
              {{ d(new Date(message.creationDate), 'long') }}
            </p>
            <a
              v-if="message.link"
              class="m3-btn m3-btn--text m3-btn--sm -ml-3 self-start"
              :href="message.link"
              target="_blank"
              rel="noopener"
            >
              <span>{{ t('user-messages.link') }}</span>
              <i
                class="mdi mdi-open-in-new"
                aria-hidden="true"
              />
            </a>
          </div>
          <div class="m3-list-item__trailing mt-2">
            <button
              v-tooltip="message.read ? t('user-messages.mark_unread') : t('user-messages.mark_read')"
              type="button"
              class="m3-icon-btn"
              :aria-label="message.read ? t('user-messages.mark_unread') : t('user-messages.mark_read')"
              @click="updateMessage(message, !message.read)"
            >
              <i
                class="mdi"
                :class="message.read ? 'mdi-email-outline' : 'mdi-email-open-outline'"
                aria-hidden="true"
              />
            </button>
          </div>
        </li>
      </ul>
      <m-empty-state
        v-else-if="!getMessagesIsLoading"
        icon="inbox-outline"
        :title="t('user-messages.no_messages')"
      />
    </section>
    <o-pagination
      v-if="messages.length > 0 && pageCount > 1"
      :current="pageAsNumber"
      :total="total"
      position="centered"
      :per-page="perPage"
      @change="updatePage"
    />
  </div>
  <o-loading
    v-model:active="getPageIsLoading"
    :full-page="true"
    :cancelable="true"
  />
</template>
