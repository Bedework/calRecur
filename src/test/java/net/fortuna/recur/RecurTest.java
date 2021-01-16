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
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.TimeZone;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.fortuna.recur.Recur.Frequency;
import net.fortuna.recur.Recur.RecurResult;
import net.fortuna.recur.util.TimeZones;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static net.fortuna.recur.WeekDay.FR;
import static net.fortuna.recur.WeekDay.MO;
import static net.fortuna.recur.WeekDay.SU;
import static net.fortuna.recur.WeekDay.TH;
import static net.fortuna.recur.WeekDay.TU;
import static net.fortuna.recur.WeekDay.WE;

/**
 * Created on 14/02/2005
 *
 * $Id$
 *
 * @author Ben Fortuna
 */
public class RecurTest extends TestCase {

    private static final Logger log =
            LoggerFactory.getLogger(RecurTest.class);

    private static final Locale testLocale = Locale.US;

    private TimeZone originalDefault;

    private Recur recur;

    private Occurrence periodStart;

    private Occurrence periodEnd;

    private boolean dateOnly;

    private int expectedCount;

    private Occurrence seed;

    private Occurrence expectedDate;

    private int calendarField;

    private int expectedCalendarValue;

    private TimeZone expectedTimeZone;

    private String recurrenceString;

    private Frequency expectedFrequency;

    private Integer expectedInterval;

    private WeekDayList expectedDayList;

    /**
     * @param testMethod to test
     * @param recur initialised Recur
     * @param periodStart occurrence
     * @param periodEnd occurrence
     * @param dateOnly true for no time
     */
    public RecurTest(final String testMethod,
                     final Recur recur,
                     final Occurrence seed,
                     final Occurrence periodStart,
                     final Occurrence periodEnd,
                     final boolean dateOnly) {
        super(testMethod);
        this.recur = recur;
        this.seed = seed;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.dateOnly = dateOnly;
    }

    /**
     * @param recur initialised Recur
     * @param periodStart occurrence
     * @param periodEnd occurrence
     * @param dateOnly true for no time
     * @param expectedCount expected size of result
     */
    public RecurTest(final Recur recur,
                     final Occurrence periodStart,
                     final Occurrence periodEnd,
                     final boolean dateOnly,
                     final int expectedCount) {
        this(recur, null, periodStart, periodEnd,
             dateOnly, expectedCount);
    }

    /**
     * @param recur initialised Recur
     * @param seed occurrence
     * @param periodStart occurrence
     * @param periodEnd occurrence
     * @param dateOnly true for no time
     * @param expectedCount expected size of result
     */
    public RecurTest(final Recur recur,
                     final Occurrence seed,
                     final Occurrence periodStart,
                     final Occurrence periodEnd,
                     final boolean dateOnly,
                     final int expectedCount) {
        this("testGetDatesCount",
             recur,
             seed,
             periodStart,
             periodEnd,
             dateOnly);
        this.expectedCount = expectedCount;
    }

    /**
     * @param recur initialised Recur
     * @param seed occurrence
     * @param periodStart occurrence
     * @param expectedDate occurrence
     */
    public RecurTest(final Recur recur,
                     final Occurrence seed,
                     final Occurrence periodStart,
                     final Occurrence expectedDate) {
        this("testGetNextDate", recur, seed, periodStart, null, false);
        this.expectedDate = expectedDate;
    }

    /**
     * @param recur initialised Recur
     * @param periodStart occurrence
     * @param periodEnd occurrence
     * @param dateOnly true for no time
     * @param calendarField to test
     * @param expectedCalendarValue to match
     */
    public RecurTest(final Recur recur,
                     final Occurrence periodStart,
                     final Occurrence periodEnd,
                     final boolean dateOnly,
                     final int calendarField,
                     final int expectedCalendarValue) {
        this("testGetDatesCalendarField", recur, null, periodStart, periodEnd, dateOnly);
        this.calendarField = calendarField;
        this.expectedCalendarValue = expectedCalendarValue;
    }

    /**
     * @param recur initialised Recur
     * @param periodStart occurrence
     * @param periodEnd occurrence
     * @param dateOnly true for no time
     * @param expectedTimeZone to match
     */
    public RecurTest(final Recur recur,
                     final Occurrence periodStart,
                     final Occurrence periodEnd,
                     final boolean dateOnly,
                     final TimeZone expectedTimeZone) {
        this("testGetDatesTimeZone", recur, null, periodStart, periodEnd, dateOnly);
        this.expectedTimeZone = expectedTimeZone;
    }

