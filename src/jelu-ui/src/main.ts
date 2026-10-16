import { createApp, watch } from 'vue'
import App from './App.vue'
import FloatingVue from 'floating-vue'
import Oruga from '@oruga-ui/oruga-next'
import router from './router'
import store, { key } from './store'
import VueSplide from '@splidejs/vue-splide';
import VueMarkdownEditor from '@kangc/v-md-editor';
import VMdPreview from '@kangc/v-md-editor/lib/preview';

// vendor style sheets are imported by style.css, in a cascade layer
import './assets/style.css'

import githubTheme from '@kangc/v-md-editor/lib/theme/github.js';
VueMarkdownEditor.use(githubTheme)
import enUS from '@kangc/v-md-editor/lib/lang/en-US';
VueMarkdownEditor.lang.use('en-US', enUS);
VMdPreview.use(githubTheme);

import { initTheme } from './composables/theme'

/*
 * All i18n resources specified in the plugin `include` option can be loaded
 * at once using the import syntax
 */
import { createI18n } from 'vue-i18n'
import messages from '@intlify/unplugin-vue-i18n/messages'
import { datetimeFormats } from './datetimeFormat'
import { usePreferredLanguages } from '@vueuse/core'
import { useLocalStorage } from '@vueuse/core'

initTheme()

const languages = usePreferredLanguages()
console.log("languages : ")
console.log(languages)
let preferredLanguage = 'en'
if (languages.value && languages.value.length > 0) {
  const candidate = languages.value[0]
  if (candidate.length > 2) {
    preferredLanguage = candidate.slice(0,2)
  } else {
    preferredLanguage = candidate
  }
}
console.log(`favourite language fetched from browser is : ${preferredLanguage}`)
const storedLanguage = useLocalStorage("jelu_language", preferredLanguage)
console.log(`favourite language fetched from storage is : ${storedLanguage.value}`)

// Material 3 writes every label in sentence case, translations are not consistent about it
const toSentenceCase = (text: string, locale: string): string => {
  const first = text.charAt(0)
  let upper: string
  try {
    upper = first.toLocaleUpperCase(locale)
  } catch {
    upper = first.toUpperCase()
  }
  return upper === first ? text : upper + text.slice(1)
}

let currentLocale: string = storedLanguage.value

  const i18n = createI18n({
    legacy: false,
    locale: storedLanguage.value,
    fallbackLocale: 'en',
    messages,
    datetimeFormats: datetimeFormats,
    postTranslation: (translated) => typeof translated === 'string'
      ? toSentenceCase(translated, currentLocale)
      : translated,
  })

watch(i18n.global.locale, (newLocale) => {
  currentLocale = newLocale
})

createApp(App)
    .use(i18n)
    .use(router)
    .use(store, key)
    .use(FloatingVue, {
        themes: {
            tooltip: {
                delay: { show: 300, hide: 0 },
            },
        },
    })
    .use(Oruga, {
        iconPack: 'mdi',
        notification: {
            position: 'bottom',
            closable: false,
        },
        pagination: {
            iconPrev: 'chevron-left',
            iconNext: 'chevron-right',
            rounded: true,
            // Oruga applies both the deprecated `order` and `position` classes.
            order: 'centered',
            position: 'centered',
        },
        modal: {
            closeIcon: 'close',
        },
    })
    .use(VueSplide)
    .use(VueMarkdownEditor)
    .use(VMdPreview)
    .mount('#app')
