package amirz.dynamicstune.math;

public class Parabola {
    public static double[] intersect(double a, double b, double c, double y) {
        // Calculate intersection with line by shifting up/down.
        c -= y;

        double D = b * b - 4 * a * c;

        // Two solutions.
        if (D > 0) {
            double sqrt = Math.sqrt(D);
            double x1 = (-b + sqrt) / (2 * a);
            double x2 = (-b - sqrt) / (2 * a);
            return new double[] { x1, x2 };
        }

        // One solution.
        if (D == 0) {
            double x = -b / (2 * a);
            return new double[] { x };
        }

        // No solutions.
        return new double[0];
    }
}
