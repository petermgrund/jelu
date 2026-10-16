<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next";
import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useStore } from 'vuex';
import { User } from "../model/User";
import dataService from "../services/DataService";
import { key } from '../store';
import { ObjectUtils } from "../utils/ObjectUtils";
import MDialog from "./m3/MDialog.vue";
import MTextField from "./m3/MTextField.vue";
import { uniqueId } from "./m3/uid";

const { t } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const oruga = useOruga()
const store = useStore(key)

const props = defineProps<{
  currentUser: User,
}>()

const form = ref({'password' : '', 'password_confirm': ''})
console.log(props.currentUser)

const emit = defineEmits<{
  (e: 'close'): void
}>()

async function editUser() {
  console.log("edit user")
  console.log(form)
  if (props.currentUser.id != null) {
    try {
      const modified = await dataService.updateUser(props.currentUser.id, {"admin": undefined, "password": form.value.password})
      store.commit('user', modified)
      ObjectUtils.toast(oruga, "success", t('admin_user.user_updated', {name : props.currentUser.login}), 2500)
      emit('close')
    } catch (err: any) {
      console.log('failed to edit user')
      console.log(err)
    }
  }
}

const isValid = computed(() => {
  return form.value.password.length >=3 && form.value.password_confirm.length >=3 && form.value.password === form.value.password_confirm
})

// document wide id: this dialog is mounted as its own tree by oruga
const formId = uniqueId('user-form')

const passwordMismatch = computed(() => form.value.password_confirm.length > 0 && form.value.password !== form.value.password_confirm)
</script>

<template>
  <m-dialog
    :headline="t('admin_user.edit_user', {name : props.currentUser.login})"
    icon="account-edit-outline"
    size="sm"
    @close="emit('close')"
  >
    <form
      :id="formId"
      class="flex flex-col gap-4 pt-2"
      @submit.prevent="isValid && editUser()"
    >
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
    </form>
    <template #actions>
      <button
        type="button"
        class="m3-btn m3-btn--text"
        @click="emit('close')"
      >
        {{ t('labels.cancel') }}
      </button>
      <button
        type="submit"
        :form="formId"
        class="m3-btn m3-btn--filled"
        :disabled="! isValid"
      >
        {{ t('labels.submit') }}
      </button>
    </template>
  </m-dialog>
</template>
