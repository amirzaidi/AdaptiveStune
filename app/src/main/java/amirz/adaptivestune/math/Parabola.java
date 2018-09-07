package amirz.adaptivestune.math;

public class Parabola {
    public static double[] root(double a, double b, double c) {
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

    public static double[] intersect(double a, double b, double c, double y) {
        // Calculate intersection with line by shifting up/down and then taking the roots.
        return root(a, b, c - y);
    }

    public static boolean derivatePositiveOnXRange(double a, double b, double minX, double maxX) {
        // Formula: y = ax2 + bx (+ c)
        // Either the parabola is a line with positive b
        if (a == 0) {
            return b > 0;
        }

        // or the derivative increases and its root is before minX,
        // or the derivative decreases and its root is after maxX.
        // Derivative: y' = 2ax + b
        double root = Line.root(2 * a, b);
        return (a > 0 && root < minX) || (a < 0 && root > maxX);
    }

    public static boolean derivateNegativeOnXRange(double a, double b, double minX, double maxX) {
        // Flip the parabola by multiplying by -1
        // This will flip the derivate because g(x) = -f(x) -> g'(x) = -f'(x)
        return derivatePositiveOnXRange(-a, -b, minX, maxX);
    }
}
