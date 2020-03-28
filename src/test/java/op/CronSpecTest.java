package op;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CronSpecTest {

    @Test
    public void constructor_and_spec_argument() {
        new CronSpec("* * * *");
    }

    @Test
    public void getNextTime() {
        testNextTime("* * * *", "2020-03-27T11:00:00", "2020-03-27T11:01:00");
    }

    @Test
    public void getNextTime_extra_seconds() {
        testNextTime("* * * *", "2020-03-27T11:00:30", "2020-03-27T11:01:00");
    }

    @Test
    public void getNextTime_extra_nanos() {
        testNextTime("* * * *", "2020-03-27T11:00:00.123456", "2020-03-27T11:01:00");
    }

    @Test
    public void getNextTime_fixed_minutes() {
        testNextTime("30 * * *", "2020-03-27T11:00", "2020-03-27T11:30");
    }

    @Test
    public void getNextTime_fixed_minutes_passed_already() {
        testNextTime("30 * * *", "2020-03-27T11:45", "2020-03-27T12:30");
    }

    @Test
    public void getNextTime_fixed_minutes_exactly_same() {
        testNextTime("30 * * *", "2020-03-27T11:30", "2020-03-27T12:30");
    }

    @Test
    public void getNextTime_fixed_hours_and_minutes_before() {
        testNextTime("15 19 * *", "2020-03-27T11:30", "2020-03-27T19:15");
    }

    @Test
    public void getNextTime_fixed_hours_and_minutes_after_() {
        testNextTime("15 19 * *", "2020-03-27T20:15", "2020-03-28T19:15");
    }

    @Test
    public void getNextTime_fixed_hours_any_minutes_before() {
        testNextTime("* 19 * *", "2020-03-27T11:30", "2020-03-27T19:00");
    }

    @Test
    public void getNextTime_fixed_days_any_hours_any_days_before() {
        testNextTime("* * 2 *", "2020-03-27T11:30", "2020-04-02T00:00");
    }

    @Test
    public void getNextTime_fixed_months() {
        testNextTime("50 10 * 3", "2020-03-01T00:00", "2020-03-01T10:50");
        testNextTime("50 10 * 3", "2020-03-01T10:50", "2020-03-02T10:50");
    }

    @Test
    public void getNextTime_only_leap_years() {
        testNextTime("0 0 29 2", "2020-01-01T00:00", "2020-02-29T00:00");
        testNextTime("0 0 29 2", "2021-01-01T00:00", "2024-02-29T00:00");
    }

    @Test
    public void getNextTime_31th_day() {
        testNextTime("0 0 31 *", "2020-01-01T00:00", "2020-01-31T00:00");
        testNextTime("0 0 31 *", "2020-02-01T00:00", "2020-03-31T00:00");
    }

    @Test
    public void getNextTime_yearly() {
        testNextTime("0 0 31 1", "2020-01-01T00:00", "2020-01-31T00:00");
        testNextTime("0 0 31 1", "2020-01-31T00:00", "2021-01-31T00:00");
    }

    @Test(expected = IllegalArgumentException.class)
    public void impossible_day_of_month_any_month() {
        // April has 30 days
        new CronSpec("* * 32 *");
    }

    @Test(expected = IllegalArgumentException.class)
    public void impossible_day_of_month_fixed_month() {
        // April has 30 days
        new CronSpec("* * 31 4");
    }

    @Test
    public void iterateNextTimes() {
        Iterable<LocalDateTime> iterable = new CronSpec("15 * * *")
                .nextTimes(time("2020-01-01T00:00"));
        Iterator<LocalDateTime> it = iterable.iterator();
        assertTrue(it.hasNext());
        assertEquals(time("2020-01-01T00:15"), it.next());
        assertEquals(time("2020-01-01T01:15"), it.next());
        assertEquals(time("2020-01-01T02:15"), it.next());
        assertEquals(time("2020-01-01T03:15"), it.next());
    }

    @Test
    public void streamNextTimes() {
        Stream<LocalDateTime> stream = new CronSpec("15 * * *")
                .streamNextTimes(time("2020-01-01T00:00"));
        List<LocalDateTime> next = stream.limit(4).collect(Collectors.toList());
        assertEquals(Arrays.asList(
                time("2020-01-01T00:15"),
                time("2020-01-01T01:15"),
                time("2020-01-01T02:15"),
                time("2020-01-01T03:15")
        ), next);
    }

    public void testNextTime(String spec, String time, String expected) {
        LocalDateTime next = new CronSpec(spec)
            .getNextTime(time(time));
        assertEquals(time(expected), next);
    }

    private static LocalDateTime time(String time) {
        return LocalDateTime.parse(time);
    }

}
