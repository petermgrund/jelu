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

/**
 * Star count with at most one decimal, ie "4", "3.5", "3.8". A single rating is
 * shown with `toHalf` so the number matches the half stars drawn for it.
 */
export function formatStars(rating: number | null | undefined, locale?: string, toHalf = false): string {
  let stars = toStars(rating)
  if (stars == null) {
    return ''
  }
  if (toHalf) {
    stars = Math.round(stars * 2) / 2
  }
  try {
    return new Intl.NumberFormat(locale, { maximumFractionDigits: 1 }).format(stars)
  } catch {
    return String(Math.round(stars * 10) / 10)
  }
}
