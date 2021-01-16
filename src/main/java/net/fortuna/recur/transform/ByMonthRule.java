package net.fortuna.recur.transform;

import com.ibm.icu.util.Calendar;
import net.fortuna.recur.NumberList;
import net.fortuna.recur.Occurrence;
import net.fortuna.recur.OccurrenceList;
import net.fortuna.recur.Recur.Frequency;
import net.fortuna.recur.WeekDay;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Applies BYMONTH rules specified in this Recur instance to the specified date list. If no BYMONTH rules are
 * specified the date list is returned unmodified.
 */
public class ByMonthRule extends AbstractDateExpansionRule {

    private final NumberList monthList;

    private final ExpansionFilter expansionFilter =
            new ExpansionFilter();

    private final LimitFilter limitFilter =
            new LimitFilter();

    public ByMonthRule(final NumberList monthList,
                       final Frequency frequency,
                       final WeekDay.Day weekStartDay) {
        super(frequency, weekStartDay);
        this.monthList = monthList;
    }

    @Override
    public OccurrenceList transform(final OccurrenceList dates) {
        if (monthList.isEmpty()) {
            return dates;
        }

        final OccurrenceList monthlyDates =
                OccurrenceList.getDateListInstance(dates);

        for (final Occurrence date: dates) {
            if (getFrequency() == Frequency.YEARLY) {
                monthlyDates.addAll(expansionFilter.apply(date));
            } else {
                limitFilter.apply(date).ifPresent(monthlyDates::add);
            }
        }
        return monthlyDates;
    }

    private class LimitFilter
            implements Function<Occurrence, Optional<Occurrence>> {

        @Override
        public Optional<Occurrence> apply(final Occurrence date) {
            final Calendar cal = getCalendarInstance(date, true);
            // Java months are zero-based..
            if (monthList.contains(cal.get(Calendar.MONTH) + 1)) {
                return Optional.of(date);
            }
            return Optional.empty();
        }
    }

    private class ExpansionFilter
            implements Function<Occurrence, List<Occurrence>> {
        @Override
        public List<Occurrence> apply(final Occurrence date) {
            final List<Occurrence> retVal = new ArrayList<>();
            final Calendar cal = getCalendarInstance(date, true);

            // construct a list of possible months..
            monthList.forEach(month -> {
                // Java months are zero-based..
//                cal.set(Calendar.MONTH, month - 1);
                cal.roll(Calendar.MONTH,
                         (month - 1) - cal.get(Calendar.MONTH));
                retVal.add(getTime(cal, date));
            });
            return retVal;
        }
    }
}
