package net.fortuna.recur.transform;

import com.ibm.icu.util.Calendar;
import net.fortuna.recur.NumberList;
import net.fortuna.recur.Occurrence;
import net.fortuna.recur.OccurrenceList;
import net.fortuna.recur.Recur.Frequency;
import net.fortuna.recur.WeekDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.fortuna.recur.Recur.MAX_WEEKS_PER_YEAR;

/**
 * Applies BYWEEKNO rules specified in this Recur instance to the specified date list. If no BYWEEKNO rules are
 * specified the date list is returned unmodified.
 */
public class ByWeekNoRule extends AbstractDateExpansionRule {

    private final transient Logger log =
            LoggerFactory.getLogger(ByWeekNoRule.class);

    private final NumberList weekNoList;

    public ByWeekNoRule(final NumberList weekNoList,
                        final Frequency frequency,
                        final WeekDay.Day weekStartDay) {
        super(frequency, weekStartDay);
        this.weekNoList = weekNoList;
    }

    @Override
    public OccurrenceList transform(final OccurrenceList dates) {
        if (weekNoList.isEmpty()) {
            return dates;
        }
        final OccurrenceList weekNoDates =
                OccurrenceList.getDateListInstance(dates);
        final Calendar initCal = getCalendarInstance(dates.get(0), true);

        for (final Occurrence date: dates) {
            final int numWeeksInYear = initCal.getActualMaximum(Calendar.WEEK_OF_YEAR);
            for (final Integer weekNo: weekNoList) {
                if (weekNo == 0 ||
                        weekNo < -MAX_WEEKS_PER_YEAR ||
                        weekNo > MAX_WEEKS_PER_YEAR) {
                    if (log.isTraceEnabled()) {
                        log.trace("Invalid week of year: " + weekNo);
                    }
                    continue;
                }

                final Calendar cal = getCalendarInstance(date, true);
                if (weekNo > 0) {
                    if (numWeeksInYear < weekNo) {
                        continue;
                    }

                    cal.set(Calendar.WEEK_OF_YEAR, weekNo);
                } else {
                    if (numWeeksInYear < -weekNo) {
                        continue;
                    }

                    cal.set(Calendar.WEEK_OF_YEAR, numWeeksInYear);
                    cal.add(Calendar.WEEK_OF_YEAR, weekNo + 1);
                }

                weekNoDates.add(getTime(cal, date));
            }
        }
        return weekNoDates;
    }
}
