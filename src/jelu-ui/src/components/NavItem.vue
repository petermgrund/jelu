<script setup lang="ts">
import { RouteLocationRaw } from 'vue-router'

/**
 * Navigation destination for the rail, the drawer and the navigation bar.
 * `row` lays the icon and the label on one line (expanded rail, drawer).
 * Without `to` it renders a button and emits click.
 */
const props = defineProps<{
  label: string,
  icon: string,
  activeIcon?: string,
  to?: RouteLocationRaw,
  row?: boolean,
}>()

const emit = defineEmits<{
  (e: 'click'): void,
}>()
</script>

<template>
  <router-link
    v-if="props.to"
    v-slot="{ href, navigate, isActive, isExactActive }"
    :to="props.to"
    custom
  >
    <a
      :href="href"
      class="m3-nav-item"
      :class="{ 'm3-nav-item--row': props.row, 'is-active': isActive }"
      :aria-current="isExactActive ? 'page' : undefined"
      @click="(event) => { navigate(event); emit('click') }"
    >
      <span class="m3-nav-item__indicator">
        <i
          class="mdi"
          :class="`mdi-${isActive && props.activeIcon ? props.activeIcon : props.icon}`"
          aria-hidden="true"
        />
        <span
          v-if="props.row"
          class="m3-nav-item__label"
        >{{ props.label }}</span>
      </span>
      <span
        v-if="!props.row"
        class="m3-nav-item__label"
      >{{ props.label }}</span>
    </a>
  </router-link>
  <button
    v-else
    type="button"
    class="m3-nav-item"
    :class="{ 'm3-nav-item--row': props.row }"
    @click="emit('click')"
  >
    <span class="m3-nav-item__indicator">
      <i
        class="mdi"
        :class="`mdi-${props.icon}`"
        aria-hidden="true"
      />
      <span
        v-if="props.row"
        class="m3-nav-item__label"
      >{{ props.label }}</span>
    </span>
    <span
      v-if="!props.row"
      class="m3-nav-item__label"
    >{{ props.label }}</span>
  </button>
</template>
