export type CalendarModelValue = Date | Date[] | (Date | null)[] | null | undefined

export function firstDateFromCalendarValue(value: CalendarModelValue): Date | undefined {
  if (value instanceof Date) {
    return value
  }

  if (Array.isArray(value)) {
    for (const entry of value) {
      if (entry instanceof Date) {
        return entry
      }
    }
  }

  return undefined
}
