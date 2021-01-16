package net.fortuna.recur.util;

import com.ibm.icu.impl.TimeZoneAdapter;

import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

public class MapTimeZoneCache implements TimeZoneCache {
    private final Map<String, TimeZoneAdapter> mapCache;

    public MapTimeZoneCache() {
        mapCache = new ConcurrentHashMap<>();
    }

    @Override
    public TimeZoneAdapter getTimezone(final String id) {
        return mapCache.get(id);
    }

    @Override
    public boolean putIfAbsent(final String id,
                               final TimeZoneAdapter timeZone) {
        final TimeZone t = mapCache.get(id);
        if (t == null) {
            mapCache.put(id, timeZone);
            return true;
        }

        return false;
    }

    @Override
    public boolean containsId(final String id) {
        return mapCache.containsKey(id);
    }

    @Override
    public void clear() {
        mapCache.clear();
    }
}
