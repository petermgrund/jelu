<script setup lang="ts">
import { onMounted, useTemplateRef, watch } from 'vue'

/**
 * Native <dialog> with the M3 scrim, for dialogs declared in a template.
 * Put an MDialog inside and bind v-model to open and close it.
 */
const open = defineModel<boolean>({ default: false })

const dialog = useTemplateRef<HTMLDialogElement>('dialog')

const sync = (value: boolean) => {
  const element = dialog.value
  if (element == null) {
    return
  }
  if (value && !element.open) {
    element.showModal()
  } else if (!value && element.open) {
    element.close()
  }
}

watch(open, sync, { flush: 'post' })
onMounted(() => sync(open.value))

// clicks on the ::backdrop target the dialog element itself
const onClick = (event: MouseEvent) => {
  if (event.target === dialog.value) {
    open.value = false
  }
}
</script>

<template>
  <dialog
    ref="dialog"
    class="m3-dialog-native"
    @click="onClick"
    @close="open = false"
  >
    <slot v-if="open" />
  </dialog>
</template>
