package amirz.adaptivestune.math;

public class Line {
    public static double root(double a, double b) {
        return -b / a;
    }

    public static double intersect(double a, double b, double y) {
        // Calculate intersection with line by shifting up/down and then taking the root.
        return root(a, b - y);
    }
}
