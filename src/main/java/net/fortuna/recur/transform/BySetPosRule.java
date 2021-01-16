package net.fortuna.recur.transform;

import net.fortuna.recur.NumberList;
import net.fortuna.recur.OccurrenceList;

import java.util.Collections;

/**
 * Applies BYSETPOS rules to <code>dates</code>. Valid positions are from 1 to the size of the date list. Invalid
 * positions are ignored.
 */
public class BySetPosRule implements Transformer {

    private final NumberList setPosList;

    public BySetPosRule(final NumberList setPosList) {
        this.setPosList = setPosList;
    }

    @Override
    public OccurrenceList transform(final OccurrenceList dates) {
        // return if no SETPOS rules specified..
        if (setPosList.isEmpty()) {
            return dates;
        }
        // sort the list before processing..
        Collections.sort(dates);
        final OccurrenceList setPosDates =
                OccurrenceList.getDateListInstance(dates);
        final int size = dates.size();
        for (final Integer setPos : setPosList) {
            final int pos = setPos;
            if (pos > 0 && pos <= size) {
                setPosDates.add(dates.get(pos - 1));
            } else if (pos < 0 && pos >= -size) {
                setPosDates.add(dates.get(size + pos));
            }
        }
        return setPosDates;
    }
}
