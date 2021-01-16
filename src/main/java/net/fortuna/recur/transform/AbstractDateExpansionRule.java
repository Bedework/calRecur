package net.fortuna.recur.transform;

import com.ibm.icu.util.Calendar;
import net.fortuna.recur.Occurrence;
import net.fortuna.recur.Recur;
import net.fortuna.recur.Recur.Frequency;
import net.fortuna.recur.WeekDay;

import java.io.Serializable;

/**
 * Subclasses provide implementations to expand (or limit) a list of dates based on rule requirements as
 * specified in RFC5545.
 *
 * <pre>
 *     3.3.10.  Recurrence Rule
 *
 *       ...
 *
 *       BYxxx rule parts modify the recurrence in some manner.  BYxxx rule
 *       parts for a period of time that is the same or greater than the
 *       frequency generally reduce or limit the number of occurrences of
 *       the recurrence generated.  For example, "FREQ=DAILY;BYMONTH=1"
 *       reduces the number of recurrence instances from all days (if
 *       BYMONTH rule part is not present) to all days in January.  BYxxx
 *       rule parts for a period of time less than the frequency generally
 *       increase or expand the number of occurrences of the recurrence.
 *       For example, "FREQ=YEARLY;BYMONTH=1,2" increases the number of
 *       days within the yearly recurrence set from 1 (if BYMONTH rule part
 *       is not present) to 2.
 *
 *       If multiple BYxxx rule parts are specified, then after evaluating
 *       the specified FREQ and INTERVAL rule parts, the BYxxx rule parts
 *       are applied to the current set of evaluated occurrences in the
 *       following order: BYMONTH, BYWEEKNO, BYYEARDAY, BYMONTHDAY, BYDAY,
 *       BYHOUR, BYMINUTE, BYSECOND and BYSETPOS; then COUNT and UNTIL are
 *       evaluated.
 *
 *       The table below summarizes the dependency of BYxxx rule part
 *       expand or limit behavior on the FREQ rule part value.
 *
 *       The term "N/A" means that the corresponding BYxxx rule part MUST
 *       NOT be used with the corresponding FREQ value.
 *
 *       BYDAY has some special behavior depending on the FREQ value and
 *       this is described in separate notes below the table.
 *
 *    +----------+--------+--------+-------+-------+------+-------+------+
 *    |          |SECONDLY|MINUTELY|HOURLY |DAILY  |WEEKLY|MONTHLY|YEARLY|
 *    +----------+--------+--------+-------+-------+------+-------+------+
 *    |BYMONTH   |Limit   |Limit   |Limit  |Limit  |Limit |Limit  |Expand|
 *    +----------+--------+--------+-------+-------+------+-------+------+
 *    |BYWEEKNO  |N/A     |N/A     |N/A    |N/A    |N/A   |N/A    |Expand|
 *    +----------+--------+--------+-------+-------+------+-------+------+
 *    |BYYEARDAY |Limit   |Limit   |Limit  |N/A    |N/A   |N/A    |Expand|
 *    +----------+--------+--------+-------+-------+------+-------+------+
 *    |BYMONTHDAY|Limit   |Limit   |Limit  |Limit  |N/A   |Expand |Expand|
 *    +----------+--------+--------+-------+-------+------+-------+------+
 *    |BYDAY     |Limit   |Limit   |Limit  |Limit  |Expand|Note 1 |Note 2|
 *    +----------+--------+--------+-------+-------+------+-------+------+
 *    |BYHOUR    |Limit   |Limit   |Limit  |Expand |Expand|Expand |Expand|
 *    +----------+--------+--------+-------+-------+------+-------+------+
 *    |BYMINUTE  |Limit   |Limit   |Expand |Expand |Expand|Expand |Expand|
 *    +----------+--------+--------+-------+-------+------+-------+------+
 *    |BYSECOND  |Limit   |Expand  |Expand |Expand |Expand|Expand |Expand|
 *    +----------+--------+--------+-------+-------+------+-------+------+
 *    |BYSETPOS  |Limit   |Limit   |Limit  |Limit  |Limit |Limit  |Limit |
 *    +----------+--------+--------+-------+-------+------+-------+------+
 *
 *       Note 1:  Limit if BYMONTHDAY is present; otherwise, special expand
 *                for MONTHLY.
 *
 *       Note 2:  Limit if BYYEARDAY or BYMONTHDAY is present; otherwise,
 *                special expand for WEEKLY if BYWEEKNO present; otherwise,
 *                special expand for MONTHLY if BYMONTH present; otherwise,
 *                special expand for YEARLY.
 * </pre>
 */
public abstract class AbstractDateExpansionRule
        implements Transformer, Serializable {

    private final Frequency frequency;

    private final int calendarWeekStartDay;

    /**
     *
     * @param frequency for recurrence
     * @param weekStartDay null for default
     */
    public AbstractDateExpansionRule(final Frequency frequency,
                                     final WeekDay.Day weekStartDay) {
        this.frequency = frequency;

        final WeekDay wday;
        if (weekStartDay == null) {
            wday = WeekDay.getWeekDay(WeekDay.Day.MO);
        } else {
            wday = WeekDay.getWeekDay(weekStartDay);
        }

        if (wday == null) {
            // Invalid day
            calendarWeekStartDay = WeekDay.getCalendarDay(WeekDay.MO);
        } else {
            calendarWeekStartDay = WeekDay.getCalendarDay(wday);
        }
    }

    protected Frequency getFrequency() {
        return frequency;
    }

    /**
     * Construct a Calendar object and sets the time.
     *
     * @param date
     * @param lenient
     * @return
     */
    protected Calendar getCalendarInstance(final Occurrence date,
                                           final boolean lenient) {
        return Recur.getCalendarInstance(date,
                                         calendarWeekStartDay,
                                         lenient);
    }

    /**
     * Get a date and time from cal.getTime().
     *
     * @param cal
     * @return
     */
    protected static Occurrence getTime(final Calendar cal,
                                        final Occurrence date) {
        return Occurrence.getInstanceLike(cal.getTime(), date);
    }
}
