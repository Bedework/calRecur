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
package net.fortuna.recur.util;

import com.ibm.icu.util.TimeZone;

/**
 * $Id$ [5/07/2004]
 *
 * Utility methods relevant to Java timezones.
 *
 * @author Ben Fortuna
 */
public final class TimeZones {
    
    /**
     * The timezone identifier for UTC time.
     */
    public static final String UTC_ID = "Etc/UTC";
    
    /**
     * The timezone identifier for UTC time in the IBM JVM.
     */
    public static final String IBM_UTC_ID = "GMT";
    
    /**
     * The timezone identifier for GMT time.
     */
    public static final String GMT_ID = "Etc/GMT";

    private static final TimeZone UTC_TIMEZONE;
    static {
        UTC_TIMEZONE = TimeZone.getTimeZone(UTC_ID);
    }

    /**
     * Constructor made private to enforce static nature.
     */
    private TimeZones() {
    }
    
    /**
     * Indicates whether the specified timezone is equivalent to
     * UTC time.
     * @param timezone a timezone instance
     * @return true if the timezone is UTC time, otherwise false
     */
    public static boolean isUtc(final TimeZone timezone) {
//        return timezone.hasSameRules(TimeZone.getTimeZone(UTC_ID));
//        return timezone.getRawOffset() == 0;
        return UTC_ID.equals(timezone.getID())
            || IBM_UTC_ID.equals(timezone.getID());
    }

    public static TimeZone getDefault() {
        return TimeZone.getDefault();
    }

    /**
     * @return the timezone used for date instances
     */
    public static TimeZone getDateTimeZone() {
    	return getUtcTimeZone();
    }

    /**
     * Get the UTC Timezone.
     */
    public static TimeZone getUtcTimeZone() {
        return UTC_TIMEZONE;
    }
}
