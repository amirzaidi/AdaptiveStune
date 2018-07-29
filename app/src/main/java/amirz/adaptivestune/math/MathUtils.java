package amirz.adaptivestune.math;

public class MathUtils {
    public static boolean between(double value, double min, double max) {
        return value != Double.NaN && value >= min && value <= max;
    }
}
