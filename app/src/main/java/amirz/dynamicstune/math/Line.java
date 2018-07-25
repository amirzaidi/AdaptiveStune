package amirz.dynamicstune.math;

public class Line {
    public static double intersect(double a, double b, double y) {
        if (a == 0) {
            // Line is equal to target or no solutions.
            return Double.NaN;
        }

        return (y - b) / a;
    }
}
