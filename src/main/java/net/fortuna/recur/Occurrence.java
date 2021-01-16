/* ********************************************************************
    Appropriate copyright notice
*/
package net.fortuna.recur;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;

import java.util.Date;

/**
 * User: mike Date: 1/8/21 Time: 13:10
 *
 * Represents an occurrence of a recurrence
 */
public class Occurrence implements Comparable<Occurrence> {
  private Date date;
  private boolean isDateOnly;

  private TimeZone timeZone;

  private boolean utc;

  public static Occurrence getInstance(
          final Calendar cal,
          final boolean dateOnly,
          final boolean utc) {
    return new Occurrence(cal.getTime(),
                          cal.getTimeZone(),
                          dateOnly,
                          utc);
  }

  public static Occurrence getInstance(
          final Date date,
          final TimeZone timeZone,
          final boolean dateOnly,
          final boolean utc) {
    return new Occurrence(date, timeZone, dateOnly, utc);
  }

  public static Occurrence getInstanceLike(
          final Date date,
          final Occurrence likeThis) {
    return new Occurrence(date, likeThis.getTimeZone(),
                          likeThis.getDateOnly(),
                          likeThis.getUtc());
  }

  public Occurrence(final Date date,
                    final TimeZone timeZone,
                    final boolean dateOnly,
                    final boolean utc) {
    this.date = date;
    this.timeZone = timeZone;
    this.isDateOnly = dateOnly;
    this.utc = utc;
  }

  public void setTimeZone(final TimeZone val) {
    timeZone = val;
    utc = val == null;
  }

  public TimeZone getTimeZone() {
    return timeZone;
  }

  public void setUtc(final boolean val) {
    if (!val) {
      timeZone = null;
    }
    utc = val;
  }

  public boolean getUtc() {
    return utc;
  }

  public boolean getDateOnly() {
    return isDateOnly;
  }

  public Date getDate() {
    return date;
  }

  public void setCalendarTime(final Calendar cal) {
    cal.setTime(getDate());
    if (getDateOnly()) {
      cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
      cal.clear(Calendar.MINUTE);
      cal.clear(Calendar.SECOND);
      cal.clear(Calendar.MILLISECOND);
    }
  }
  public boolean after(final Occurrence that) {
    return this.getDate().after(that.getDate());
  }

  public boolean after(final Date that) {
    return this.getDate().after(that);
  }

  public boolean before(final Occurrence that) {
    return this.getDate().before(that.getDate());
  }

  @Override
  public int compareTo(final Occurrence o) {
    if (getUtc() && !o.getUtc()) {
      return 1;
    }

    if (!getUtc() && o.getUtc()) {
      return -1;
    }

    return getDate().compareTo(o.getDate());
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof Occurrence)) {
      return false;
    }

    return ((Occurrence)o).getDate().equals(getDate());
  }

  public int hashCode() {
    return getDate().hashCode();
  }
}
