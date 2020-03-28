package op;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.util.Iterator;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class CronSpec {

    private final ConstraintChain constraints;

    public CronSpec(String spec) {
        String[] parts = spec.split(" ");
        if (parts.length != 4)
            throw new IllegalArgumentException("Invalid spec: " + spec);

        SingleConstraint minutesConstraint = new SingleConstraint(Part.MINUTES, parts[0]);
        SingleConstraint hoursConstraint = new SingleConstraint(Part.HOURS, parts[1]);
        SingleConstraint daysConstraint = new SingleConstraint(Part.DAYS, parts[2]);
        SingleConstraint monthsConstraint = new SingleConstraint(Part.MONTHS, parts[3]);

        constraints = new ConstraintChain(minutesConstraint, hoursConstraint, daysConstraint, monthsConstraint);

        if (!daysConstraint.isAny()) {
            // Validate days of month
            if (monthsConstraint.isAny()) {
                // Day is enough to be valid in some month
                if (daysConstraint.getFixed() > Part.DAYS.getMaximum())
                    throw new IllegalArgumentException("Day " + daysConstraint.getFixed() + " is not valid in any month");
            } else {
                try {
                    MonthDay.of(monthsConstraint.getFixed(), daysConstraint.getFixed());
                } catch (DateTimeException e) {
                    throw new IllegalArgumentException("Day " + daysConstraint.getFixed() + " is never valid for month " + monthsConstraint.getFixed());
                }
            }
        }
    }

    public LocalDateTime getNextTime(LocalDateTime time) {
        // Clear ignored parts
        time = time.withSecond(0).withNano(0);

        // Move to next possible suitable moment according to resolution (minutes)
        time = time.plusMinutes(1);

        // Align the time i.e find smallest suitable moment if current one is not aligned
        time = constraints.align(time);

        return time;
    }

    public Iterable<LocalDateTime> nextTimes(LocalDateTime time) {
        return () -> new Iterator<>() {
            LocalDateTime currentTime = time;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public LocalDateTime next() {
                return currentTime = getNextTime(currentTime);
            }
        };
    }

    public Stream<LocalDateTime> streamNextTimes(LocalDateTime time) {
        return Stream.generate(new Supplier<>() {
            LocalDateTime currentTime = time;

            @Override
            public LocalDateTime get() {
                return currentTime = getNextTime(currentTime);
            }
        });
    }

    /**
     * Part handles the relation between java.time units and fields and our spec parts
     */
    private enum Part {
        MINUTES(ChronoField.MINUTE_OF_HOUR),
        HOURS(ChronoField.HOUR_OF_DAY),
        DAYS(ChronoField.DAY_OF_MONTH),
        MONTHS(ChronoField.MONTH_OF_YEAR),
        YEARS(ChronoField.YEAR),
        ;

        private final TemporalUnit unit;
        private final TemporalField field;

        Part(TemporalField field) {
            this.unit = field.getBaseUnit();
            this.field = field;
        }

        public Part getLarger() {
            return Part.values()[ordinal() + 1];
        }

        long getMaximum() {
            return field.range().getMaximum();
        }

        private long getMaximum(LocalDateTime time) {
            return time.range(field).getMaximum();
        }

        private int getMinimum() {
            return (int) field.range().getMinimum();
        }

        private LocalDateTime set(LocalDateTime time, int fixed) {
            return time.with(field, fixed);
        }

        private int get(LocalDateTime time) {
            return time.get(field);
        }

        private LocalDateTime increment(LocalDateTime time) {
            return time.plus(1, unit);
        }
    }

    private interface Constraint {
        boolean isAligned(LocalDateTime time);

        LocalDateTime align(LocalDateTime time);

        LocalDateTime reset(LocalDateTime time);
    }

    private static class SingleConstraint implements Constraint {
        private final Part part;
        private final int value;

        SingleConstraint(Part part, String spec) {
            this.part = part;
            if (spec.equals("*"))
                this.value = -1;
            else
                this.value = Integer.parseInt(spec);
        }

        public boolean isAny() {
            return value == -1;
        }

        public int getFixed() {
            if (value == -1)
                throw new IllegalStateException("Spec is not fixed");
            return value;
        }

        @Override
        public boolean isAligned(LocalDateTime time) {
            return isAny() || part.get(time) == getFixed();
        }

        @Override
        public LocalDateTime align(LocalDateTime time) {
            if (isAny())
                return time;
            if (part.get(time) > getFixed()) {
                // Larger parts roll
                time = part.getLarger().increment(time);
            }

            // This is for day constraints 29 (leap years), 31 (months with only 30 days)
            // We find next suitable larger unit i.e month
            while (part.getMaximum(time) < getFixed())
                time = part.getLarger().increment(time);

            time = part.set(time, getFixed());

            return time;
        }

        @Override
        public LocalDateTime reset(LocalDateTime time) {
            return part.set(time, part.getMinimum());
        }
    }

    private static class ConstraintChain implements Constraint {
        private final Constraint[] constraints;

        ConstraintChain(Constraint... constraints) {
            this.constraints = constraints;
        }

        @Override
        public LocalDateTime align(LocalDateTime time) {
            for (;;) {
                // Find the largest misaligned part
                Constraint largestMisaligned = findLargestMisaligned(time);

                // If everything is aligned we are done
                if (largestMisaligned == null)
                    return time;

                // We align the largest misaligned
                time = largestMisaligned.align(time);

                // And just reset the smaller parts and align them next round
                time = resetSmallerParts(time, largestMisaligned);
            }
        }

        private Constraint findLargestMisaligned(LocalDateTime time) {
            Constraint largestMisaligned = null;
            for (Constraint constraint : constraints) {
                if (!constraint.isAligned(time))
                    largestMisaligned = constraint;
            }
            return largestMisaligned;
        }

        private LocalDateTime resetSmallerParts(LocalDateTime time, Constraint largestMisaligned) {
            for (Constraint smaller : constraints) {
                if (smaller == largestMisaligned)
                    break;
                time = smaller.reset(time);
            }
            return time;
        }

        @Override
        public boolean isAligned(LocalDateTime time) {
            for (Constraint constraint : constraints) {
                if (!constraint.isAligned(time))
                    return false;
            }
            return true;
        }

        @Override
        public LocalDateTime reset(LocalDateTime time) {
            for (Constraint constraint : constraints) {
                time = constraint.reset(time);
            }
            return time;
        }
    }
}
