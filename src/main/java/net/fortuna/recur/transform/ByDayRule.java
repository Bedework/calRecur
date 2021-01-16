package net.fortuna.recur.transform;

import com.ibm.icu.util.Calendar;
import net.fortuna.recur.Occurrence;
import net.fortuna.recur.OccurrenceList;
import net.fortuna.recur.Recur.Frequency;
import net.fortuna.recur.WeekDay;
import net.fortuna.recur.WeekDayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Applies BYDAY rules specified in this Recur instance to the specified date list. If no BYDAY rules are specified
 * the date list is returned unmodified.
 */
public class ByDayRule extends AbstractDateExpansionRule {
    private final WeekDayList dayList;

    public ByDayRule(final WeekDayList dayList,
                     final Frequency frequency,
                     final WeekDay.Day weekStartDay) {
        super(frequency, weekStartDay);
        this.dayList = dayList;
    }

    @Override
    public OccurrenceList transform(final OccurrenceList dates) {
        if (dayList.isEmpty()) {
            return dates;
        }

        final OccurrenceList weekDayDates =
                OccurrenceList.getDateListInstance(dates);

        final Function<Occurrence, List<Occurrence>> transformer;

        switch (getFrequency()) {
            case WEEKLY:
                transformer = new WeeklyExpansionFilter();
                break;
            case MONTHLY:
                transformer = new MonthlyExpansionFilter();
                break;
            case YEARLY:
                transformer = new YearlyExpansionFilter();
                break;
            case DAILY:
            default:
                transformer = new LimitFilter();
        }

        for (final Occurrence date: dates) {
            final List<Occurrence> transformed = transformer.apply(date);

            // filter by offset
            final List<Occurrence> filtered = new ArrayList<>();

            dayList.forEach(day -> filtered.addAll(
                    getOffsetDates(transformed.stream().filter(
                            d -> getCalendarInstance(d, true)
                                    .get(Calendar.DAY_OF_WEEK) == WeekDay.getCalendarDay(day))
                                              .collect(Collectors.toCollection(() -> OccurrenceList.getDateListInstance(weekDayDates))), day.getOffset())));
            weekDayDates.addAll(filtered);
        }
        return weekDayDates;
    }

    private class LimitFilter
            implements Function<Occurrence, List<Occurrence>> {
        @Override
        public List<Occurrence> apply(final Occurrence date) {
            final Calendar cal = getCalendarInstance(date, true);
            if (dayList.contains(WeekDay.getWeekDay(cal))) {
                return Collections.singletonList(date);
            }
            return Collections.emptyList();
        }
    }

    private class WeeklyExpansionFilter
            implements Function<Occurrence, List<Occurrence>> {
        @Override
        public List<Occurrence> apply(final Occurrence date) {
            final List<Occurrence> retVal = new ArrayList<>();

            final Calendar cal = getCalendarInstance(date, true);
            final int weekNo = cal.get(Calendar.WEEK_OF_YEAR);
            // construct a list of possible week days..
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
            while (cal.get(Calendar.WEEK_OF_YEAR) == weekNo) {
                if (dayList.stream().map(WeekDay::getCalendarDay)
                           .anyMatch(calDay -> cal
                                     .get(Calendar.DAY_OF_WEEK) == calDay)) {
                    retVal.add(getTime(cal, date));
                }
                cal.add(Calendar.DAY_OF_WEEK, 1);
            }
            return retVal;
        }
    }

    private class MonthlyExpansionFilter
            implements Function<Occurrence, List<Occurrence>> {
        @Override
        public List<Occurrence> apply(final Occurrence date) {
            final List<Occurrence> retVal = new ArrayList<>();

            final Calendar cal = getCalendarInstance(date, true);
            final int month = cal.get(Calendar.MONTH);
            // construct a list of possible month days..
            cal.set(Calendar.DAY_OF_MONTH, 1);
            while (cal.get(Calendar.MONTH) == month) {
                if (dayList.stream().map(WeekDay::getCalendarDay)
                           .anyMatch(calDay -> cal
                                     .get(Calendar.DAY_OF_WEEK) == calDay)) {
                    retVal.add(getTime(cal, date));
                }
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }
            return retVal;
        }
    }

    private class YearlyExpansionFilter
            implements Function<Occurrence, List<Occurrence>> {
        @Override
        public List<Occurrence> apply(final Occurrence date) {
            final List<Occurrence> retVal = new ArrayList<>();

            final Calendar cal = getCalendarInstance(date, true);
            final int year = cal.get(Calendar.YEAR);
            // construct a list of possible year days..
            cal.set(Calendar.DAY_OF_YEAR, 1);
            while (cal.get(Calendar.YEAR) == year) {
                if (dayList.stream().map(WeekDay::getCalendarDay)
                           .anyMatch(calDay -> cal
                                     .get(Calendar.DAY_OF_WEEK) == calDay)) {
                    retVal.add(getTime(cal, date));
                }
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
            return retVal;
        }
    }

    /**
     * Returns a single-element sublist containing the element of <code>list</code> at <code>offset</code>. Valid
     * offsets are from 1 to the size of the list. If an invalid offset is supplied, all elements from <code>list</code>
     * are added to <code>sublist</code>.
     *
     * @param dates
     * @param offset
     */
    private List<Occurrence> getOffsetDates(final OccurrenceList dates,
                                            final int offset) {
        if (offset == 0) {
            return dates;
        }
        final List<Occurrence> offsetDates =
                OccurrenceList.getDateListInstance(dates);
        final int size = dates.size();
        if (offset < 0 && offset >= -size) {
            offsetDates.add(dates.get(size + offset));
        } else if (offset > 0 && offset <= size) {
            offsetDates.add(dates.get(offset - 1));
        }
        return offsetDates;
    }
}
