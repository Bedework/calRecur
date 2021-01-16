package net.fortuna.recur.transform;

import com.ibm.icu.util.Calendar;
import net.fortuna.recur.NumberList;
import net.fortuna.recur.Occurrence;
import net.fortuna.recur.OccurrenceList;
import net.fortuna.recur.Recur.Frequency;
import net.fortuna.recur.WeekDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static net.fortuna.recur.Recur.Frequency.YEARLY;
import static net.fortuna.recur.Recur.MAX_DAYS_PER_YEAR;

/**
 * Applies BYYEARDAY rules specified in this Recur instance to the specified date list. If no BYYEARDAY rules are
 * specified the date list is returned unmodified.
 */
public class ByYearDayRule extends AbstractDateExpansionRule {
    private final transient Logger log =
            LoggerFactory.getLogger(ByYearDayRule.class);

    private final NumberList yearDayList;

    private final ExpansionFilter expansionFilter =
            new ExpansionFilter();

    private final LimitFilter limitFilter =
            new LimitFilter();

    public ByYearDayRule(final NumberList yearDayList,
                         final Frequency frequency,
                         final WeekDay.Day weekStartDay) {
        super(frequency, weekStartDay);
        this.yearDayList = yearDayList;
    }

    @Override
    public OccurrenceList transform(final OccurrenceList dates) {
        if (yearDayList.isEmpty()) {
            return dates;
        }
        final OccurrenceList yearDayDates =
                OccurrenceList.getDateListInstance(dates);
        for (final Occurrence date: dates) {
            if (getFrequency() == YEARLY) {
                yearDayDates.addAll(expansionFilter.apply(date));
            } else {
                limitFilter.apply(date).ifPresent(yearDayDates::add);
            }
        }
        return yearDayDates;
    }

    private class LimitFilter
            implements Function<Occurrence, Optional<Occurrence>> {
        @Override
        public Optional<Occurrence> apply(final Occurrence date) {
            final Calendar cal = getCalendarInstance(date, true);
            if (yearDayList.contains(cal.get(Calendar.DAY_OF_YEAR))) {
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
            final Calendar cal = getCalendarInstance(date, false);
            // construct a list of possible year days..
            for (final int yearDay : yearDayList) {
                if (yearDay == 0 ||
                        yearDay < -MAX_DAYS_PER_YEAR ||
                        yearDay > MAX_DAYS_PER_YEAR) {
                    if (log.isTraceEnabled()) {
                        log.trace("Invalid day of year: " + yearDay);
                    }
                    continue;
                }

                final int numDaysInYear =
                        cal.getActualMaximum(Calendar.DAY_OF_YEAR);
                if (yearDay > 0) {
                    if (numDaysInYear < yearDay) {
                        continue;
                    }
                    cal.set(Calendar.DAY_OF_YEAR, yearDay);
                } else {
                    if (numDaysInYear < -yearDay) {
                        continue;
                    }
                    cal.set(Calendar.DAY_OF_YEAR, numDaysInYear);
                    cal.add(Calendar.DAY_OF_YEAR, yearDay + 1);
                }
                retVal.add(getTime(cal, date));
            }
            return retVal;
        }
    }
}
