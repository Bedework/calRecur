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
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static net.fortuna.recur.Recur.Frequency.MONTHLY;
import static net.fortuna.recur.Recur.Frequency.YEARLY;
import static net.fortuna.recur.Recur.MAX_DAYS_PER_MONTH;

/**
 * Applies BYMONTHDAY rules specified in this Recur instance to the specified date list. If no BYMONTHDAY rules are
 * specified the date list is returned unmodified.
 */
public class ByMonthDayRule extends AbstractDateExpansionRule {
    private final transient Logger log =
            LoggerFactory.getLogger(ByMonthDayRule.class);

    private final NumberList monthDayList;

    private final ExpansionFilter expansionFilter =
            new ExpansionFilter();

    private final LimitFilter limitFilter =
            new LimitFilter();

    public ByMonthDayRule(final NumberList monthDayList,
                          final Frequency frequency,
                          final WeekDay.Day weekStartDay) {
        super(frequency, weekStartDay);
        this.monthDayList = monthDayList;
    }

    @Override
    public OccurrenceList transform(final OccurrenceList dates) {
        if (monthDayList.isEmpty()) {
            return dates;
        }
        final OccurrenceList monthDayDates =
                OccurrenceList.getDateListInstance(dates);

        for (final Occurrence date: dates) {
            if (EnumSet.of(MONTHLY, YEARLY).contains(getFrequency())) {
                monthDayDates.addAll(expansionFilter.apply(date));
            } else {
                limitFilter.apply(date).ifPresent(monthDayDates::add);
            }
        }
        return monthDayDates;
    }

    private class LimitFilter
            implements Function<Occurrence, Optional<Occurrence>> {
        @Override
        public Optional<Occurrence> apply(final Occurrence date) {
            final Calendar cal = getCalendarInstance(date, true);
            if (monthDayList.contains(cal.get(Calendar.DAY_OF_MONTH))) {
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
            // construct a list of possible month days..
            for (final int monthDay : monthDayList) {
                if (monthDay == 0 ||
                        monthDay < -MAX_DAYS_PER_MONTH ||
                        monthDay > MAX_DAYS_PER_MONTH) {
                    if (log.isTraceEnabled()) {
                        log.trace("Invalid day of month: " + monthDay);
                    }
                    continue;
                }
                final int numDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                if (monthDay > 0) {
                    if (numDaysInMonth < monthDay) {
                        continue;
                    }
                    cal.set(Calendar.DAY_OF_MONTH, monthDay);
                } else {
                    if (numDaysInMonth < -monthDay) {
                        continue;
                    }
                    cal.set(Calendar.DAY_OF_MONTH, numDaysInMonth);
                    cal.add(Calendar.DAY_OF_MONTH, monthDay + 1);
                }
                retVal.add(getTime(cal, date));
            }
            return retVal;
        }
    }
}
