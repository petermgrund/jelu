import {
  argbFromHex,
  DynamicScheme,
  hexFromArgb,
  Hct,
  MaterialDynamicColors,
  SchemeExpressive,
  SchemeFidelity,
  SchemeMonochrome,
  SchemeNeutral,
  SchemeTonalSpot,
  SchemeVibrant,
} from '@material/material-color-utilities'

/**
 * Material 3 dynamic color for Jelu.
 *
 * A theme is fully described by a seed color, a color style (scheme variant),
 * a contrast level, a light/dark mode and a display typeface. From the seed we
 * generate every M3 color role for both light and dark schemes and expose them
 * as `--md-sys-color-*` custom properties. The light/dark switch itself is
 * driven by the `data-theme` attribute on <html> so that "system" mode keeps
 * following `prefers-color-scheme`.
 */

export type ThemeMode = 'system' | 'light' | 'dark'
export type ThemeVariant = 'tonal_spot' | 'neutral' | 'vibrant' | 'expressive' | 'fidelity' | 'monochrome'
export type ThemeContrast = 'standard' | 'medium' | 'high'
export type DisplayFont = 'roboto' | 'typewriter' | 'cormorant'

export interface ThemeSettings {
  mode: ThemeMode
  seed: string
  variant: ThemeVariant
  contrast: ThemeContrast
  font: DisplayFont
}

export const THEME_STORAGE_KEY = 'jelu_theme'
export const THEME_CSS_STORAGE_KEY = 'jelu_theme_css'

export const DEFAULT_SEED = '#8d795b'

export const DEFAULT_THEME: ThemeSettings = {
  mode: 'system',
  seed: DEFAULT_SEED,
  variant: 'tonal_spot',
  contrast: 'standard',
  font: 'roboto',
}

export const SEED_PRESETS: ReadonlyArray<{ id: string, hex: string }> = [
  { id: 'jelu', hex: DEFAULT_SEED },
  { id: 'baseline', hex: '#6750a4' },
  { id: 'forest', hex: '#4f6f52' },
  { id: 'ocean', hex: '#36618e' },
  { id: 'rose', hex: '#9a4058' },
  { id: 'slate', hex: '#5b6170' },
]

export const THEME_VARIANTS: ReadonlyArray<ThemeVariant> = ['tonal_spot', 'neutral', 'vibrant', 'expressive', 'fidelity', 'monochrome']

const CONTRAST_LEVELS: Record<ThemeContrast, number> = {
  standard: 0,
  medium: 0.5,
  high: 1,
}

const HEX_COLOR = /^#[0-9a-f]{6}$/i

export function isHexColor(value: unknown): value is string {
  return typeof value === 'string' && HEX_COLOR.test(value)
}

/** Merges stored (possibly partial or outdated) settings with the defaults. */
export function sanitizeSettings(raw: Partial<ThemeSettings> | null | undefined): ThemeSettings {
  const settings = { ...DEFAULT_THEME }
  if (raw == null || typeof raw !== 'object') {
    return settings
  }
  if (raw.mode === 'light' || raw.mode === 'dark' || raw.mode === 'system') {
    settings.mode = raw.mode
  }
  if (isHexColor(raw.seed)) {
    settings.seed = raw.seed.toLowerCase()
  }
  if (raw.variant != null && THEME_VARIANTS.includes(raw.variant)) {
    settings.variant = raw.variant
  }
  if (raw.contrast != null && raw.contrast in CONTRAST_LEVELS) {
    settings.contrast = raw.contrast
  }
  if (raw.font === 'roboto' || raw.font === 'typewriter' || raw.font === 'cormorant') {
    settings.font = raw.font
  }
  return settings
}

function buildScheme(settings: ThemeSettings, isDark: boolean): DynamicScheme {
  const source = Hct.fromInt(argbFromHex(settings.seed))
  const contrast = CONTRAST_LEVELS[settings.contrast]
  switch (settings.variant) {
    case 'neutral':
      return new SchemeNeutral(source, isDark, contrast)
    case 'vibrant':
      return new SchemeVibrant(source, isDark, contrast)
    case 'expressive':
      return new SchemeExpressive(source, isDark, contrast)
    case 'fidelity':
      return new SchemeFidelity(source, isDark, contrast)
    case 'monochrome':
      return new SchemeMonochrome(source, isDark, contrast)
    default:
      return new SchemeTonalSpot(source, isDark, contrast)
  }
}

const dynamicColors = new MaterialDynamicColors()

// these roles only exist in the 2025 spec, which we do not use
const SPEC_2025_ONLY = new Set(['primary_dim', 'secondary_dim', 'tertiary_dim', 'error_dim'])

export function schemeColors(settings: ThemeSettings, isDark: boolean): Record<string, string> {
  const scheme = buildScheme(settings, isDark)
  const colors: Record<string, string> = {}
  const roles = [...dynamicColors.allColors, dynamicColors.surfaceVariant()]
  for (const color of roles) {
    if (color == null || SPEC_2025_ONLY.has(color.name)) {
      continue
    }
    colors[color.name.replace(/_/g, '-')] = hexFromArgb(color.getArgb(scheme))
  }
  colors['shadow'] = '#000000'
  colors['scrim'] = '#000000'
  return colors
}

function declarations(colors: Record<string, string>): string {
  return Object.entries(colors)
    .map(([role, hex]) => `--md-sys-color-${role}:${hex};`)
    .join('')
}

/**
 * CSS for both schemes. The `html:root` selectors are intentionally more
 * specific than the static defaults in tokens.css so the generated theme wins
 * whatever the order of the style sheets in the document.
 */
export function buildThemeCss(settings: ThemeSettings): string {
  const light = declarations(schemeColors(settings, false))
  const dark = declarations(schemeColors(settings, true))
  return `html:root{${light}}`
    + `@media (prefers-color-scheme: dark){html:root:not([data-theme="light"]){${dark}}}`
    + `html:root[data-theme="dark"]{${dark}}`
}
