import { RouteLocationRaw } from 'vue-router'

export interface MOption<V> {
  label: string,
  value: V,
  icon?: string,
  disabled?: boolean,
}

export interface MTab {
  label: string,
  value?: string | number,
  icon?: string,
  to?: RouteLocationRaw,
}
