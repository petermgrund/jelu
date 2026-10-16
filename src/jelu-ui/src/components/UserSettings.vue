<script setup lang="ts">
import { useLocalStorage, useTitle } from '@vueuse/core'
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import useTheme from '../composables/theme'
import { DisplayFont, schemeColors, SEED_PRESETS, THEME_VARIANTS, ThemeContrast, ThemeMode } from '../theme/m3Theme'
import MSegmentedButton from './m3/MSegmentedButton.vue'
import MSelect from './m3/MSelect.vue'
import MTextField from './m3/MTextField.vue'

useTitle('Jelu | User settings')

const { t, locale, availableLocales } = useI18n({
      inheritLocale: true,
      useScope: 'global'
    })

const { settings, isDark, resetTheme } = useTheme()

const modeOptions = computed(() => [
  { value: 'system' as ThemeMode, label: t('settings.mode_system'), icon: 'theme-light-dark' },
  { value: 'light' as ThemeMode, label: t('settings.mode_light'), icon: 'white-balance-sunny' },
  { value: 'dark' as ThemeMode, label: t('settings.mode_dark'), icon: 'weather-night' },
])

const contrastOptions = computed(() => [
  { value: 'standard' as ThemeContrast, label: t('settings.contrast_standard') },
  { value: 'medium' as ThemeContrast, label: t('settings.contrast_medium') },
  { value: 'high' as ThemeContrast, label: t('settings.contrast_high') },
])

const variantOptions = computed(() => THEME_VARIANTS.map(variant => ({
  value: variant,
  label: t(`settings.variant_${variant}`),
})))

const fontOptions = computed(() => [
  { value: 'roboto' as DisplayFont, label: t('settings.font_roboto') },
  { value: 'typewriter' as DisplayFont, label: t('settings.font_typewriter') },
  { value: 'cormorant' as DisplayFont, label: t('settings.font_cormorant') },
])

// three tones of each preset, like the Android wallpaper color picker
const swatches = computed(() => SEED_PRESETS.map(preset => {
  const colors = schemeColors({ ...settings, seed: preset.hex }, isDark.value)
  return {
    ...preset,
    primary: colors['primary'],
    secondary: colors['secondary-container'],
    tertiary: colors['tertiary-container'],
  }
}))

const isCustomSeed = computed(() => !SEED_PRESETS.some(preset => preset.hex === settings.seed))

const localeOptions = computed(() => [...availableLocales].sort().map(code => {
  let name = code
  try {
    name = new Intl.DisplayNames([code], { type: 'language' }).of(code) ?? code
  } catch {
    // unknown to Intl, keep the code
  }
  return { value: code, label: `${name.charAt(0).toLocaleUpperCase(code)}${name.slice(1)} (${code})` }
}))

watch(() => locale.value, (newValue, oldValue) => {
  const storedLanguage = useLocalStorage("jelu_language", oldValue)
  storedLanguage.value = newValue
})

const currency = ref("EUR")
const saved = localStorage.getItem("JL_CURRENCY")
if (saved != null) {
  currency.value = saved
}

watch(() => currency.value, () => {
  if (currency.value.length === 3) {
    localStorage.setItem("JL_CURRENCY", currency.value)
  }
})
</script>

