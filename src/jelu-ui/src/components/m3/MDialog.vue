<script setup lang="ts">
import { computed, useId } from 'vue'
import { useI18n } from 'vue-i18n'

/**
 * M3 dialog surface: headline, scrollable content and actions.
 * Used as the root of every component opened with oruga.modal.open
 * and inside MModal for dialogs declared in templates.
 */
const props = withDefaults(defineProps<{
  headline?: string,
  icon?: string,
  size?: 'sm' | 'md' | 'lg' | 'xl',
  fullscreen?: boolean,
  closable?: boolean,
}>(), {
  size: 'md',
  closable: true,
})

const emit = defineEmits<{
  (e: 'close'): void,
}>()

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const headlineId = useId()

const widths: Record<string, string> = {
  sm: '22.5rem',
  md: '35rem',
  lg: '45rem',
  xl: '60rem',
}

const style = computed(() => ({ '--m3-dialog-width': widths[props.size] }))
</script>

<template>
  <section
    class="m3-dialog relative"
    :class="{ 'm3-dialog--fullscreen': props.fullscreen }"
    :style="style"
    role="dialog"
    aria-modal="true"
    :aria-labelledby="props.headline || $slots.headline ? headlineId : undefined"
  >
    <button
      v-if="props.closable"
      v-tooltip="t('labels.close')"
      type="button"
      class="m3-icon-btn absolute top-3 right-3 z-10"
      :aria-label="t('labels.close')"
      @click="emit('close')"
    >
      <i
        class="mdi mdi-close"
        aria-hidden="true"
      />
    </button>
    <header
      v-if="props.headline || $slots.headline || props.icon"
      class="m3-dialog__header"
      :class="{ 'm3-dialog__header--icon': props.icon, 'pr-16': props.closable && !props.icon }"
    >
      <i
        v-if="props.icon"
        class="m3-dialog__icon mdi"
        :class="`mdi-${props.icon}`"
        aria-hidden="true"
      />
      <h2
        :id="headlineId"
        class="m3-dialog__headline"
      >
        <slot name="headline">
          {{ props.headline }}
        </slot>
      </h2>
    </header>
    <div class="m3-dialog__content">
      <slot />
    </div>
    <footer
      v-if="$slots.actions"
      class="m3-dialog__actions"
    >
      <slot name="actions" />
    </footer>
  </section>
</template>
