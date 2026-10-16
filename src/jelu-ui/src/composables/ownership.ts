import { useI18n } from 'vue-i18n'
import { OwnershipStatus } from '../model/Ownership'

interface WithOwnership {
  owned?: boolean | null,
  borrowed?: boolean | null,
}

export const OWNERSHIP_STATUSES: Array<OwnershipStatus> = [OwnershipStatus.OWNED, OwnershipStatus.BORROWED, OwnershipStatus.NOT_OWNED]

/** Current ownership of a book, owning a copy wins over borrowing one */
export function ownershipOf(book: WithOwnership | null | undefined): OwnershipStatus {
  if (book?.owned) {
    return OwnershipStatus.OWNED
  }
  if (book?.borrowed) {
    return OwnershipStatus.BORROWED
  }
  return OwnershipStatus.NOT_OWNED
}

/** List query parameters selecting the books of an ownership state */
export function ownershipQuery(status: OwnershipStatus | null | undefined): { owned: boolean | null, borrowed: boolean | null } {
  switch (status) {
    case OwnershipStatus.OWNED:
      return { owned: true, borrowed: null }
    case OwnershipStatus.BORROWED:
      return { owned: false, borrowed: true }
    case OwnershipStatus.NOT_OWNED:
      return { owned: false, borrowed: false }
    default:
      return { owned: null, borrowed: null }
  }
}

export function isOwnershipStatus(value: unknown): value is OwnershipStatus {
  return OWNERSHIP_STATUSES.includes(value as OwnershipStatus)
}

export default function useOwnership() {
  const { t } = useI18n({
    inheritLocale: true,
    useScope: 'global'
  })

  /** Current state, ie "On my shelf" */
  const ownershipLabel = (status: OwnershipStatus | null | undefined): string => {
    switch (status) {
      case OwnershipStatus.OWNED:
        return t('ownership.on_my_shelf')
      case OwnershipStatus.BORROWED:
        return t('ownership.borrowed')
      case OwnershipStatus.NOT_OWNED:
        return t('ownership.not_on_my_shelf')
      default:
        return ''
    }
  }

  /** History entry, ie "Added to my shelf" */
  const ownershipChangeLabel = (status: OwnershipStatus): string => {
    switch (status) {
      case OwnershipStatus.OWNED:
        return t('ownership.added_to_shelf')
      case OwnershipStatus.BORROWED:
        return t('ownership.borrowed')
      default:
        return t('ownership.no_longer_have')
    }
  }

  /** mdi icon class */
  const ownershipIcon = (status: OwnershipStatus | null | undefined): string => {
    switch (status) {
      case OwnershipStatus.OWNED:
        return 'mdi-bookshelf'
      case OwnershipStatus.BORROWED:
        return 'mdi-hand-extended-outline'
      default:
        return 'mdi-book-off-outline'
    }
  }

  /** tonal container behind an ownership icon */
  const ownershipContainerClass = (status: OwnershipStatus | null | undefined): string => {
    switch (status) {
      case OwnershipStatus.OWNED:
        return 'bg-primary-container text-on-primary-container'
      case OwnershipStatus.BORROWED:
        return 'bg-secondary-container text-on-secondary-container'
      default:
        return 'bg-surface-container-highest text-on-surface-variant'
    }
  }

  return {
    ownershipLabel,
    ownershipChangeLabel,
    ownershipIcon,
    ownershipContainerClass,
  }
}
