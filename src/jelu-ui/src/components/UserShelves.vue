<script setup lang="ts">
import { useTitle } from '@vueuse/core'
import UserShelvesList from './UserShelvesList.vue'


useTitle('Jelu | User page')


</script>

<template>
  <user-shelves-list
    :allow-delete="true"
    :allow-create="true"
  />
</template>
