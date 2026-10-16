/**
 * Ratings are stored out of 10 (imports and the API keep working unchanged) and
 * shown as 5 stars with half stars, so every stored value maps to a star count.
 */
export const MAX_STARS = 5

export function toStars(rating: number | null | undefined): number | null {
  return rating == null ? null : rating / 2
}

export function fromStars(stars: number | null | undefined): number | null {
  return stars == null ? null : stars * 2
}

/** Star count with at most one decimal, ie "4", "3.5", "3.8" */
export function formatStars(rating: number | null | undefined, locale?: string): string {
  const stars = toStars(rating)
  if (stars == null) {
    return ''
  }
  try {
    return new Intl.NumberFormat(locale, { maximumFractionDigits: 1 }).format(stars)
  } catch {
    return String(Math.round(stars * 10) / 10)
  }
}
