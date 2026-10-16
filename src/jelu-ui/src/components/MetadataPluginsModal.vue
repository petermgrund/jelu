<script setup lang="ts">
import { Ref, ref } from "vue";
import { useI18n } from 'vue-i18n';
import draggable from "vuedraggable";
import { useStore } from 'vuex';
import { PluginInfo, PluginInfoOrder } from "../model/PluginInfo";
import { key } from '../store';
import MDialog from "./m3/MDialog.vue";

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

const store = useStore(key)

const emit = defineEmits<{
  (e: 'close'): void,
  (e: 'plugins', plugins: Array<PluginInfo>): void
}>()

const plugins: Ref<Array<PluginInfoOrder>> = ref([])
for (const elem of store.getters.getSettings.metadataPlugins) {
  console.log('elem' + elem)
  plugins.value.push({name: elem.name, order: elem.order, enabled: true})

}
plugins.value.sort((a, b) => b.order - a.order)
const progress: Ref<boolean> = ref(false)

 const submit =async () => {
  console.log("plugins")
  console.log(plugins.value)
  let finalPlugins = plugins.value.filter(p => p.enabled).map((pluginInfo, idx) => newPlugin(pluginInfo, idx, plugins.value.length))
  console.log("new plugins")
  console.log(finalPlugins)
  emit('plugins', finalPlugins)
  emit('close')
}

const newPlugin = (plugin: PluginInfo, idx: number, size: number): PluginInfo => {
  return {name: plugin.name, order: size - idx}
}

const dismiss =async () => {
  emit('close')
}

function getKey(item: PluginInfo) {
      return item.order;
}

/** move item to the bottom if disabled */
const checkState = (element: PluginInfoOrder) => {
  console.log("state")
  console.log(element)
  if (element.enabled === false) {
    plugins.value = [...plugins.value.filter(a => a !== element), element]
  } else {
    plugins.value = [element, ...plugins.value.filter(a => a !== element)]
  }
}

/** prevent disabled items to be dragged */
function checkMove(evt: any){
    return (evt.draggedContext.element.enabled);
}
</script>

<template>
  <m-dialog
    :headline="t('metadata.reorder_plugins')"
    size="sm"
    @close="emit('close')"
  >
    <div class="flex flex-col gap-4">
      <p class="text-body-medium">
        {{ t('metadata.description') }}
      </p>
      <draggable
        v-model="plugins"
        class="m3-card m3-card--outlined m3-list"
        :item-key="getKey"
        :move="checkMove"
        ghost-class="bg-secondary-container"
        tag="ul"
      >
        <template #item="{ element }">
          <li
            class="m3-list-item m3-list-item--two-line gap-3 pr-4 pl-2"
            :class="element.enabled ? 'cursor-move' : 'cursor-not-allowed'"
          >
            <span
              class="m3-list-item__leading"
              :class="{ 'opacity-38': !element.enabled }"
            >
              <i
                class="mdi mdi-drag"
                aria-hidden="true"
              />
            </span>
            <span
              class="m3-list-item__content"
              :class="{ 'opacity-38': !element.enabled }"
            >
              <span class="m3-list-item__headline">{{ element.name }}</span>
              <span class="m3-list-item__supporting">{{ t('metadata.default_priority') }}: {{ element.order }}</span>
            </span>
            <span class="m3-list-item__trailing">
              <input
                v-model="element.enabled"
                type="checkbox"
                role="switch"
                class="m3-switch"
                :aria-label="element.name"
                @change="checkState(element)"
              >
            </span>
          </li>
        </template>
      </draggable>
      <p
        class="flex items-center gap-3 rounded-md bg-tertiary-container p-4 text-body-medium text-on-tertiary-container"
        role="note"
      >
        <i
          class="mdi mdi-information-outline text-[1.25rem]"
          aria-hidden="true"
        />
        <span>{{ t('metadata.note') }}</span>
      </p>
    </div>
    <template #actions>
      <button
        type="button"
        class="m3-btn m3-btn--text"
        :disabled="progress"
        @click="dismiss"
      >
        {{ t('labels.cancel') }}
      </button>
      <button
        type="button"
        class="m3-btn m3-btn--filled"
        :disabled="progress"
        @click="submit"
      >
        <span
          v-if="progress"
          class="m3-spinner"
        />
        <span>{{ t('labels.apply') }}</span>
      </button>
    </template>
  </m-dialog>
</template>
