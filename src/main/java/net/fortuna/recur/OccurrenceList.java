/* ********************************************************************
    Appropriate copyright notice
*/
package net.fortuna.recur;

import com.ibm.icu.util.TimeZone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: mike Date: 1/8/21 Time: 13:10
 *
 * Represents an occurrence of a recurrence
 */
public class OccurrenceList extends ArrayList<Occurrence> {
//        implements List<Occurrence>, Serializable, Iterable<Occurrence> {
  private TimeZone timeZone;

  private boolean utc;

  private boolean dateOnly;

  public OccurrenceList(final boolean dateOnly) {
    this.dateOnly = dateOnly;
  }

  /**
   * Instantiate a new occurrence list with the same type, timezone and utc settings
   * as the origList.
   *
   * @param origList
   * @return a new empty list.
   */
  public static OccurrenceList getDateListInstance(
          final OccurrenceList origList) {
    final OccurrenceList list = new OccurrenceList(origList.getDateOnly());
    if (origList.getUtc()) {
      list.setUtc(true);
    } else {
      list.setTimeZone(origList.getTimeZone());
    }

    return list;
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

  public void setDateOnly(final boolean val) {
    dateOnly = val;
  }

  public boolean getUtc() {
    return utc;
  }

  public boolean getDateOnly() {
    return dateOnly;
  }

  /**
   * Add a date to the list. The date will be updated to reflect the timezone of this list.
   *
   * @param val
   *            the date to add
   * @return true
   * @see List#add(java.lang.Object)
   */
  @Override
  public final boolean add(final Occurrence val) {
    if (!getUtc() && getTimeZone() == null) {
      /* If list hasn't been initialized yet use defaults from the first added element */
      if (!val.getDateOnly()) {
        setDateOnly(false);
        if (val.getUtc()) {
          this.setUtc(true);
        } else {
          setTimeZone(val.getTimeZone());
        }
      } else {
        setDateOnly(true);
      }
    }

    if (!val.getDateOnly()) {
      if (getUtc()) {
        val.setUtc(true);
      } else {
        val.setTimeZone(getTimeZone());
      }
    }

    return super.add(val);
  }

  public void sort() {
    Collections.sort(this);
  }

  /*

  @Override
  public final int size() {
    return occurrences.size();
  }

  @Override
  public final boolean isEmpty() {
    return occurrences.isEmpty();
  }

  @Override
  public final Iterator<Occurrence> iterator() {
    return occurrences.iterator();
  }

  @Override
  public final boolean contains(final Object o) {
    return occurrences.contains(o);
  }

  @Override
  public final boolean containsAll(final Collection<?> arg0) {
    return occurrences.containsAll(arg0);
  }

  @Override
  public final Object[] toArray() {
    return occurrences.toArray();
  }

  @Override
  public final <T> T[] toArray(final T[] arg0) {
    return occurrences.toArray(arg0);
  }

  @Override
  public final boolean remove(final Object o) {
    return occurrences.remove(o);
  }
   */
}
