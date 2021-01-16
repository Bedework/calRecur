package net.fortuna.recur.transform;

import com.ibm.icu.util.Calendar;
import net.fortuna.recur.NumberList;
import net.fortuna.recur.Occurrence;
import net.fortuna.recur.OccurrenceList;
import net.fortuna.recur.Recur.Frequency;
import net.fortuna.recur.WeekDay;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static net.fortuna.recur.Recur.Frequency.DAILY;
import static net.fortuna.recur.Recur.Frequency.MONTHLY;
import static net.fortuna.recur.Recur.Frequency.WEEKLY;
import static net.fortuna.recur.Recur.Frequency.YEARLY;

/**
 * Applies BYHOUR rules specified in this Recur instance to the specified date list. If no BYHOUR rules are
 * specified the date list is returned unmodified.
 */
public class ByHourRule extends AbstractDateExpansionRule {
    private final NumberList hourList;

    private final ExpansionFilter expansionFilter =
            new ExpansionFilter();

    private final LimitFilter limitFilter =
            new LimitFilter();

    private final EnumSet<Frequency> dwmy =
            EnumSet.of(DAILY, WEEKLY, MONTHLY, YEARLY);

    public ByHourRule(final NumberList hourList,
                      final Frequency frequency,
                      final WeekDay.Day weekStartDay) {
        super(frequency, weekStartDay);
        this.hourList = hourList;
    }

    @Override
    public OccurrenceList transform(final OccurrenceList dates) {
        if (hourList.isEmpty()) {
            return dates;
        }
        final OccurrenceList hourlyDates =
                OccurrenceList.getDateListInstance(dates);

        for (final Occurrence date: dates) {
            if (dwmy.contains(getFrequency())) {
                hourlyDates.addAll(expansionFilter.apply(date));
            } else {
                limitFilter.apply(date).ifPresent(hourlyDates::add);
            }
        }
        return hourlyDates;
    }

    private class LimitFilter implements Function<Occurrence,
            Optional<Occurrence>> {
        @Override
        public Optional<Occurrence> apply(final Occurrence date) {
            final Calendar cal = getCalendarInstance(date, true);
            if (hourList.contains(cal.get(Calendar.HOUR_OF_DAY))) {
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

            hourList.forEach(hour -> {
                cal.set(Calendar.HOUR_OF_DAY, hour);
                retVal.add(getTime(cal, date));
            });
            return retVal;
        }
    }
}
