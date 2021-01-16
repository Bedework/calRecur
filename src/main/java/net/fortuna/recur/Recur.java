/*
 * Copyright (c) 2012, Ben Fortuna
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  o Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 *  o Neither the name of Ben Fortuna nor the names of any other contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.fortuna.recur;

import com.ibm.icu.util.Calendar;
import net.fortuna.recur.transform.ByDayRule;
import net.fortuna.recur.transform.ByHourRule;
import net.fortuna.recur.transform.ByMinuteRule;
import net.fortuna.recur.transform.ByMonthDayRule;
import net.fortuna.recur.transform.ByMonthRule;
import net.fortuna.recur.transform.BySecondRule;
import net.fortuna.recur.transform.BySetPosRule;
import net.fortuna.recur.transform.ByWeekNoRule;
import net.fortuna.recur.transform.ByYearDayRule;
import net.fortuna.recur.transform.Transformer;
import net.fortuna.recur.util.MapTimeZoneCache;
import net.fortuna.recur.util.TimeZoneCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static net.fortuna.recur.Recur.RecurStatus.InvalidRecurrenceRulePart;
import static net.fortuna.recur.Recur.RecurStatus.Ok;

/**
 * $Id$ [18-Apr-2004]
 * <p/>
 * Defines a recurrence.
 *
 * @author Ben Fortuna
 * @version 2.0
 */
public class Recur implements Serializable {

    private static final long serialVersionUID = -7333226591784095142L;

    private static final String FREQ = "FREQ";

    private static final String UNTIL = "UNTIL";

    private static final String COUNT = "COUNT";

    private static final String INTERVAL = "INTERVAL";

    private static final String BYSECOND = "BYSECOND";

    private static final String BYMINUTE = "BYMINUTE";

    private static final String BYHOUR = "BYHOUR";

    private static final String BYDAY = "BYDAY";

    private static final String BYMONTHDAY = "BYMONTHDAY";

    private static final String BYYEARDAY = "BYYEARDAY";

    private static final String BYWEEKNO = "BYWEEKNO";

    private static final String BYMONTH = "BYMONTH";

    private static final String BYSETPOS = "BYSETPOS";

    private static final String WKST = "WKST";

    public enum Frequency {
        SECONDLY, MINUTELY, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY;
    }

    /**
     * When calculating dates matching this recur ({@code getDates()} or {@code getNextDate}),
     * this property defines the maximum number of attempt to find a matching date by
     * incrementing the seed.
     * <p>The default value is 1000. A value of -1 corresponds to no maximum.</p>
     */
    private static int maxIncrementCount = 1000;

    public static void setMaxIncrementCount(final int val) {
        maxIncrementCount = val;
    }

    private final transient Logger log =
            LoggerFactory.getLogger(Recur.class);

    public static TimeZoneCache timeZoneCache = new MapTimeZoneCache();

    /**
     * Maximum number of days per month.
     */
    public static final int MAX_DAYS_PER_MONTH = 31;

    /**
     * Maximum number of weeks per year.
     */
    public static final int MAX_WEEKS_PER_YEAR = 53;

    /**
     * Maximum number of days per year.
     */
    public static final int MAX_DAYS_PER_YEAR = 366;

    private final Frequency frequency;

    private final Date until;

    private final Integer count;

    private final Integer interval;

    private NumberList secondList;

    private NumberList minuteList;

    private NumberList hourList;

    private WeekDayList dayList;

    private NumberList monthDayList;

    private NumberList yearDayList;

    private NumberList weekNoList;

    private NumberList monthList;

    private NumberList setPosList;

    private Map<String, Transformer> transformers;

    private final WeekDay.Day weekStartDay;

    private int calendarWeekStartDay;

    // Calendar field we increment based on frequency.
    private int calIncField;

    public enum RecurStatus {
        Ok,

        InvalidRecurrenceRulePart
    }
    public static class RecurResult {
        private Recur recur;
        private RecurStatus status;
        private String message;

        public Recur getRecur() {
            return recur;
        }