    /**
     * @param recurrenceString to test
     */
    public RecurTest(final String recurrenceString) {
        super("testInvalidRecurrenceString");
        this.recurrenceString = recurrenceString;
    }

    /**
     * @param recurrenceString to test
     * @param expectedFrequency to match
     * @param expectedInterval to match
     * @param expectedDayList to match
     */
    public RecurTest(final String recurrenceString,
                     final Frequency expectedFrequency,
                     final int expectedInterval,
                     final WeekDayList expectedDayList) {
        super("testRecurrenceString");
        this.recurrenceString = recurrenceString;
        this.expectedFrequency = expectedFrequency;
        this.expectedInterval = expectedInterval;
        this.expectedDayList = expectedDayList;
    }

    @Override
    protected void setUp() {
        originalDefault = TimeZone.getDefault();
    }

    @Override
    protected void tearDown() {
        TimeZone.setDefault(originalDefault);
    }

    /**
     *
     */
    public void testGetDatesCount() {
        TimeZone.setDefault(TimeZone.getTimeZone("Australia/Melbourne"));
        final OccurrenceList dates;
        if (seed != null) {
            dates = recur.getDates(seed, periodStart, periodEnd);
        }
        else {
            dates = recur.getDates(periodStart, periodEnd);
        }
        assertEquals(expectedCount, dates.size());
        // assertTrue("Date list exceeds expected count", dates.size() <= expectedCount);
    }

