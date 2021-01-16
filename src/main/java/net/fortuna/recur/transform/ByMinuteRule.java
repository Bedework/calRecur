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
import static net.fortuna.recur.Recur.Frequency.HOURLY;
import static net.fortuna.recur.Recur.Frequency.MONTHLY;
import static net.fortuna.recur.Recur.Frequency.WEEKLY;
import static net.fortuna.recur.Recur.Frequency.YEARLY;

/**
 * Applies BYMINUTE rules specified in this Recur instance to the specified date list. If no BYMINUTE rules are
 * specified the date list is returned unmodified.
 */
public class ByMinuteRule extends AbstractDateExpansionRule {
    private final static EnumSet<Frequency> freqs =
            EnumSet.of(HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY);

    private final NumberList minuteList;

    private final ExpansionFilter expansionFilter =
            new ExpansionFilter();

    private final LimitFilter limitFilter =
            new LimitFilter();

    public ByMinuteRule(final NumberList minuteList,
                        final Frequency frequency,
                        final WeekDay.Day weekStartDay) {
        super(frequency, weekStartDay);
        this.minuteList = minuteList;
    }

    @Override
    public OccurrenceList transform(final OccurrenceList dates) {
        if (minuteList.isEmpty()) {
            return dates;
        }

        final OccurrenceList minutelyDates =
                OccurrenceList.getDateListInstance(dates);

        for (final Occurrence date: dates) {
            if (freqs.contains(getFrequency())) {
                minutelyDates.addAll(expansionFilter.apply(date));
            } else {
                limitFilter.apply(date).ifPresent(minutelyDates::add);
            }
        }
        return minutelyDates;
    }

    private class LimitFilter
            implements Function<Occurrence, Optional<Occurrence>> {

        @Override
        public Optional<Occurrence> apply(final Occurrence date) {
            final Calendar cal = getCalendarInstance(date, true);

            if (minuteList.contains(cal.get(Calendar.MINUTE))) {
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

            // construct a list of possible minutes..
            minuteList.forEach(minute -> {
                cal.set(Calendar.MINUTE, minute);
                retVal.add(getTime(cal, date));
            });
            return retVal;
        }
    }
}
