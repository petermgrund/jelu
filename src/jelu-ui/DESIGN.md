# Jelu UI design system

The web UI follows [Material Design 3](https://m3.material.io). This page is the short version of
the rules every component follows, so new screens stay consistent with the rest of the app.

## Foundations

- **Color**: every color is an M3 color role (`--md-sys-color-*`), generated at runtime from a seed
  color by `src/theme/m3Theme.ts` (light, dark and system modes, contrast levels, color styles). The
  user picks them in *Settings > Appearance*. Use the roles through Tailwind utilities:
  `bg-surface-container`, `text-on-surface-variant`, `bg-primary-container`, `border-outline-variant`...
  The default Tailwind palette is disabled on purpose.
- **Typography**: Roboto and the M3 type scale only: `text-display-*`, `text-headline-*`,
  `text-title-*`, `text-body-*`, `text-label-*` (large, medium, small). Display, headline and title
  large use the brand typeface, which can be switched to the typewriter or Cormorant fonts in the
  settings. Labels are written in sentence case: every translation gets its first letter upper cased,
  so never use `capitalize` or `uppercase`.
- **Shape**: `rounded-xs` 4px, `rounded-sm` 8px, `rounded-md` 12px, `rounded-lg` 16px, `rounded-xl` 28px,
  `rounded-full`.
- **Elevation**: `shadow-elevation-1` to `shadow-elevation-5`.
- **Breakpoints** are the M3 window size classes: `sm` 600px (medium), `md` 840px (expanded),
  `lg` 1200px (large), `xl` 1600px (extra large).
- **Icons**: [Material Design Icons](https://pictogrammers.com/library/mdi/) only,
  `<i class="mdi mdi-name" aria-hidden="true" />`, outlined variants for neutral icons.

## Components

Plain CSS classes in `src/assets/m3/components.css`:

| Need | Use |
| --- | --- |
| Buttons | `m3-btn` + `--filled` (main action), `--tonal`, `--outlined`, `--text`, `--elevated`, `--error`, `--sm`, `--block` |
| Icon buttons | `m3-icon-btn` + `--filled`, `--tonal`, `--outlined`, `--sm`, `is-selected` for toggles |
| Floating action button | `m3-fab` + `--extended`, `--small`, `--secondary`, `--tertiary`, `--surface` |
| Chips | `m3-chip` + `--assist`, `--elevated`, `--input`, `is-selected`, inside `m3-chip-set` |
| Status labels | `m3-status` + `--primary`, `--secondary`, `--tertiary`, `--error`, `--outlined` |
| Cards | `m3-card` + `--elevated`, `--filled`, `--outlined`, `--interactive`, `m3-card__content`, `m3-card__actions` |
| Lists | `m3-list`, `m3-list-item` (`--two-line`, `--three-line`) with `__leading`, `__content`, `__headline`, `__supporting`, `__trailing` |
| Checkbox, radio, switch | native inputs with `m3-checkbox`, `m3-radio`, `m3-switch`, wrapped in `label.m3-choice` |
| Progress | `progress.m3-linear-progress`, `m3-spinner` |
| Layout | `m3-page`, `m3-section`, `m3-section__title`, `m3-meta` (key/value), `jl-book-grid`, `m3-divider` |

Vue components in `src/components/m3/`: `MTextField`, `MSelect`, `MField` (outline around any other
control), `MSegmentedButton`, `MChipSet`, `MRadioGroup`, `MSlider`, `MStarRating`, `MTabs`, `MMenu`,
`MDialog`, `MModal`, `MSideSheet`, `MPageHeader`, `MEmptyState`, `MAvatar`.

App level building blocks: `BookCard`, `BookCover`, `ReadingStatus`, `BookGridSkeleton`,
`BulkSelectionBar`, `SortFilterBar`, `TriStateFilter`, `NavItem`.

## Patterns

- **Pages** start with `MPageHeader` (title, supporting text, actions) inside `m3-page`. Detail pages
  use `m3-page--medium`, forms `m3-page--narrow`.
- **Lists of books**: `jl-book-grid` of `BookCard`, `BookGridSkeleton` while loading, `MEmptyState`
  when empty, `SortFilterBar` (a side sheet) for sorting and filtering, `BulkSelectionBar` for bulk edits.
- **Dialogs**: components opened with `oruga.modal.open` render an `MDialog` (headline, content,
  actions: *Cancel* as a text button, the confirming action as a filled button, destructive actions as
  error text buttons on the left). Dialogs declared in a template use `MModal` + `MDialog`.
- **Forms**: `MTextField` / `MSelect`, Oruga pickers wrapped in `MField`, switches for on/off settings,
  segmented buttons for 2 to 5 exclusive choices, grouped in outlined cards with a title. Cards let
  autocomplete and date picker menus overflow them, and `MMenu` uses fixed positioning (opening above
  its trigger when there is no room below), so both work inside cards and dialogs.
- **Ids**: generate element ids with `uniqueId()` from `src/components/m3/uid.ts`. Vue's `useId()`
  restarts in every programmatic Oruga modal, so its ids can collide with the page underneath.
- **Accessibility**: icon-only buttons always have an `aria-label` and a `v-tooltip`, buttons have a
  `type`, focus is visible (secondary color ring), and motion respects `prefers-reduced-motion`.
