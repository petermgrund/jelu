import { Author } from "../model/Author";
import { Book, UserBook } from "../model/Book";
import Swal from 'sweetalert2';
import { Tag } from "../model/Tag";

export class ObjectUtils {

  // from https://javascript.plainenglish.io/deep-clone-an-object-and-preserve-its-type-with-typescript-d488c35e5574
  public static deepCopy<T>(source: T): T {
    return Array.isArray(source)
    //@ts-ignore
    ? source.map(item => this.deepCopy(item))
    : source instanceof Date
    //@ts-ignore
    ? new Date(source.getTime())
        : source && typeof source === 'object'
          ? Object.getOwnPropertyNames(source).reduce((o, prop) => {
            Object.defineProperty(o, prop, Object.getOwnPropertyDescriptor(source, prop)!);
            o[prop] = this.deepCopy((source as { [key: string]: any })[prop]);
            return o;
          }, Object.create(Object.getPrototypeOf(source)))
          : source as T;
  }

  public static toast = (oruga: any, variant: string, message: string, duration = 2000) => {
    oruga.notification.open({
      message: message,
      position: "bottom",
      variant: variant,
      duration: duration,
    });
  };

  public static toUserBook = (book: Book): UserBook => {
    const converted = {
      id: book.userBookId != null ? book.userBookId : null,
      book: book,
      lastReadingEvent: book.userbook?.lastReadingEvent != null ? book.userbook?.lastReadingEvent : null,
      lastReadingEventDate: book.userbook?.lastReadingEventDate != null ? book.userbook?.lastReadingEventDate : null,
      creationDate: book.userbook?.creationDate != null ? book.userbook?.creationDate : null,
      modificationDate: book.userbook?.modificationDate != null ? book.userbook?.modificationDate : null,
      personalNotes: book.userbook?.personalNotes != null ? book.userbook?.personalNotes : null,
      owned: book.userbook?.owned != null ? book.userbook?.owned : null,
      borrowed: book.userbook?.borrowed != null ? book.userbook?.borrowed : null,
      toRead: book.userbook?.toRead != null ? book.userbook?.toRead : null,
      readingEvents: book.userbook?.readingEvents != null ? book.userbook?.readingEvents : null,
      percentRead: book.userbook?.percentRead != null ? book.userbook?.percentRead : null,
      currentPageNumber: book.userbook?.currentPageNumber != null ? book.userbook?.currentPageNumber : null,
      avgRating: book.userbook?.avgRating != null ? book.userbook?.avgRating : null,
      userAvgRating: book.userbook?.userAvgRating != null ? book.userbook?.userAvgRating : null
    } as UserBook
    return converted
  }

  public static unwrapUserBook = (book: Book): UserBook => {
    const userbook = book.userbook
    if (userbook != undefined) {
      userbook.book = {
        ...book,
        userbook: undefined
      }
      console.log('ub')
      console.log(userbook)
      return userbook
    } else {
      const converted = {
        id: undefined,
        book: book
      } as UserBook
      return converted
    }
  }
  // sweetalert2 dialogs styled as M3 basic dialogs (see assets/m3/vendors.css)
  private static swalClasses = {
    container: '',
    popup: '',
    htmlContainer: '',
    title: '',
    input: '',
    actions: '',
    confirmButton: 'm3-btn m3-btn--text',
    denyButton: 'm3-btn m3-btn--text m3-btn--error',
    cancelButton: 'm3-btn m3-btn--text',
  }

  public static swalMixin = Swal.mixin({
    buttonsStyling: false,
    reverseButtons: true,
    customClass: ObjectUtils.swalClasses,
  })

  /**
   * Workaround sweetalert bug
   */
  public static swalYesNoMixin = Swal.mixin({
    buttonsStyling: false,
    reverseButtons: true,
    customClass: ObjectUtils.swalClasses,
  })

  public static baseSwalMixin = Swal.mixin({
    buttonsStyling: false,
    customClass: ObjectUtils.swalClasses,
  })

  /**
   *
   * @param newVals [currentPageNumber, percentRead, pageCount]
   * @param oldVals [currentPageNumber, percentRead, pageCount]
   * @param target userbook, or reactive form
   * @param pageCount current pageCount
   */
  public static computePages = (newVals: Array<number | string | null | undefined>, oldVals: Array<number | string | null | undefined>,
    target: { currentPageNumber?: number | null, percentRead?: number | null }, pageCount: number | null) => {
    console.log("pagecount " + pageCount)
    if (pageCount != null) {
      console.log(newVals)
      console.log(oldVals)
      if (newVals[0] as number >= pageCount) {
        target.currentPageNumber = pageCount
        target.percentRead = 100
      } else {
        target.percentRead = Math.min(100, ((newVals[0] as number * 100) / pageCount))
      }
    } else {
      target.currentPageNumber = null
    }
  }

  // https://stackoverflow.com/questions/39924644/es6-generate-an-array-of-numbers
  public static range = (start: number, end: number, step: number) => {
    return Array.from(Array.from(Array(Math.ceil((end - start) / step)).keys()), x => start + x * step);
  }

  public static wrapForOptions = (object: any) => {
    return {"label": object.name, "value": object}
  }

  public static wrapAsAuthor(a: Author|Tag|string) {
      if (typeof a === 'string') {
        return {"name": a}
      } else {
        return a
      }
  }

  public static createNamedItem(item: Author | Tag | string) {
  console.log("create")
  console.log(item)
  if (typeof item === 'string') {
    return {"name": item}
  } else {
    return item
  }
}

// only take minutes to avoid invalidating cache too often
public static timestamp = () => new Date().toISOString().substring(0,16)

public static amountInLocale = (amount: number, locale: string, currency: string) => {
  let localCurrency = currency
  console.log("amount " + amount + " locale " + locale + " currency " + currency)
  if (currency.length !== 3) {
    console.log("wrong currency " + currency + " , go set it in the settings page, it should be the currency code, ie EUR")
    localCurrency = "EUR"
  }
  return amount.toLocaleString(locale, {
    maximumFractionDigits: 2,
    style: "currency",
    currency: localCurrency
    })
}


/** route query values are a string when there is only one, always get an array */
public static asArray = <T>(value: T | Array<T> | null | undefined): Array<T> => {
  if (Array.isArray(value)) {
    return value
  }
  return value == null || value === '' ? [] : [value]
}

}

export const asArray = ObjectUtils.asArray