        public RecurStatus getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Constructs a new instance from the specified icalendar string value.
     *
     * @param rule an iCalendar (RFC5545) string representation of a recurrence.
     * @throws IllegalArgumentException where the recurrence string contains an unrecognised token
     */
    public static RecurResult fromIcalendar(final String rule,
                                            final boolean relaxed) {
        final RecurResult res = new RecurResult();
        final Builder builder = new Builder();

        final Iterator<String> tokens =
                Arrays.asList(rule.split("[;=]")).iterator();

        while (tokens.hasNext()) {
            final String token = tokens.next();

            switch (token) {
                case FREQ:
                    builder.frequency(
                            Frequency.valueOf(nextToken(tokens,
                                                            token)));
                    break;

                case UNTIL:
                    final String untilString = nextToken(tokens,
                                                         token);

                    if (untilString != null) {
                        final DateFormat df;

                        if (untilString.contains("T")) {
                            if (untilString.endsWith("Z")) {
                                df = new SimpleDateFormat(
                                        "yyyyMMdd'T'HHmmss'Z'");
                            } else {
                                df = new SimpleDateFormat(
                                        "yyyyMMdd'T'HHmmss");
                            }
                        } else {
                            df = new SimpleDateFormat("yyyyMMdd");
                        }

                        builder.until(
                                untilFromString(df, untilString));
                    }
                    break;

                case COUNT:
                    builder.count(Integer.parseInt(
                            nextToken(tokens, token)));
                    break;

                case INTERVAL:
                    builder.interval(Integer.parseInt(
                            nextToken(tokens, token)));
                    break;

                case BYSECOND:
                    builder.secondList(new NumberList(
                            nextToken(tokens, token), 0, 59, false));
                    break;

                case BYMINUTE:
                    builder.minuteList(new NumberList(
                            nextToken(tokens, token), 0, 59, false));
                    break;

                case BYHOUR:
                    builder.hourList(new NumberList(
                            nextToken(tokens, token), 0, 23, false));
                    break;

                case BYDAY:
                    builder.dayList(new WeekDayList(
                            nextToken(tokens, token)));
                    break;

                case BYMONTHDAY:
                    builder.monthDayList(new NumberList(
                            nextToken(tokens, token), 1, 31, true));
                    break;

                case BYYEARDAY:
                    builder.yearDayList(new NumberList(
                            nextToken(tokens, token), 1, 366, true));
                    break;

                case BYWEEKNO:
                    builder.weekNoList(new NumberList(
                            nextToken(tokens, token), 1, 53, true));
                    break;

                case BYMONTH:
                    builder.monthList(new NumberList(
                            nextToken(tokens, token), 1, 12, false));
                    break;

                case BYSETPOS:
                    builder.setPosList(new NumberList(
                            nextToken(tokens, token), 1, 366, true));
                    break;

                case WKST:
                    builder.weekStartDay(WeekDay.Day
                            .valueOf(nextToken(tokens, token)));
                    break;
                default:
                    if (!relaxed) {
                        res.status = InvalidRecurrenceRulePart;
                        res.message =
                                String.format(
                                        "Invalid recurrence rule part: %s=%s",
                                        token,
                                        nextToken(tokens, token));
                        return res;
                    }
            }
        }

        res.recur = builder.build();
        res.status = Ok;

        return res;
    }

    private static String nextToken(final Iterator<String> tokens,
                                    final String lastToken) {
        try {
            return tokens.next();
        } catch (final NoSuchElementException e) {
            throw new IllegalArgumentException("Missing expected token, last token: " + lastToken);
        }
    }

