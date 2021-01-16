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
 * Applies BYSECOND rules specified in this Recur instance to the specified date list. If no BYSECOND rules are
 * specified the date list is returned unmodified.
 */
public class BySecondRule extends AbstractDateExpansionRule {
    private final NumberList secondList;

    private final ExpansionFilter expansionFilter =
            new ExpansionFilter();

    private final LimitFilter limitFilter =
            new LimitFilter();

    public BySecondRule(final NumberList secondList,
                        final Frequency frequency,
                        final WeekDay.Day weekStartDay) {
        super(frequency, weekStartDay);
        this.secondList = secondList;
    }

    @Override
    public OccurrenceList transform(final OccurrenceList dates) {
        if (secondList.isEmpty()) {
            return dates;
        }
        final OccurrenceList secondlyDates =
                OccurrenceList.getDateListInstance(dates);

        for (final Occurrence date: dates) {
            if (getFrequency() == Frequency.SECONDLY) {
                limitFilter.apply(date).ifPresent(secondlyDates::add);
            } else {
                secondlyDates.addAll(
                        expansionFilter.apply(date));
            }
        }
        return secondlyDates;
    }

    private class LimitFilter
            implements Function<Occurrence, Optional<Occurrence>> {
        @Override
        public Optional<Occurrence> apply(final Occurrence date) {
            final Calendar cal = getCalendarInstance(date, true);
            if (secondList.contains(cal.get(Calendar.SECOND))) {
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

            // construct a list of possible seconds..
            secondList.forEach(second -> {
                cal.set(Calendar.SECOND, second);
                retVal.add(getTime(cal, date));
            });
            return retVal;
        }
    }
}
