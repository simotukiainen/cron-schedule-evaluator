Cron-schedule-evaluator
#######################


Cron expression parser and evaluator for Java is a small exercise I worked out
for fun.

It is archived here for sharing purposes. I may expand on it later as many of
the other evaluators I found were either part of a larger scheduling library
(that one might not need) or were difficult to understand.

You should not use this code for anything serious, but if you need to implement
something yourself, it may help you.

Currently it only supports either fixed or * flags, and those only with four
fields i.e minute, hour, day and month. Day of week field, ranges, fractions
and lists are not supported yet, but the code is such that it would be pretty
easy to add any of them.
