export enum OwnershipStatus {
  OWNED = 'OWNED',
  BORROWED = 'BORROWED',
  NOT_OWNED = 'NOT_OWNED',
}

/** One entry of a book's ownership history, the latest one is the current state. */
export interface OwnershipEvent {
  id?: string,
  creationDate?: string,
  modificationDate?: string,
  status: OwnershipStatus,
  eventDate?: string|null,
  note?: string|null,
}

export interface OwnershipEventInput {
  status: OwnershipStatus,
  eventDate?: string|null,
  note?: string|null,
}
