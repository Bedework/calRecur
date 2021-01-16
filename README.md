# Calendar Recurrence Library

## Motivation
This library was motivated by the following needs:

 * To implement [rscale](https://tools.ietf.org/html/rfc7529) 
 * To implement recurrences for JsCalendar
 * To implement [ISO style recurrences](https://standards.calconnect.org/csd/cc-18012.html)
 * Internal recurrence calculation in bedework.

## Summary
This library implements recurrences as defined in [RFC5545](https://tools.ietf.org/html/rfc5545) and will also implement Rscale as defined in [RFC7529](https://tools.ietf.org/html/rfc7529)

This library is independent of other calendar libraries such as ical4j so that it may be used by any library manipulating calendar data - for example support of jscalendar.

It is (at least initially) a reimplementation of the ical4j Recur classes. The tests were also reimplemented and the new classes pass all.

Obtaining a Recur class can be via the static **fromIcalendar** method or by direct calls to the builder.

## Acknowledgement
This library is a recasting of the work carried out by Ben Fortuna for the [ical4j](https://github.com/ical4j/ical4j) project without which this would have been a much more difficul;t proposition.

