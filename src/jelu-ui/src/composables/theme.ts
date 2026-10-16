import { usePreferredDark } from '@vueuse/core'
import { computed, reactive, watch } from 'vue'
import {
  buildThemeCss,
  DEFAULT_THEME,
  sanitizeSettings,
  THEME_CSS_STORAGE_KEY,
  THEME_STORAGE_KEY,
  ThemeSettings,
} from '../theme/m3Theme'

function readStoredSettings(): ThemeSettings {
  try {
    return sanitizeSettings(JSON.parse(localStorage.getItem(THEME_STORAGE_KEY) ?? 'null'))
  } catch {
    return { ...DEFAULT_THEME }
  }
}

// one theme for the whole app, shared by every caller of useTheme()
const settings = reactive<ThemeSettings>(readStoredSettings())
let initialized = false

function updateThemeColorMeta() {
  const color = getComputedStyle(document.documentElement)
    .getPropertyValue('--md-sys-color-surface')
    .trim()
  if (color.length === 0) {
    return
  }
  let meta = document.querySelector('meta[name="theme-color"]')
  if (meta == null) {
    meta = document.createElement('meta')
    meta.setAttribute('name', 'theme-color')
    document.head.appendChild(meta)
  }
  meta.setAttribute('content', color)
}

function applyTheme(current: ThemeSettings) {
  const root = document.documentElement
  if (current.mode === 'system') {
    root.removeAttribute('data-theme')
  } else {
    root.setAttribute('data-theme', current.mode)
  }
  if (current.font === 'roboto') {
    root.removeAttribute('data-font')
  } else {
    root.setAttribute('data-font', current.font)
  }
  const css = buildThemeCss(current)
  let style = document.getElementById('jelu-theme')
  if (!(style instanceof HTMLStyleElement)) {
    style = document.createElement('style')
    style.id = 'jelu-theme'
    document.head.appendChild(style)
  }
  style.textContent = css
  try {
    // the cached css is injected by the inline script of index.html before the app boots
    localStorage.setItem(THEME_STORAGE_KEY, JSON.stringify(current))
    localStorage.setItem(THEME_CSS_STORAGE_KEY, css)
  } catch {
    // storage can be unavailable (private mode, quota), the theme still applies
  }
  updateThemeColorMeta()
}

/** Applies the stored theme and keeps the document in sync with later changes. */
export function initTheme() {
  if (initialized) {
    return
  }
  initialized = true
  applyTheme(settings)
  watch(settings, (current) => applyTheme(current), { deep: true })
  window.matchMedia('(prefers-color-scheme: dark)')
    .addEventListener('change', () => updateThemeColorMeta())
}

/** Reads a resolved M3 color role, ie `cssColor('primary')`, useful for canvas based charts. */
export function cssColor(role: string): string {
  return getComputedStyle(document.documentElement)
    .getPropertyValue(`--md-sys-color-${role}`)
    .trim()
}

export default function useTheme() {
  const prefersDark = usePreferredDark()

  const isDark = computed(() => settings.mode === 'dark'
    || (settings.mode === 'system' && prefersDark.value))

  const resetTheme = () => {
    Object.assign(settings, DEFAULT_THEME)
  }

  return {
    settings,
    isDark,
    resetTheme,
  }
}
