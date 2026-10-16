import { useI18n } from 'vue-i18n';
import { ReadingEventType } from '../model/ReadingEvent';


export default function useEvents() {
  const { t } = useI18n({
    inheritLocale: true,
    useScope: 'global'
  })

  /** m3-status modifier for a reading event type */
  const eventClass = (type: ReadingEventType | null | undefined) => {
    if (type === ReadingEventType.FINISHED) {
      return "m3-status--tertiary";
    } else if (type === ReadingEventType.DROPPED) {
      return "m3-status--error";
    } else if (type === ReadingEventType.CURRENTLY_READING) {
      return "m3-status--primary";
    } else return "";
  };

  /** mdi icon class for a reading event type */
  const eventIcon = (type: ReadingEventType | null | undefined) => {
    if (type === ReadingEventType.FINISHED) {
      return "mdi-check-circle-outline";
    } else if (type === ReadingEventType.DROPPED) {
      return "mdi-close-circle-outline";
    } else if (type === ReadingEventType.CURRENTLY_READING) {
      return "mdi-book-open-page-variant-outline";
    } else return "mdi-circle-outline";
  };

  const eventLabel = (type: ReadingEventType | null | undefined) => {
    if (type === ReadingEventType.FINISHED) {
      return t('reading_events.finished');
    } else if (type === ReadingEventType.DROPPED) {
      return t('reading_events.dropped');
    } else if (type === ReadingEventType.CURRENTLY_READING) {
      return t('reading_events.reading');
    } else if (type === ReadingEventType.NONE) {
      return t('reading_events.none');
    } else return "";
  };

  return {
    eventClass,
    eventIcon,
    eventLabel
  }
}