    /**
     *
     */
    public void testGetNextDate() {
        final Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(testLocale);
            assertEquals(expectedDate, recur.getNextDate(seed, periodStart));
        } finally {
            Locale.setDefault(defaultLocale);
        }

    }

    /**
     *
     */
    public void testGetDatesCalendarField() {
        final OccurrenceList dates =
                recur.getDates(periodStart, periodEnd);

        final Calendar cal;
        if (dateOnly) {
            cal = Calendar.getInstance(TimeZones.getDateTimeZone());
        } else {
            cal = Calendar.getInstance();
        }

        dates.forEach(date -> {
            date.setCalendarTime(cal);
            assertEquals(expectedCalendarValue, cal.get(calendarField));
        });
    }

    /**
     *
     */
    public void testGetDatesOrdering() {
        final OccurrenceList dl1 = recur.getDates(periodStart, periodEnd);
        Occurrence prev;
        Occurrence event = null;
        for (int i = 0; i < dl1.size(); i++) {
            prev = event;
            event = dl1.get(i);
            log.debug("Occurence " + i + " at " + event);
            assertTrue(prev == null || !prev.after(event));
        }
    }

    /**
     *
     */
    public void testGetDatesNotEmpty() {
        assertFalse(recur.getDates(periodStart, periodEnd).isEmpty());
    }

    /**
     *
     */
    public void testGetDatesTimeZone() {
        final OccurrenceList dates = recur.getDates(periodStart, periodEnd);
        dates.forEach(date -> assertEquals(expectedTimeZone,
                                           date.getTimeZone()));
    }

    public void testInvalidRecurrenceString() {
        try {
            fromRule(recurrenceString);
            fail("IllegalArgumentException not thrown!");
        } catch (final IllegalArgumentException e) {
            // expected
            log.info("Caught exception: " + e.getMessage());
        }
    }

    /**
     */
    public void testRecurrenceString()  {
        final Recur recur = fromRule(recurrenceString);

        assertEquals(expectedFrequency, recur.getFrequency());
        assertEquals(expectedInterval, recur.getInterval());
        assertEquals(expectedDayList, recur.getDayList());
    }

    /**
     *
     */
    public void testGetDatesWithBase() {
        /*
         *  Here is an example of evaluating multiple BYxxx rule parts.
         *
         *    DTSTART;TZID=US-Eastern:19970105T083000
         *    RRULE:FREQ=YEARLY;INTERVAL=2;BYMONTH=1;BYDAY=SU;BYHOUR=8,9;
         *     BYMINUTE=30
         */
        final Calendar testCal = Calendar.getInstance();
        testCal.set(Calendar.YEAR, 1997);
        testCal.set(Calendar.MONTH, 1);
        testCal.set(Calendar.DAY_OF_MONTH, 5);
        testCal.set(Calendar.HOUR, 8);
        testCal.set(Calendar.MINUTE, 30);
        testCal.set(Calendar.SECOND, 0);

        final Recur recur = new Recur.Builder()
                .frequency(Frequency.YEARLY)
                .count(-1)
                .interval(2)
                .monthList(new NumberList("1"))
                .dayList(new WeekDayList(SU))
                .hourList(new NumberList("8,9"))
                .minuteList(new NumberList("30"))
                .build();

        final Calendar cal = Calendar.getInstance();
        final Occurrence start = dateTime(cal);
        cal.add(Calendar.YEAR, 2);
        final Occurrence end = dateTime(cal);

        log.debug(recur.toString());

        final OccurrenceList dates =
                recur.getDates(dateTime(testCal),
                               start,
                               end);
        log.debug(dates.toString());
    }

    /*
    public void testSublistNegative() {
        List list = new LinkedList();
        list.add("1");
        list.add("2");
        list.add("3");
        assertSublistEquals(list, list, 0);
        assertSublistEquals(asList("3"), list, -1);
        assertSublistEquals(asList("2"), list, -2);
        assertSublistEquals(asList("1"), list, -3);
        assertSublistEquals(list, list, -4);
    }

    public void testSublistPositive() {
        List list = new LinkedList();
        list.add("1");
        list.add("2");
        list.add("3");
        assertSublistEquals(list, list, 0);
        assertSublistEquals(asList("1"), list, 1);
        assertSublistEquals(asList("2"), list, 2);
        assertSublistEquals(asList("3"), list, 3);
        assertSublistEquals(list, list, 4);
    }

    private void assertSublistEquals(List expected, List list, int offset) {
        List sublist = new LinkedList();
        Recur.sublist(list, offset, sublist);
        assertEquals(expected, sublist);
    }

    private List asList(Object o) {
        List list = new LinkedList();
        list.add(o);
        return list;
    }

    public void testSetPosNegative() throws Exception {
        Date[] dates = new Date[] { new Date(1), new Date(2), new Date(3) };
        Date[] expected = new Date[] { new Date(3), new Date(2) };
        assertSetPosApplied(expected, dates, "BYSETPOS=-1,-2");
    }

    public void testSetPosPositve() throws Exception {
        Date[] dates = new Date[] { new Date(1), new Date(2), new Date(3) };
        Date[] expected = new Date[] { new Date(2), new Date(3) };
        assertSetPosApplied(expected, dates, "BYSETPOS=2,3");
    }

    public void testSetPosOutOfBounds() throws Exception {
        Date[] dates = new Date[] { new Date(1) };
        Date[] expected = new Date[] {};
        assertSetPosApplied(expected, dates, "BYSETPOS=-2,2");
    }

    private void assertSetPosApplied(Date[] expected, Date[] dates, String rule)
            throws Exception {
        Recur recur = new Recur(rule);
        final OccurrenceList expectedList = asDateList(expected);
        assertEquals(expectedList, recur.applySetPosRules(asDateList(dates)));
    }

    private DateList asDateList(Date[] dates) {
        DateList dateList = new DateList(Value.DATE);
        dateList.addAll(Arrays.asList(dates));
        return dateList;
    }
    */

    /**
     * This test confirms SETPOS rules are working correctly.
     * <pre>
     *      The BYSETPOS rule part specifies a COMMA character (US-ASCII decimal
     *      44) separated list of values which corresponds to the nth occurrence
     *      within the set of events specified by the rule. Valid values are 1 to
     *      366 or -366 to -1. It MUST only be used in conjunction with another
     *      BYxxx rule part. For example "the last work day of the month" could
     *      be represented as:
     *
     *        RRULE:FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=-1
     * </pre>
     */
    public final void testSetPosProcessing() {
        final Recur recur =
                new Recur.Builder()
                        .frequency(Frequency.MONTHLY).count(-1)
                        .dayList(new WeekDayList(MO, TU, WE, TH, FR))
                        .setPosList(new NumberList("-1"))
                        .build();

        log.debug(recur.toString());

        final Calendar cal = Calendar.getInstance();
        final Occurrence start = dateTime(cal);
        cal.add(Calendar.YEAR, 2);
        final Occurrence end = dateTime(cal);

        final OccurrenceList dates = recur.getDates(start, end);
        log.debug(dates.toString());
    }

    public void testMgmill2001() {
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 11);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.YEAR, 2005);
        final java.util.Date eventStart = cal.getTime();

        cal.set(Calendar.DAY_OF_MONTH, 1);
        final java.util.Date rangeStart = cal.getTime();

        cal.set(Calendar.YEAR, 2009);
        final java.util.Date rangeEnd = cal.getTime();

        // FREQ=MONTHLY;INTERVAL=1;COUNT=4;BYMONTHDAY=2
        Recur recur =
                new Recur.Builder()
                        .frequency(Frequency.MONTHLY)
                        .count(4)
                        .interval(1)
                        .monthDayList(new NumberList("2"))
                        .build();

        getDates(rangeStart, rangeEnd, eventStart, recur);

        // FREQ=MONTHLY;INTERVAL=2;COUNT=4;BYDAY=2MO
        recur = new Recur.Builder()
                .frequency(Frequency.MONTHLY)
                .count(4)
                .interval(2)
                .dayList(new WeekDayList(new WeekDay(MO, 2)))
                .build();

        getDates(rangeStart, rangeEnd, eventStart, recur);

        // FREQ=YEARLY;COUNT=4;BYMONTH=2;BYMONTHDAY=3
        recur = new Recur.Builder()
                .frequency(Frequency.YEARLY)
                .count(4)
                .monthList(new NumberList("2"))
                .monthDayList(new NumberList("3"))
                .build();

        getDates(rangeStart, rangeEnd, eventStart, recur);

        // FREQ=YEARLY;COUNT=4;BYMONTH=2;BYDAY=2SU
        recur = new Recur.Builder()
                .frequency(Frequency.YEARLY)
                .count(4)
                .monthList(new NumberList("2"))
                .dayList(new WeekDayList(new WeekDay(SU, 2)))
                .build();

        getDates(rangeStart, rangeEnd, eventStart, recur);
    }

    private void getDates(final java.util.Date startRange,
                          final java.util.Date endRange,
                          final java.util.Date eventStart,
                          final Recur recur) {
        final TimeZone tz= TimeZone.getDefault();
        final Occurrence start = dateTime(startRange, tz);
        final Occurrence end = dateTime(endRange, tz);
        final Occurrence seed = dateTime(eventStart, tz);

        final OccurrenceList dates = recur.getDates(seed, start, end);

        for (int i = 0; i < dates.size(); i++) {
            log.info("date_" + i + " = " + dates.get(i).toString());
        }
    }

    public void testGetDatesRalph() {
        final Recur recur = fromRule(
                "FREQ=WEEKLY;WKST=MO;INTERVAL=1;" +
                        "UNTIL=20051003T000000Z;BYDAY=MO,WE");

        final Calendar queryStartDate = new GregorianCalendar(
                TimeZone.getTimeZone(TimeZones.UTC_ID));
        queryStartDate.set(2005, Calendar.SEPTEMBER, 3, 0,
                           0, 0);

        final Calendar queryEndDate = new GregorianCalendar(
                TimeZone.getTimeZone(TimeZones.UTC_ID));
        queryEndDate.set(2005, Calendar.OCTOBER, 31, 23,
                         59, 0);

        final OccurrenceList dateList =
                recur.getDates(utc(queryStartDate),
                               utc(queryStartDate),
                               utc(queryEndDate));

        log.debug(dateList.toString());
    }

    /* *
     *
     */
    /*
    public void testInvalidRule() throws ParseException {
//        String rrule = "FREQ=DAILY;COUNT=60;BYDAY=TU,TH;BYSETPOS=2";
//        Recur recur = new Recur(rrule);

        Calendar cal = Calendar.getInstance();
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.HOUR);
//        java.util.Date start = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, 1);
//        java.util.Date end = cal.getTime();

//        DateList recurrences = recur.getDates(new DateTime(start),
//                new DateTime(end), Value.DATE_TIME);
    }
    */

    /**
     * @return a suite
     */
    public static TestSuite suite() {
        final TestSuite suite = new TestSuite();

        // java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Europe/Paris"));

        // testGetDates..
        Recur recur = new Recur.Builder()
                .frequency(Frequency.DAILY)
                .count(10)
                .interval(2)
                .build();
        log.debug(recur.toString());

        Calendar cal = Calendar.getInstance();
        cal.set(2018, 12, 16);
        Occurrence start = dateTime(cal);
        cal.add(Calendar.DAY_OF_WEEK_IN_MONTH, 10);
        Occurrence end = dateTime(cal);
        log.debug(recur.getDates(start, end
        ).toString());

        recur = new Recur.Builder()
                .frequency(Frequency.DAILY)
                .until(new java.util.Date(cal.getTime().getTime()))
                .interval(2)
                .build();
        log.info(recur.toString());
        log.debug(recur.getDates(start, end).toString());

        recur = new Recur.Builder()
                .frequency(Frequency.WEEKLY)
                .until(new java.util.Date(cal.getTime().getTime()))
                .interval(2)
                .dayList(new WeekDayList(MO))
                .build();
        log.debug(recur.toString());

        final OccurrenceList dates =
                recur.getDates(start, end);
        log.debug(dates.toString());

        suite.addTest(new RecurTest(recur, start, end, true, 5));

        // testGetNextDate..
        recur = new Recur.Builder()
                .frequency(Frequency.DAILY)
                .count(3)
                .build();

        Occurrence seed = dateOnly("20080401");
        Occurrence firstDate = dateOnly("20080402");
        Occurrence secondDate = dateOnly("20080403");

        suite.addTest(new RecurTest(recur, seed, seed, firstDate));
        suite.addTest(new RecurTest(recur, seed, firstDate, secondDate));
        suite.addTest(new RecurTest(recur, seed, secondDate, null));

        // test DateTime
        recur = new Recur.Builder()
                .frequency(Frequency.WEEKLY)
                .until(dateTime("20080421T063000").getDate())
                .build();
        seed = dateTime("20080407T063000");
        firstDate = dateTime("20080414T063000");
        secondDate = dateTime("20080421T063000");

        suite.addTest(new RecurTest(recur, seed, seed, firstDate));
        suite.addTest(new RecurTest(recur, seed, firstDate, secondDate));
        suite.addTest(new RecurTest(recur, seed, secondDate, null));

        // Test BYDAY rules..
        recur = new Recur.Builder()
                .frequency(Frequency.DAILY)
                .count(10)
                .interval(1)
                .dayList(new WeekDayList(MO, TU, WE, TH, FR))
                .build();
        log.debug(recur.toString());

        cal = Calendar.getInstance();
        start = dateOnly(cal);
        cal.add(Calendar.DAY_OF_WEEK_IN_MONTH, 10);
        end = dateOnly(cal);

        suite.addTest(new RecurTest(recur, start, end, true, 10));

        // Test BYDAY recurrence rules..
        recur = fromRule("FREQ=MONTHLY;WKST=SU;INTERVAL=2;BYDAY=5TU");

        cal = Calendar.getInstance();
        cal.clear(Calendar.SECOND);
        start = dateOnly(cal);
        cal.add(Calendar.YEAR, 2);
        end = dateOnly(cal);

        suite.addTest(new RecurTest(recur, start, end, true,
                                    Calendar.WEEK_OF_MONTH, 5));

        /*
         * This test creates a rule outside of the specified boundaries to
         * confirm that the returned date list is empty.
         * <pre>
         *  Weekly on Tuesday and Thursday for 5 weeks:
         *
         *  DTSTART;TZID=US-Eastern:19970902T090000
         *  RRULE:FREQ=WEEKLY;UNTIL=19971007T000000Z;WKST=SU;BYDAY=TU,TH
         *  or
         *
         *  RRULE:FREQ=WEEKLY;COUNT=10;WKST=SU;BYDAY=TU,TH
         *
         *  ==> (1997 9:00 AM EDT)September 2,4,9,11,16,18,23,25,30;October 2
         * </pre>
         */
        recur = new Recur.Builder()
                .frequency(Frequency.WEEKLY)
                .count(10)
                .dayList(new WeekDayList(TU, TH))
                .build();

        log.debug(recur.toString());

        cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 1997);
        cal.set(Calendar.MONTH, Calendar.SEPTEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 2);
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);

        seed = dateOnly(cal);
        cal = Calendar.getInstance();
        start = dateOnly(cal);
        cal.add(Calendar.YEAR, 2);
        end = dateOnly(cal);

        suite.addTest(new RecurTest(recur, seed, start, end,
                                    true, 0));

        // testRecurGetDates..
        recur = fromRule("FREQ=WEEKLY;INTERVAL=1;BYDAY=SA");

        start = dateOnly("20050101");
        end = dateOnly("20060101");

        suite.addTest(new RecurTest(recur, start, end, true,
                                    Calendar.DAY_OF_WEEK,
                                    Calendar.SATURDAY));

        // Test ordering of returned dates..
        recur = fromRule("FREQ=WEEKLY;COUNT=75;" +
                                 "INTERVAL=2;BYDAY=SU,MO,TU;WKST=SU");
        cal = Calendar.getInstance();
        final Occurrence d1 = dateOnly(cal);
        cal.add(Calendar.YEAR,1);
        final Occurrence d2 = dateOnly(cal);

        suite.addTest(new RecurTest("testGetDatesOrdering",
                                    recur, null, d1, d2, true));

        // testMonthByDay..
        recur = fromRule("FREQ=MONTHLY;UNTIL=20061220;" +
                                 "INTERVAL=1;BYDAY=3WE");

        cal = Calendar.getInstance(TimeZones.getDateTimeZone());
        cal.set(2006, 11, 1);
        start = dateOnly(cal);
        cal.add(Calendar.YEAR, 1);

        suite.addTest(new RecurTest("testGetDatesNotEmpty",
                                    recur, null, start,
                                    dateOnly(cal), true));

        // testAlternateTimeZone..
        recur = fromRule("FREQ=WEEKLY;BYDAY=WE;BYHOUR=12;BYMINUTE=0");

