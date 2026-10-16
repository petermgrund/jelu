<script setup lang="ts">
import { MTab } from './types'

/**
 * M3 tabs. Tabs with a `to` location are router links (active state comes
 * from the router), the others are buttons bound to v-model.
 */
const props = withDefaults(defineProps<{
  tabs: Array<MTab>,
  variant?: 'primary' | 'secondary',
  scrollable?: boolean,
  inline?: boolean,
  ariaLabel?: string,
}>(), {
  variant: 'primary',
  ariaLabel: undefined,
})

const model = defineModel<string | number | null | undefined>()
</script>

<template>
  <nav
    class="m3-tabs"
    :class="{
      'm3-tabs--secondary': props.variant === 'secondary',
      'm3-tabs--scrollable': props.scrollable,
      'm3-tabs--inline': props.inline,
    }"
    :role="props.tabs.some(tab => tab.to != null) ? undefined : 'tablist'"
    :aria-label="props.ariaLabel"
  >
    <template
      v-for="tab in props.tabs"
      :key="tab.label"
    >
      <router-link
        v-if="tab.to"
        :to="tab.to"
        class="m3-tab"
        active-class=""
        exact-active-class="is-active"
      >
        <i
          v-if="tab.icon"
          class="mdi"
          :class="`mdi-${tab.icon}`"
          aria-hidden="true"
        />
        <span>{{ tab.label }}</span>
      </router-link>
      <button
        v-else
        type="button"
        role="tab"
        class="m3-tab"
        :aria-selected="model === tab.value"
        @click="model = tab.value"
      >
        <i
          v-if="tab.icon"
          class="mdi"
          :class="`mdi-${tab.icon}`"
          aria-hidden="true"
        />
        <span>{{ tab.label }}</span>
      </button>
    </template>
  </nav>
</template>
