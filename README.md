# Calendar Recurrence Library

This library implements recurrences as defined in [RFC5545](https://tools.ietf.org/html/rfc5545) and will also implement Rscale as defined in [RFC7529](https://tools.ietf.org/html/rfc7529)

This library is independent of other calendar libraries such as ical4j so that it may be used by any library manipulating calendar data - for example support of jscalendar.

It is (at least initially) a reimplementation of the ical4j Recur classes. The tests were also reimplemented and the new classes pass all.

Obtaining a Recur class can be via the static **fromIcalendar** method or by direct calls to the builder.