//        TimeZone originalDefault = TimeZone.getDefault();
//        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));

        cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        cal.clear(Calendar.SECOND);
        start = dateTime(cal);
        cal.add(Calendar.MONTH, 2);
        end = dateTime(cal);

        suite.addTest(
                new RecurTest(recur, start, end,
                              false,
                              TimeZone.getTimeZone("America/Los_Angeles")));

        // testFriday13Recur..
        recur = fromRule("FREQ=MONTHLY;BYDAY=FR;BYMONTHDAY=13");

        cal = Calendar.getInstance();
        cal.clear(Calendar.SECOND);
        cal.set(1997, 0, 1);
        start = dateOnly(cal);
        cal.set(2000, 0, 1);
        end = dateOnly(cal);

        suite.addTest(new RecurTest(recur, start, end, true, Calendar.DAY_OF_MONTH, 13));
        suite.addTest(new RecurTest(recur, start, end, true, Calendar.DAY_OF_WEEK, Calendar.FRIDAY));

        // testNoFrequency..
        suite.addTest(new RecurTest("BYDAY=MO,TU,WE,TH,FR"));

        // testUnknownFrequency..
        suite.addTest(new RecurTest("FREQ=FORTNIGHTLY;BYDAY=MO,TU,WE,TH,FR"));

        // various invalid values
        suite.addTest(new RecurTest("FREQ=YEARLY;BYMONTH=0"));
        suite.addTest(new RecurTest("FREQ=YEARLY;BYMONTHDAY=-400"));
        suite.addTest(new RecurTest(""));
        suite.addTest(new RecurTest("WEEKLY"));
        suite.addTest(new RecurTest("FREQ"));
        suite.addTest(new RecurTest("FREQ=WEEKLY;BYDAY=xx"));

        // Unit test for recurrence every 4th february..
        recur = fromRule("FREQ=YEARLY;BYMONTH=2;BYMONTHDAY=4;" +
                                 "BYDAY=MO,TU,WE,TH,FR,SA,SU");

        cal = Calendar.getInstance();
        cal.clear(Calendar.SECOND);
        cal.set(2006, 3, 10);
        start = dateOnly(cal);
        cal.set(2008, 1, 6);
        end = dateOnly(cal);

        suite.addTest(new RecurTest(recur, start, end, true, Calendar.DAY_OF_MONTH, 4));
        suite.addTest(new RecurTest(recur, start, end, true, Calendar.MONTH, 1));

        // Unit test for recurrence generation where seed month-date specified is
        // not valid for recurrence instances (e.g. feb 31).
        recur = fromRule("FREQ=YEARLY;BYMONTH=2;BYMONTHDAY=4;" +
                                 "BYDAY=MO,TU,WE,TH,FR,SA,SU");

        cal = Calendar.getInstance();
        cal.clear(Calendar.SECOND);
        cal.set(2006, 11, 31);
        start = dateOnly(cal);
        cal.set(2008, 11, 31);
        end = dateOnly(cal);

        suite.addTest(new RecurTest(recur, start, end, true, 2));
        suite.addTest(new RecurTest(recur, start, end, true, Calendar.DAY_OF_MONTH, 4));
        suite.addTest(new RecurTest(recur, start, end, true, Calendar.MONTH, 1));

        // Unit test for recurrence representing each half-hour..
        recur = fromRule("FREQ=DAILY;BYHOUR=9,10,11,12,13,14,15,16;" +
                                 "BYMINUTE=0,30");

        cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2019, 2, 14, 8, 0, 0);
        start = dateTime(cal);
        cal.add(Calendar.DAY_OF_YEAR, 1);
        end = dateTime(cal);

        suite.addTest(new RecurTest(recur, start, end, false, 16));

        // Test creation of recur instances..
        final WeekDayList expectedDayList = new WeekDayList();
        expectedDayList.add(new WeekDay(MO, 3));

        suite.addTest(new RecurTest(
                "FREQ=MONTHLY;INTERVAL=2;BYDAY=3MO",
                Frequency.MONTHLY, 2, expectedDayList));

        // testCountMonthsWith31Days..
        recur = fromRule("FREQ=MONTHLY;BYMONTHDAY=31");
        cal = Calendar.getInstance();
        start = dateTime(cal);
        cal.add(Calendar.YEAR, 1);
        end = dateTime(cal);

        suite.addTest(new RecurTest(recur, start, end, true, 7));

        // Ensure the first result from getDates is the same as getNextDate..
        recur = fromRule("FREQ=WEEKLY;WKST=MO;INTERVAL=3;BYDAY=MO,WE,TH");
        seed = dateTime("20081103T070000");
        final Occurrence periodStart = dateTime("20081109T210000Z");
        final Occurrence periodEnd = dateTime("20100104T210000Z");

        final Locale currentLocale = Locale.getDefault();
        final Occurrence getDatesFirstResult;
        try {
            Locale.setDefault(testLocale);
            getDatesFirstResult = recur.getDates(seed,
                                                 periodStart,
                                                 periodEnd).get(0);
        } finally {
            Locale.setDefault(currentLocale);
        }

        suite.addTest(new RecurTest(recur, seed, periodStart,
                                    getDatesFirstResult));

        recur = fromRule("FREQ=WEEKLY;BYDAY=MO");
        suite.addTest(new RecurTest(recur,
                                    dateOnly("20081212"),
                                    dateOnly("20081211"),
                                    dateOnly("20081215")));

        recur = fromRule("FREQ=YEARLY;BYMONTH=4;BYDAY=1SU");
        suite.addTest(new RecurTest(recur,
                                    seed,
                                    periodStart,
                                    dateTime("20090405T070000")));

        // rrule never matching any candidate  - should reach limit
        recur = fromRule("FREQ=DAILY;COUNT=60;BYDAY=TU,TH;BYSETPOS=2");
        suite.addTest(new RecurTest(recur, seed, start, end, true, 0));

        // rrule with negative bymonthday
        recur = fromRule("FREQ=YEARLY;COUNT=4;INTERVAL=2;BYMONTH=1,2,3;BYMONTHDAY=-1");
        suite.addTest(new RecurTest(recur,
                                    seed,
                                    periodStart,
                                    dateTime("20100131T070000")));

        // rather uncommon rule
        recur = fromRule("FREQ=YEARLY;BYWEEKNO=1,2,3,4");
        suite.addTest(new RecurTest(recur,
                                    dateTime("20130101T120000Z"),
                                    dateTime("20130101T120000Z"),
                                    dateTime("20130123T120000Z"),
                                    false, 4));
        suite.addTest(new RecurTest(recur,
                                    dateTime("20130101T120000Z"),
                                    dateTime("20160101T120000Z"),
                                    dateTime("20160123T120000Z"),
                                    false, 3));

        recur = fromRule("FREQ=DAILY;COUNT=3;INTERVAL=1;BYDAY=MO,TU,WE,TH,FR");
        suite.addTest(new RecurTest(recur,
                                    dateTime("20131215T000000Z"),
                                    dateTime("20131215T000000Z"),
                                    dateTime("20180101T120000Z"),
                                    false, 3));

        // rrule with bymonth and count. Should return correct number of occurrences near the end of its perioud.
        recur = fromRule("FREQ=MONTHLY;COUNT=3;INTERVAL=1;BYMONTH=1,9,10,12;BYMONTHDAY=12");
        suite.addTest(new RecurTest(recur,
                                    dateTime("20150917T000000Z"),
                                    dateTime("20160101T000000Z"),
                                    dateTime("20160201T000000Z"),
                                    true, 1));
        suite.addTest(new RecurTest(recur,
                                    dateTime("20150917T000000Z"),
                                    dateTime("20160201T000000Z"),
                                    dateTime("20160301T000000Z"),
                                    true, 0));

        // rrule with bymonth, byday and bysetpos. Issue #39
        recur = fromRule("FREQ=MONTHLY;WKST=MO;INTERVAL=1;" +
                                 "BYMONTH=2,3,9,10;" +
                                 "BYMONTHDAY=28,29,30,31;BYSETPOS=-1");
        suite.addTest(new RecurTest(recur,
                                    dateTime("20150701T000000"),
                                    dateTime("20150701T000000"),
                                    dateTime("20150930T000000")));

        // test getting valid recurrence at tip of smart increment
        // feb 29 2020 monthly with only valid month by february should return feb 28 2021
        recur = fromRule("FREQ=MONTHLY;BYMONTH=2;INTERVAL=1");
        suite.addTest(new RecurTest(recur,
                                    dateTime("20200229T000000"),
                                    dateTime("20200229T000000"),
                                    dateTime("20240229T000000")));

        // test hitting limit when getting invalid next recurrence
        recur = fromRule("FREQ=MONTHLY;BYMONTH=2;BYMONTHDAY=30;INTERVAL=1");
        suite.addTest(new RecurTest(recur,
                                    dateTime("20200229T000000"),
                                    dateTime("20200229T000000"),
                                    null));

        // test hitting leap year appropriately
        recur = fromRule("FREQ=YEARLY;BYMONTHDAY=29;INTERVAL=1");
        suite.addTest(new RecurTest(recur,
                                    dateTime("20200229T000000"),
                                    dateTime("20200229T000000"),
                                    dateTime("20240229T000000")));

        // test correct hit on first incrementation
        recur = fromRule("FREQ=YEARLY;INTERVAL=4");
        suite.addTest(new RecurTest(recur,
                                    dateTime("20200229T000000"),
                                    dateTime("20200229T000000"),
                                    dateTime("20240229T000000")));

        // last working day starting from may 31 2020 should return jun 30 2020
        recur = fromRule("FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=-1");
        suite.addTest(new RecurTest(recur,
                                    dateTime("20200531T000000"),
                                    dateTime("20200531T000000"),
                                    dateTime("20200630T000000")));

        // 5th sunday monthly starting from aug 31 2020 should return nov 29 2020
        recur = fromRule("FREQ=MONTHLY;BYDAY=SU;BYSETPOS=5");
        suite.addTest(new RecurTest(recur,
                                    dateTime("20200831T000000"),
                                    dateTime("20200831T000000"),
                                    dateTime("20201129T000000")));

        return suite;
    }

    private static final DateFormat dateOnlyFormat = new SimpleDateFormat("yyyyMMdd");

    private static Occurrence dateOnly(final String date) {
        try {
            return new Occurrence(dateOnlyFormat.parse(date),
                                  null, true, false);
        } catch (final ParseException pe) {
            throw new IllegalArgumentException(pe);
        }
    }

    private static Occurrence dateOnly(final Calendar cal) {
        return Occurrence.getInstance(cal, true, false);
    }

    private static final DateFormat dateTimeFormat =
            new SimpleDateFormat("yyyyMMdd'T'HHmmss");

    private static Occurrence dateTime(final String date) {
        final TimeZone tz;
        if (date.endsWith("Z")) {
            tz = TimeZone.getTimeZone(TimeZones.UTC_ID);
        } else {
            tz = TimeZone.getDefault();
        }

        try {
            return new Occurrence(dateTimeFormat.parse(date),
                                  tz, false, false);
        } catch (final ParseException pe) {
            throw new IllegalArgumentException(pe);
        }
    }

    private static Occurrence dateTime(final Calendar cal) {
        return Occurrence.getInstance(cal, false, false);
    }

    private static Occurrence dateTime(final java.util.Date date,
                                       final TimeZone tz) {
        return Occurrence.getInstance(date, tz, false, false);
    }

    private static Occurrence utc(final Calendar cal) {
        return Occurrence.getInstance(cal, false, true);
    }

    private static Recur fromRule(final String rrule) {
        final RecurResult rres = Recur.fromIcalendar(rrule, false);
        assertEquals("Expected OK: message was " +
                             rres.getMessage(),
                     Recur.RecurStatus.Ok,
                     rres.getStatus());

        return rres.getRecur();
    }
}