<template>
  <div class="flex max-w-3xl flex-col gap-6">
    <section class="m3-card m3-card--outlined">
      <div class="m3-card__content gap-6 p-6">
        <div class="flex items-center gap-3">
          <i
            class="mdi mdi-palette-outline text-[1.5rem] text-on-surface-variant"
            aria-hidden="true"
          />
          <h2 class="flex-1 text-title-large">
            {{ t('settings.appearance') }}
          </h2>
          <button
            type="button"
            class="m3-btn m3-btn--text"
            @click="resetTheme"
          >
            <i
              class="mdi mdi-restore"
              aria-hidden="true"
            />
            <span>{{ t('settings.reset_theme') }}</span>
          </button>
        </div>

        <div class="flex flex-col gap-3">
          <h3 class="text-title-small text-on-surface-variant">
            {{ t('settings.theme_mode') }}
          </h3>
          <m-segmented-button
            v-model="settings.mode"
            :options="modeOptions"
            :aria-label="t('settings.theme_mode')"
            class="max-w-md"
            block
          />
        </div>

        <div class="flex flex-col gap-3">
          <h3 class="text-title-small text-on-surface-variant">
            {{ t('settings.theme_color') }}
          </h3>
          <div
            class="flex flex-wrap gap-3"
            role="radiogroup"
            :aria-label="t('settings.theme_color')"
          >
            <button
              v-for="swatch in swatches"
              :key="swatch.id"
              v-tooltip="t(`settings.preset_${swatch.id}`)"
              type="button"
              role="radio"
              :aria-checked="settings.seed === swatch.hex"
              :aria-label="t(`settings.preset_${swatch.id}`)"
              class="jl-swatch"
              :class="{ 'is-selected': settings.seed === swatch.hex }"
              @click="settings.seed = swatch.hex"
            >
              <span
                class="jl-swatch__colors"
                :style="{ '--swatch-primary': swatch.primary, '--swatch-secondary': swatch.secondary, '--swatch-tertiary': swatch.tertiary }"
              />
              <i
                v-if="settings.seed === swatch.hex"
                class="mdi mdi-check jl-swatch__check"
                aria-hidden="true"
              />
            </button>
            <label
              v-tooltip="t('settings.custom_color')"
              class="jl-swatch jl-swatch--custom"
              :class="{ 'is-selected': isCustomSeed }"
              :style="isCustomSeed ? { backgroundColor: settings.seed } : undefined"
            >
              <input
                v-model="settings.seed"
                type="color"
                class="sr-only"
                :aria-label="t('settings.custom_color')"
              >
              <i
                class="mdi jl-swatch__check"
                :class="isCustomSeed ? 'mdi-check' : 'mdi-palette'"
                aria-hidden="true"
              />
            </label>
          </div>
        </div>

        <div class="grid gap-4 sm:grid-cols-2">
          <m-select
            v-model="settings.variant"
            :label="t('settings.color_style')"
            :options="variantOptions"
          />
          <m-select
            v-model="settings.font"
            :label="t('settings.display_font')"
            :options="fontOptions"
            :supporting-text="t('settings.choose_font')"
          />
        </div>

        <div class="flex flex-col gap-3">
          <h3 class="text-title-small text-on-surface-variant">
            {{ t('settings.contrast') }}
          </h3>
          <m-segmented-button
            v-model="settings.contrast"
            :options="contrastOptions"
            :aria-label="t('settings.contrast')"
            class="max-w-md"
            block
          />
        </div>

        <div class="flex flex-col gap-3 rounded-lg bg-surface-container p-4">
          <p class="text-headline-small text-on-surface">
            The Left Hand of Darkness
          </p>
          <p class="text-body-medium text-on-surface-variant">
            Ursula K. Le Guin
          </p>
          <div class="flex flex-wrap gap-2">
            <span class="m3-status m3-status--primary"><i
              class="mdi mdi-book-open-page-variant-outline"
              aria-hidden="true"
            /><span>{{ t('reading_events.reading') }}</span></span>
            <span class="m3-status m3-status--tertiary"><i
              class="mdi mdi-check-circle-outline"
              aria-hidden="true"
            /><span>{{ t('reading_events.finished') }}</span></span>
            <span class="m3-status m3-status--error"><i
              class="mdi mdi-close-circle-outline"
              aria-hidden="true"
            /><span>{{ t('reading_events.dropped') }}</span></span>
          </div>
          <div class="flex flex-wrap gap-2 pt-2">
            <span class="m3-btn m3-btn--filled pointer-events-none">{{ t('labels.save') }}</span>
            <span class="m3-btn m3-btn--tonal pointer-events-none">{{ t('labels.edit') }}</span>
            <span class="m3-btn m3-btn--outlined pointer-events-none">{{ t('labels.cancel') }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="m3-card m3-card--outlined">
      <div class="m3-card__content gap-6 p-6">
        <div class="flex items-center gap-3">
          <i
            class="mdi mdi-translate text-[1.5rem] text-on-surface-variant"
            aria-hidden="true"
          />
          <h2 class="text-title-large">
            {{ t('settings.language_region') }}
          </h2>
        </div>
        <div class="grid gap-4 sm:grid-cols-2">
          <m-select
            v-model="locale"
            :label="t('settings.pick_language')"
            :options="localeOptions"
            leading-icon="web"
          />
          <m-text-field
            v-model="currency"
            :label="t('settings.choose_currency')"
            leading-icon="cash-multiple"
            minlength="3"
            maxlength="3"
            :error="currency.length != 3 ? t('settings.currency_description') : false"
            :supporting-text="t('settings.currency_description')"
          />
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.jl-swatch {
  position: relative;
  display: inline-grid;
  place-items: center;
  width: 3.5rem;
  height: 3.5rem;
  padding: 0.25rem;
  border: 0;
  border-radius: 1rem;
  background-color: var(--md-sys-color-surface-container-high);
  color: var(--md-sys-color-on-surface);
  cursor: pointer;
  transition: border-radius var(--md-sys-motion-duration-short4) var(--md-sys-motion-easing-standard);
}

.jl-swatch.is-selected {
  border-radius: 9999px;
  outline: 2px solid var(--md-sys-color-on-surface);
  outline-offset: 2px;
}

.jl-swatch__colors {
  width: 100%;
  height: 100%;
  border-radius: 9999px;
  background:
    linear-gradient(to bottom, var(--swatch-primary) 0 50%, transparent 50%),
    linear-gradient(to right, var(--swatch-secondary) 0 50%, var(--swatch-tertiary) 50%);
}

.jl-swatch__check {
  position: absolute;
  display: grid;
  place-items: center;
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 9999px;
  background-color: var(--md-sys-color-surface-container-highest);
  color: var(--md-sys-color-on-surface);
  font-size: 1rem;
  line-height: 1;
}

.jl-swatch--custom {
  background-color: var(--md-sys-color-surface-container-highest);
}

.jl-swatch--custom:focus-within {
  outline: 3px solid var(--md-sys-color-secondary);
  outline-offset: 2px;
}
</style>
