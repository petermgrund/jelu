import { useI18n } from 'vue-i18n'
import { ReadingEventType } from '../model/ReadingEvent'

/**
 * A book has a single status: want to read, then reading, then read or stopped.
 * The server keeps it in two places, the want to read flag and the last reading
 * event, the flag is cleared as soon as a reading starts, finishes or stops.
 */
export enum BookStatus {
  WANT_TO_READ = 'WANT_TO_READ',
  READING = 'READING',
  READ = 'READ',
  STOPPED = 'STOPPED',
}

/** Books without any status, to filter lists */
export const NO_STATUS = 'NONE'

export type StatusFilter = BookStatus | typeof NO_STATUS

export const BOOK_STATUSES: Array<BookStatus> = [BookStatus.WANT_TO_READ, BookStatus.READING, BookStatus.READ, BookStatus.STOPPED]

interface WithStatus {
  toRead?: boolean | null,
  lastReadingEvent?: ReadingEventType | null,
}

export function bookStatus(book: WithStatus | null | undefined): BookStatus | null {
  if (book == null) {
    return null
  }
  // wanting to read a book again comes after its last reading
  if (book.toRead) {
    return BookStatus.WANT_TO_READ
  }
  switch (book.lastReadingEvent) {
    case ReadingEventType.CURRENTLY_READING:
      return BookStatus.READING
    case ReadingEventType.FINISHED:
      return BookStatus.READ
    case ReadingEventType.DROPPED:
      return BookStatus.STOPPED
    default:
      return null
  }
}

/** The reading event that moves a book to a status, want to read has none */
export function statusEventType(status: BookStatus): ReadingEventType | null {
  switch (status) {
    case BookStatus.READING:
      return ReadingEventType.CURRENTLY_READING
    case BookStatus.READ:
      return ReadingEventType.FINISHED
    case BookStatus.STOPPED:
      return ReadingEventType.DROPPED
    default:
      return null
  }
}

/** List query parameters selecting the books of a status */
export function statusQuery(status: StatusFilter | null | undefined): { eventTypes: Array<ReadingEventType> | null, toRead: boolean | null } {
  if (status == null) {
    return { eventTypes: null, toRead: null }
  }
  if (status === BookStatus.WANT_TO_READ) {
    return { eventTypes: null, toRead: true }
  }
  if (status === NO_STATUS) {
    return { eventTypes: [ReadingEventType.NONE], toRead: false }
  }
  return { eventTypes: [statusEventType(status) as ReadingEventType], toRead: false }
}

export function isStatusFilter(value: unknown): value is StatusFilter {
  return value === NO_STATUS || BOOK_STATUSES.includes(value as BookStatus)
}

export default function useBookStatus() {
  const { t } = useI18n({
    inheritLocale: true,
    useScope: 'global'
  })

  const statusLabel = (status: StatusFilter | null | undefined): string => {
    switch (status) {
      case BookStatus.WANT_TO_READ:
        return t('status.want_to_read')
      case BookStatus.READING:
        return t('status.reading')
      case BookStatus.READ:
        return t('status.read')
      case BookStatus.STOPPED:
        return t('status.stopped')
      case NO_STATUS:
        return t('status.none')
      default:
        return ''
    }
  }

  /** mdi icon class */
  const statusIcon = (status: StatusFilter | null | undefined): string => {
    switch (status) {
      case BookStatus.WANT_TO_READ:
        return 'mdi-bookmark-outline'
      case BookStatus.READING:
        return 'mdi-book-open-page-variant-outline'
      case BookStatus.READ:
        return 'mdi-check-circle-outline'
      case BookStatus.STOPPED:
        return 'mdi-stop-circle-outline'
      default:
        return 'mdi-circle-outline'
    }
  }

  /** m3-status modifier */
  const statusClass = (status: StatusFilter | null | undefined): string => {
    switch (status) {
      case BookStatus.WANT_TO_READ:
        return 'm3-status--secondary'
      case BookStatus.READING:
        return 'm3-status--primary'
      case BookStatus.READ:
        return 'm3-status--tertiary'
      case BookStatus.STOPPED:
        return 'm3-status--error'
      default:
        return 'm3-status--outlined'
    }
  }

  /** tonal container behind a status icon */
  const statusContainerClass = (status: StatusFilter | null | undefined): string => {
    switch (status) {
      case BookStatus.WANT_TO_READ:
        return 'bg-secondary-container text-on-secondary-container'
      case BookStatus.READING:
        return 'bg-primary-container text-on-primary-container'
      case BookStatus.READ:
        return 'bg-tertiary-container text-on-tertiary-container'
      case BookStatus.STOPPED:
        return 'bg-error-container text-on-error-container'
      default:
        return 'bg-surface-container-highest text-on-surface-variant'
    }
  }

  return {
    statusLabel,
    statusIcon,
    statusClass,
    statusContainerClass,
  }
}