    /**
     * @param frequency a recurrence frequency string
     * @param until     maximum recurrence date
     */
    public Recur(final Frequency frequency,
                 final Date until) {
        this(frequency, null, null, until,
             null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * @param frequency a recurrence frequency string
     * @param count     maximum recurrence count
     */
    public Recur(final Frequency frequency,
                 final int count) {
        this(frequency, count, null, null,
             null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * @param frequency a recurrence frequency string
     * @param count     maximum recurrence count
     */
    public Recur(final Frequency frequency,
                 final Integer count,
                 final Integer interval,
                 final Date until,
                 final NumberList secondList,
                 final NumberList minuteList,
                 final NumberList hourList,
                 final WeekDayList dayList,
                 final NumberList monthDayList,
                 final NumberList yearDayList,
                 final NumberList weekNoList,
                 final NumberList monthList,
                 final NumberList setPosList,
                 final WeekDay.Day weekStartDay) {
        this.frequency = frequency;
        this.count = count;
        this.interval = interval;
        this.until = until;
        this.secondList = secondList;
        this.minuteList = minuteList;
        this.hourList = hourList;
        this.dayList = dayList;
        this.monthDayList = monthDayList;
        this.yearDayList = yearDayList;
        this.weekNoList = weekNoList;
        this.monthList = monthList;
        this.setPosList = setPosList;
        this.weekStartDay = weekStartDay;

        validate();
        initTransformers();
    }

    private void initTransformers() {
        transformers = new HashMap<>();

        if (secondList != null) {
            transformers.put(BYSECOND,
                             new BySecondRule(secondList,
                                              frequency,
                                              weekStartDay));
        } else {
            secondList = new NumberList(0, 59, false);
        }

        if (minuteList != null) {
            transformers.put(BYMINUTE,
                             new ByMinuteRule(minuteList,
                                              frequency,
                                              weekStartDay));
        } else {
            minuteList = new NumberList(0, 59, false);
        }

        if (hourList != null) {
            transformers.put(BYHOUR,
                             new ByHourRule(hourList,
                                            frequency,
                                            weekStartDay));
        } else {
            hourList = new NumberList(0, 23, false);
        }

        if (monthDayList != null) {
            transformers.put(BYMONTHDAY,
                             new ByMonthDayRule(monthDayList,
                                                frequency,
                                                weekStartDay));
        } else {
            monthDayList = new NumberList(1, 31, true);
        }

        if (yearDayList != null) {
            transformers.put(BYYEARDAY,
                             new ByYearDayRule(yearDayList,
                                               frequency,
                                               weekStartDay));
        } else {
            yearDayList = new NumberList(1, 366, true);
        }

        if (weekNoList != null) {
            transformers.put(BYWEEKNO,
                             new ByWeekNoRule(weekNoList,
                                              frequency,
                                              weekStartDay));
        } else {
            weekNoList = new NumberList(1, 53, true);
        }

        if (monthList != null) {
            transformers.put(BYMONTH,
                             new ByMonthRule(monthList,
                                             frequency,
                                             weekStartDay));
        } else {
            monthList = new NumberList(1, 12, false);
        }

        if (dayList != null) {
            transformers.put(BYDAY,
                             new ByDayRule(dayList,
                                           deriveFilterType(),
                                           weekStartDay));
        } else {
            dayList = new WeekDayList();
        }

        if (setPosList != null) {
            transformers.put(BYSETPOS, new BySetPosRule(setPosList));
        } else {
            setPosList = new NumberList(1, 366, true);
        }
    }

    private Frequency deriveFilterType() {
        if (frequency == Frequency.DAILY ||
                !getYearDayList().isEmpty() ||
                !getMonthDayList().isEmpty()) {
            return Frequency.DAILY;
        }

        if (frequency == Frequency.WEEKLY ||
                !getWeekNoList().isEmpty()) {
            return Frequency.WEEKLY;
        }

        if (frequency == Frequency.MONTHLY ||
                !getMonthList().isEmpty()) {
            return Frequency.MONTHLY;
        }

        if (frequency == Frequency.YEARLY) {
            return Frequency.YEARLY;
        }

        return null;
    }

    /**
     * Accessor for the configured BYDAY list.
     * NOTE: Any changes to the returned list will have no effect on the recurrence rule processing.
     *
     * @return Returns the dayList.
     */
    public final WeekDayList getDayList() {
        return dayList;
    }

    /**
     * Accessor for the configured BYHOUR list.
     * NOTE: Any changes to the returned list will have no effect on the recurrence rule processing.
     *
     * @return Returns the hourList.
     */
    public final NumberList getHourList() {
        return hourList;
    }

    /**
     * Accessor for the configured BYMINUTE list.
     * NOTE: Any changes to the returned list will have no effect on the recurrence rule processing.
     *
     * @return Returns the minuteList.
     */
    public final NumberList getMinuteList() {
        return minuteList;
    }

    /**
     * Accessor for the configured BYMONTHDAY list.
     * NOTE: Any changes to the returned list will have no effect on the recurrence rule processing.
     *
     * @return Returns the monthDayList.
     */
    public final NumberList getMonthDayList() {
        return monthDayList;
    }

    /**
     * Accessor for the configured BYMONTH list.
     * NOTE: Any changes to the returned list will have no effect on the recurrence rule processing.
     *
     * @return Returns the monthList.
     */
    public final NumberList getMonthList() {
        return monthList;
    }

    /**
     * Accessor for the configured BYSECOND list.
     * NOTE: Any changes to the returned list will have no effect on the recurrence rule processing.
     *
     * @return Returns the secondList.
     */
    public final NumberList getSecondList() {
        return secondList;
    }

    /**
     * Accessor for the configured BYSETPOS list.
     * NOTE: Any changes to the returned list will have no effect on the recurrence rule processing.
     *
     * @return Returns the setPosList.
     */
    public final NumberList getSetPosList() {
        return setPosList;
    }

    /**
     * Accessor for the configured BYWEEKNO list.
     * NOTE: Any changes to the returned list will have no effect on the recurrence rule processing.
     *
     * @return Returns the weekNoList.
     */
    public final NumberList getWeekNoList() {
        return weekNoList;
    }

    /**
     * Accessor for the configured BYYEARDAY list.
     * NOTE: Any changes to the returned list will have no effect on the recurrence rule processing.
     *
     * @return Returns the yearDayList.
     */
    public final NumberList getYearDayList() {
        return yearDayList;
    }

    /**
     * @return Returns the count or -1 if the rule does not have a count.
     */
    public final int getCount() {
        return Optional.ofNullable(count).orElse(-1);
    }

    /**
     * @return Returns the frequency.
     */
    public final Frequency getFrequency() {
        return frequency;
    }

    /**
     * @return Returns the interval or null if the rule does not have an interval defined.
     */
    public final Integer getInterval() {
        return Optional.ofNullable(interval).orElse(-1);
    }

    /**
     * @return Returns the until or null if there is none.
     */
    public final Date getUntil() {
        return until;
    }

    /**
     * @return Returns the weekStartDay or null if there is none.
     */
    public final WeekDay.Day getWeekStartDay() {
        return weekStartDay;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String toString() {
        final StringBuilder b = new StringBuilder();
        b.append(FREQ);
        b.append('=');
        b.append(frequency);
        if (weekStartDay != null) {
            b.append(';');
            b.append(WKST);
            b.append('=');
            b.append(weekStartDay);
        }
        if (until != null) {
            b.append(';');
            b.append(UNTIL);
            b.append('=');
            // Note: date-time representations should always be in UTC time.
            b.append(until);
        }
        if (count != null) {
            b.append(';');
            b.append(COUNT);
            b.append('=');
            b.append(count);
        }
        if (interval != null) {
            b.append(';');
            b.append(INTERVAL);
            b.append('=');
            b.append(interval);
        }
        if (!monthList.isEmpty()) {
            b.append(';');
            b.append(BYMONTH);
            b.append('=');
            b.append(monthList);
        }
        if (!weekNoList.isEmpty()) {
            b.append(';');
            b.append(BYWEEKNO);
            b.append('=');
            b.append(weekNoList);
        }
        if (!yearDayList.isEmpty()) {
            b.append(';');
            b.append(BYYEARDAY);
            b.append('=');
            b.append(yearDayList);
        }
        if (!monthDayList.isEmpty()) {
            b.append(';');
            b.append(BYMONTHDAY);
            b.append('=');
            b.append(monthDayList);
        }
        if (!dayList.isEmpty()) {
            b.append(';');
            b.append(BYDAY);
            b.append('=');
            b.append(dayList);
        }
        if (!hourList.isEmpty()) {
            b.append(';');
            b.append(BYHOUR);
            b.append('=');
            b.append(hourList);
        }
        if (!minuteList.isEmpty()) {
            b.append(';');
            b.append(BYMINUTE);
            b.append('=');
            b.append(minuteList);
        }
        if (!secondList.isEmpty()) {
            b.append(';');
            b.append(BYSECOND);
            b.append('=');
            b.append(secondList);
        }
        if (!setPosList.isEmpty()) {
            b.append(';');
            b.append(BYSETPOS);
            b.append('=');
            b.append(setPosList);
        }
        return b.toString();
    }

    /**
     * Returns a list of start dates in the specified period represented by this recur. Any date fields not specified by
     * this recur are retained from the period start, and as such you should ensure the period start is initialised
     * correctly.
     *
     * @param periodStart the start of the period
     * @param periodEnd   the end of the period
     * @return a list of dates
     */
    public final OccurrenceList getDates(final Occurrence periodStart,
                                         final Occurrence periodEnd) {
        return getDates(periodStart, periodStart, periodEnd,
                        -1);
    }

    /**
     * Returns a list of start dates in the specified period represented by this recur. This method includes a base date
     * argument, which indicates the start of the fist occurrence of this recurrence. The base date is used to inject
     * default values to return a set of dates in the correct format. For example, if the search start date (start) is
     * Wed, Mar 23, 12:19PM, but the recurrence is Mon - Fri, 9:00AM - 5:00PM, the start dates returned should all be at
     * 9:00AM, and not 12:19PM.
     *
     * @param seed        the start date of this Recurrence's first instance
     * @param periodStart the start of the period
     * @param periodEnd   the end of the period
     * @return a list of dates represented by this recur instance
     */
    public final OccurrenceList getDates(final Occurrence seed,
                                         final Occurrence periodStart,
                                         final Occurrence periodEnd) {
        return getDates(seed, periodStart, periodEnd,
                        -1);
    }

    /**
     * Returns a list of start dates in the specified period represented by this recur. This method includes a base date
     * argument, which indicates the start of the fist occurrence of this recurrence. The base date is used to inject
     * default values to return a set of dates in the correct format. For example, if the search start date (start) is
     * Wed, Mar 23, 12:19PM, but the recurrence is Mon - Fri, 9:00AM - 5:00PM, the start dates returned should all be at
     * 9:00AM, and not 12:19PM.
     *
     * @param seed        the start date of this Recurrence's first instance
     * @param periodStart the start of the period
     * @param periodEnd   the end of the period
     * @param maxCount    limits the number of instances returned. Up to one years
     *                    worth extra may be returned. Less than 0 means no limit
     * @return a list of dates represented by this recur instance
     */
    public final OccurrenceList getDates(final Occurrence seed,
                                         final Occurrence periodStart,
                                         final Occurrence periodEnd,
                                         final int maxCount) {
        final boolean dateOnly = seed.getDateOnly();
        final OccurrenceList dates =
                new OccurrenceList(dateOnly);
        if (!dateOnly) {
            if (seed.getUtc()) {
                dates.setUtc(true);
            } else {
                dates.setTimeZone(seed.getTimeZone());
            }
        }

        final Calendar cal =
                getCalendarInstance(seed,
                                    calendarWeekStartDay, true);
        final Calendar rootSeed = (Calendar)cal.clone();
        
        // optimize the start time for selecting candidates
        // (only applicable where a COUNT is not specified)
        if (count == null) {
            final Calendar seededCal = (Calendar) cal.clone();
            while (seededCal.getTime().before(periodStart.getDate())) {
                cal.setTime(seededCal.getTime());
                increment(seededCal);
            }
        }

        final HashSet<Occurrence> invalidCandidates = new HashSet<>();

        int noCandidateIncrementCount = 0;
        Occurrence candidate = null;
        while ((maxCount < 0) || (dates.size() < maxCount)) {
            final Occurrence candidateSeed =
                    Occurrence.getInstanceLike(cal.getTime(),
                                               seed);

            if (getUntil() != null && candidate != null
                    && candidate.getDate().after(getUntil())) {
                break;
            }
            if (periodEnd != null && candidate != null
                    && candidate.after(periodEnd)) {
                break;
            }
            if (getCount() >= 1
                    && (dates.size() + invalidCandidates.size()) >= getCount()) {
                break;
            }

            // rootSeed = date used for the seed for the RRule at the
            //            start of the first period.
            // candidateSeed = date used for the start of 
            //                 the current period.
            final OccurrenceList candidates =
                    getCandidates(rootSeed, candidateSeed, dateOnly);
            if (!candidates.isEmpty()) {
                noCandidateIncrementCount = 0;
                // sort candidates for identifying when UNTIL date is exceeded..
                Collections.sort(candidates);
                for (final Occurrence candidate1: candidates) {
                    candidate = candidate1;
                    // don't count candidates that occur before the seed date..
                    if (!candidate.before(seed)) {
                        // candidates exclusive of periodEnd..
                        if (candidate.before(periodStart)
                                || candidate.after(periodEnd)) {
                            invalidCandidates.add(candidate);
                        } else if (getCount() >= 1
                                && (dates.size() + invalidCandidates.size()) >= getCount()) {
                            break;
                        } else if (!candidate.before(periodStart) && !candidate.after(periodEnd)
                            && (getUntil() == null || !candidate.after(getUntil()))) {

                            dates.add(candidate);
                        }
                    }
                }
            } else {
                noCandidateIncrementCount++;
                if ((maxIncrementCount > 0) && (noCandidateIncrementCount > maxIncrementCount)) {
                    break;
                }
            }
            increment(cal);
        }
        // sort final list..
        Collections.sort(dates);
        return dates;
    }

    /**
     * Returns the the next date of this recurrence given a seed date
     * and start date.  The seed date indicates the start of the fist
     * occurrence of this recurrence. The start date is the
     * starting date to search for the next recurrence.  Return null
     * if there is no occurrence date after start date.
     *
     * @param seed      the start date of this Recurrence's first instance
     * @param startDate the date to start the search
     * @return the next date in the recurrence series after startDate
     */
    public final Occurrence getNextDate(final Occurrence seed,
                                        final Occurrence startDate) {

        final Calendar cal = getCalendarInstance(seed,
                                                 calendarWeekStartDay,
                                                 true);
        final Calendar rootSeed = (Calendar)cal.clone();

        // optimize the start time for selecting candidates
        // (only applicable where a COUNT is not specified)
        if (count == null) {
            final Calendar seededCal = (Calendar) cal.clone();
            while (seededCal.getTime().before(startDate.getDate())) {
                cal.setTime(seededCal.getTime());
                increment(seededCal);
            }
        }

        int invalidCandidateCount = 0;
        int noCandidateIncrementCount = 0;
        Occurrence candidate = null;

        final boolean dateOnly = seed.getDateOnly();

        while (true) {
            final Occurrence candidateSeed =
                    Occurrence.getInstanceLike(cal.getTime(),
                                               seed);

            if (getUntil() != null &&
                    candidate != null &&
                    candidate.after(getUntil())) {
                break;
            }

            if (getCount() > 0 &&
                    invalidCandidateCount >= getCount()) {
                break;
            }

            final OccurrenceList candidates =
                    getCandidates(rootSeed, candidateSeed, dateOnly);

            if (!candidates.isEmpty()) {
                noCandidateIncrementCount = 0;
                // sort candidates for identifying when UNTIL date is exceeded..
                Collections.sort(candidates);

                for (final Occurrence candidate1: candidates) {
                    candidate = candidate1;
                    // don't count candidates that occur before the seed date..

                    if (!candidate.before(seed)) {
                        // Candidate must be after startDate because
                        // we want the NEXT occurrence
                        if (!candidate.after(startDate)) {
                            invalidCandidateCount++;
                        } else if (getCount() > 0
                                && invalidCandidateCount >= getCount()) {
                            break;
                        } else if (!(getUntil() != null
                                && candidate.after(getUntil()))) {
                            return candidate;
                        }
                    }
                }
            } else {
                noCandidateIncrementCount++;
                if ((maxIncrementCount > 0) &&
                        (noCandidateIncrementCount > maxIncrementCount)) {
                    break;
                }
            }
            increment(cal);
        }
        return null;
    }

    /**
     * Increments the specified calendar according to the frequency and interval specified in this recurrence rule.
     *
     * @param cal a java.util.Calendar to increment
     */
    private void increment(final Calendar cal) {
        // initialise interval..
        if (getInterval() >= 1) {
            cal.add(calIncField, getInterval());
        } else {
            cal.add(calIncField, 1);
        }
    }

    /**
     * Returns a list of possible dates generated from the applicable BY* rules, using the specified date as a seed.
     *
     * @param date  the seed date
     * @param dateOnly the type of list to return
     * @return a DateList
     */
    private OccurrenceList getCandidates(final Calendar rootSeed,
                                         final Occurrence date,
                                         final boolean dateOnly) {
        OccurrenceList dates = new OccurrenceList(dateOnly);
        dates.add(date); // If first will set utc/timezone

        if (transformers.get(BYMONTH) != null) {
            dates = transformers.get(BYMONTH).transform(dates);
            // debugging..
            if (log.isDebugEnabled()) {
                log.debug("Dates after BYMONTH processing: " + dates);
            }
        }

        if (transformers.get(BYWEEKNO) != null) {
            dates = transformers.get(BYWEEKNO).transform(dates);
            // debugging..
            if (log.isDebugEnabled()) {
                log.debug("Dates after BYWEEKNO processing: " + dates);
            }
        }

        if (transformers.get(BYYEARDAY) != null) {
            dates = transformers.get(BYYEARDAY).transform(dates);
            // debugging..
            if (log.isDebugEnabled()) {
                log.debug("Dates after BYYEARDAY processing: " + dates);
            }
        }

        if (transformers.get(BYMONTHDAY) != null) {
            dates = transformers.get(BYMONTHDAY).transform(dates);
            // debugging..
            if (log.isDebugEnabled()) {
                log.debug("Dates after BYMONTHDAY processing: " + dates);
            }
        } else if ((frequency == Frequency.MONTHLY && dayList.isEmpty()) ||
                (frequency == Frequency.YEARLY && yearDayList.isEmpty() && weekNoList.isEmpty() && dayList.isEmpty())) {

            final NumberList implicitMonthDayList = new NumberList();
            implicitMonthDayList.add(rootSeed.get(Calendar.DAY_OF_MONTH));
            final ByMonthDayRule implicitRule =
                    new ByMonthDayRule(implicitMonthDayList,
                                       frequency,
                                       weekStartDay);
            dates = implicitRule.transform(dates);
        }

        if (transformers.get(BYDAY) != null) {
            dates = transformers.get(BYDAY).transform(dates);
            // debugging..
            if (log.isDebugEnabled()) {
                log.debug("Dates after BYDAY processing: " + dates);
            }
        } else if (frequency == Frequency.WEEKLY || (frequency == Frequency.YEARLY && yearDayList.isEmpty()
                && !weekNoList.isEmpty() && monthDayList.isEmpty())) {

            final ByDayRule implicitRule = new ByDayRule(new WeekDayList(WeekDay.getWeekDay(rootSeed)),
                    deriveFilterType(),  weekStartDay);
            dates = implicitRule.transform(dates);
        }

        if (transformers.get(BYHOUR) != null) {
            dates = transformers.get(BYHOUR).transform(dates);
            // debugging..
            if (log.isDebugEnabled()) {
                log.debug("Dates after BYHOUR processing: " + dates);
            }
        }

        if (transformers.get(BYMINUTE) != null) {
            dates = transformers.get(BYMINUTE).transform(dates);
            // debugging..
            if (log.isDebugEnabled()) {
                log.debug("Dates after BYMINUTE processing: " + dates);
            }
        }

        if (transformers.get(BYSECOND) != null) {
            dates = transformers.get(BYSECOND).transform(dates);
            // debugging..
            if (log.isDebugEnabled()) {
                log.debug("Dates after BYSECOND processing: " + dates);
            }
        }

        if (transformers.get(BYSETPOS) != null) {
            dates = transformers.get(BYSETPOS).transform(dates);
            // debugging..
            if (log.isDebugEnabled()) {
                log.debug("Dates after SETPOS processing: " + dates);
            }
        }
        return dates;
    }

    private void validate() {
        if (frequency == null) {
            throw new IllegalArgumentException("A recurrence MUST have a frequency.");
        }

        switch (frequency) {
            case SECONDLY:
                calIncField = Calendar.SECOND;
                break;
            case MINUTELY:
                calIncField = Calendar.MINUTE;
                break;
            case HOURLY:
                calIncField = Calendar.HOUR_OF_DAY;
                break;
            case DAILY:
                calIncField = Calendar.DAY_OF_YEAR;
                break;
            case WEEKLY:
                calIncField = Calendar.WEEK_OF_YEAR;
                break;
            case MONTHLY:
                calIncField = Calendar.MONTH;
                break;
            case YEARLY:
                calIncField = Calendar.YEAR;
                break;
            default:
                throw new IllegalArgumentException(
                        "Invalid frequency '"
                                + frequency + "' in recurrence rule");
        }

        if ((count != null) && (until != null)) {
            throw new IllegalArgumentException(
                    "Cannot have both UNTIL and COUNT " +
                            "in a recurrence rule");
        }

        if (weekStartDay != null) {
            calendarWeekStartDay = WeekDay.getCalendarDay(WeekDay.getWeekDay(weekStartDay));
        } else {
            // default week start is Monday per RFC5545
            calendarWeekStartDay = Calendar.MONDAY;
        }
    }

    /**
     * Construct a Calendar object and sets the time.
     *
     * @param date Occurence for the date
     * @param calendarWeekStartDay start of week
     * @param lenient true for more relaxed
     * @return an ICU Calendar
     */
    public static Calendar getCalendarInstance(
            final Occurrence date,
            final int calendarWeekStartDay,
            final boolean lenient) {
        final Calendar cal;

        if (date.getTimeZone() != null) {
            cal = Calendar.getInstance(date.getTimeZone());
        } else {
            cal = Calendar.getInstance();
        }

        // A week should have at least 4 days to be considered as such per RFC5545
        cal.setMinimalDaysInFirstWeek(4);
        cal.setFirstDayOfWeek(calendarWeekStartDay);
        cal.setLenient(lenient);
        date.setCalendarTime(cal);

        return cal;
    }

    /**
     * Support for building Recur instances.
     */
    public static class Builder {

        private Frequency frequency;

        private Date until;

        private Integer count;

        private Integer interval;

        private NumberList secondList;

        private NumberList minuteList;

        private NumberList hourList;

        private WeekDayList dayList;

        private NumberList monthDayList;

        private NumberList yearDayList;

        private NumberList weekNoList;

        private NumberList monthList;

        private NumberList setPosList;

        private WeekDay.Day weekStartDay;

        public Builder frequency(final Frequency frequency) {
            this.frequency = frequency;
            return this;
        }

        public Builder until(final Date until) {
            this.until = until;
            return this;
        }

        public Builder count(final Integer count) {
            this.count = count;
            return this;
        }

        public Builder interval(final Integer interval) {
            this.interval = interval;
            return this;
        }

        public Builder secondList(final NumberList secondList) {
            this.secondList = secondList;
            return this;
        }

        public Builder minuteList(final NumberList minuteList) {
            this.minuteList = minuteList;
            return this;
        }

        public Builder hourList(final NumberList hourList) {
            this.hourList = hourList;
            return this;
        }

        public Builder dayList(final WeekDayList dayList) {
            this.dayList = dayList;
            return this;
        }

        public Builder monthDayList(final NumberList monthDayList) {
            this.monthDayList = monthDayList;
            return this;
        }

        public Builder yearDayList(final NumberList yearDayList) {
            this.yearDayList = yearDayList;
            return this;
        }

        public Builder weekNoList(final NumberList weekNoList) {
            this.weekNoList = weekNoList;
            return this;
        }

        public Builder monthList(final NumberList monthList) {
            this.monthList = monthList;
            return this;
        }

        public Builder setPosList(final NumberList setPosList) {
            this.setPosList = setPosList;
            return this;
        }

        public Builder weekStartDay(final WeekDay.Day weekStartDay) {
            this.weekStartDay = weekStartDay;
            return this;
        }

        public Recur build() {
            return new Recur(frequency,
                             count,
                             interval,
                             until,
                             secondList,
                             minuteList,
                             hourList,
                             dayList,
                             monthDayList,
                             yearDayList,
                             weekNoList,
                             monthList,
                             setPosList,
                             weekStartDay);
        }
    }

    private static Date untilFromString(final DateFormat df,
                                        final String val) {
        try {
            return df.parse(val);
        } catch (final ParseException pe) {
            throw new IllegalArgumentException(pe);
        }
    }
}
