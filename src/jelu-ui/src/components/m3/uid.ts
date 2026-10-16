let counter = 0

/**
 * Document wide unique id. Vue's useId() restarts for every component tree
 * mounted on its own, which is how Oruga renders programmatic modals, so ids
 * of a dialog could collide with ids of the page underneath.
 */
export function uniqueId(prefix = 'm3'): string {
  counter += 1
  return `${prefix}-${counter}`
}
