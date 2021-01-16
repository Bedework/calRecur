package net.fortuna.recur.util;

import com.ibm.icu.impl.TimeZoneAdapter;

public interface TimeZoneCache {

    TimeZoneAdapter getTimezone(String id);

    boolean putIfAbsent(String id, TimeZoneAdapter timeZone);

    boolean containsId(String id);

    void clear();
}
