package amirz.adaptivestune.math;

public class MathUtils {
    public static boolean between(double value, double min, double max) {
        return !Double.isNaN(value) && value >= min && value <= max;
    }
}
