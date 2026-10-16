<script setup lang="ts">
import { onClickOutside, onKeyStroke } from '@vueuse/core'
import { ref, useTemplateRef } from 'vue'

/**
 * M3 menu anchored to its trigger.
 *
 * <m-menu>
 *   <template #trigger="{ toggle, open }">
 *     <button class="m3-icon-btn" :aria-expanded="open" @click="toggle">...</button>
 *   </template>
 *   <button class="m3-menu-item" @click="doSomething">...</button>
 * </m-menu>
 *
 * Clicking an item closes the menu.
 */
const props = withDefaults(defineProps<{
  align?: 'start' | 'end',
  placement?: 'bottom' | 'top',
}>(), {
  align: 'end',
  placement: 'bottom',
})

const open = ref(false)
const root = useTemplateRef<HTMLElement>('root')

const toggle = () => {
  open.value = !open.value
}

const close = () => {
  open.value = false
}

onClickOutside(root, close)
onKeyStroke('Escape', () => {
  if (open.value) {
    close()
  }
})

defineExpose({ close, toggle })
</script>

<template>
  <div
    ref="root"
    class="relative inline-flex"
  >
    <slot
      name="trigger"
      :toggle="toggle"
      :open="open"
      :close="close"
    />
    <Transition name="m3-menu">
      <div
        v-if="open"
        class="m3-menu"
        role="menu"
        :class="[
          props.align === 'end' ? 'right-0' : 'left-0',
          props.placement === 'bottom' ? 'top-full mt-1' : 'bottom-full mb-1',
        ]"
        @click="close"
      >
        <slot :close="close" />
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.m3-menu-enter-active {
  transition: opacity var(--md-sys-motion-duration-short4) linear,
    transform var(--md-sys-motion-duration-medium2) var(--md-sys-motion-easing-emphasized-decelerate);
}

.m3-menu-leave-active {
  transition: opacity var(--md-sys-motion-duration-short2) linear;
}

.m3-menu-enter-from,
.m3-menu-leave-to {
  opacity: 0;
  transform: scaleY(0.8);
  transform-origin: top;
}
</style>
