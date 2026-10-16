<script setup lang="ts">
import { onClickOutside, onKeyStroke, useEventListener } from '@vueuse/core'
import { ref, useTemplateRef, watch } from 'vue'

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
 * Clicking an item closes the menu. The menu uses fixed positioning so a
 * scrolling ancestor (a dialog, a card) cannot clip it, and it opens on the
 * other side of the trigger when there is not enough room.
 */
const props = withDefaults(defineProps<{
  align?: 'start' | 'end',
  placement?: 'bottom' | 'top',
  menuClass?: string,
}>(), {
  align: 'end',
  placement: 'bottom',
  menuClass: undefined,
})

const GAP = 4
const MARGIN = 8

const open = ref(false)
const root = useTemplateRef<HTMLElement>('root')
const menu = useTemplateRef<HTMLElement>('menu')

const toggle = () => {
  open.value = !open.value
}

const close = () => {
  open.value = false
}

const place = () => {
  const anchor = root.value
  const panel = menu.value
  if (!open.value || anchor == null || panel == null) {
    return
  }
  // A transformed ancestor moves the origin of fixed positioning, so measure
  // where (0, 0) actually lands before placing the menu.
  panel.style.top = '0px'
  panel.style.left = '0px'
  panel.style.maxHeight = ''
  panel.style.transformOrigin = 'top'
  const origin = panel.getBoundingClientRect()
  const trigger = anchor.getBoundingClientRect()
  const viewportWidth = document.documentElement.clientWidth
  const viewportHeight = document.documentElement.clientHeight
  const height = panel.offsetHeight
  const width = panel.offsetWidth
  const spaceBelow = viewportHeight - trigger.bottom - GAP - MARGIN
  const spaceAbove = trigger.top - GAP - MARGIN
  const above = props.placement === 'top'
    ? height <= spaceAbove || spaceAbove >= spaceBelow
    : height > spaceBelow && spaceAbove > spaceBelow
  const available = Math.max(above ? spaceAbove : spaceBelow, 0)
  const top = above ? trigger.top - GAP - Math.min(height, available) : trigger.bottom + GAP
  const preferredLeft = props.align === 'end' ? trigger.right - width : trigger.left
  const left = Math.max(Math.min(preferredLeft, viewportWidth - width - MARGIN), MARGIN)
  panel.style.maxHeight = `min(24rem, ${available}px)`
  panel.style.top = `${top - origin.top}px`
  panel.style.left = `${left - origin.left}px`
  panel.style.transformOrigin = above ? 'bottom' : 'top'
}

const onScroll = (event: Event) => {
  // Scrolling inside a long menu must not move it.
  if (menu.value != null && event.target instanceof Node && menu.value.contains(event.target)) {
    return
  }
  place()
}

watch(open, place, { flush: 'post' })
useEventListener(window, 'resize', place, { passive: true })
useEventListener(document, 'scroll', onScroll, { capture: true, passive: true })

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
        ref="menu"
        class="m3-menu fixed"
        role="menu"
        :class="props.menuClass"
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
}
</style>
