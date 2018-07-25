package amirz.dynamicstune.math;

import java.util.List;

public class Polynomial {
    public static class Point {
        public final double x;
        public final double y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private final double[] mCoefficients;

    // Source: http://www.bragitoff.com/2017/04/polynomial-fitting-java-codeprogram-works-android-well/
    public Polynomial(List<Point> points, int n) {
        int N = points.size();

        // Consecutive positions of the array will store N,sigma(xi),sigma(xi^2),sigma(xi^3)....sigma(xi^2n)
        double[] X = new double[2 * n + 1];
        for (int i = 0; i < 2 * n + 1; i++) {
            X[i] = 0;
            for (int j = 0; j < N; j++) {
                X[i] = X[i] + Math.pow(points.get(j).x, i);
            }
        }

        // B is the Normal matrix(augmented) that will store the equations, 'coefficients' is for value of the final coefficients
        double[][] augmentedNormals = new double[n + 1][n + 2];
        mCoefficients = new double[n + 1];

        for (int i = 0; i <= n; i++) {
            // Build the Normal matrix by storing the corresponding coefficients at the right positions except the last column of the matrix
            System.arraycopy(X, i, augmentedNormals[i], 0, n + 1);
        }

        // Array to store the values of sigma(yi),sigma(xi*yi),sigma(xi^2*yi)...sigma(xi^n*yi)
        double Y[] = new double[n + 1];
        for (int i = 0; i < n + 1; i++) {
            Y[i] = 0;
            for (int j = 0; j < N; j++) {
                // Consecutive positions will store sigma(yi),sigma(xi*yi),sigma(xi^2*yi)...sigma(xi^n*yi)
                Y[i] += Math.pow(points.get(j).x, i) * points.get(j).y;
            }
        }

        for (int i = 0; i <= n; i++) {
            // Load the values of Y as the last column of B(Normal Matrix but augmented)
            augmentedNormals[i][n + 1] = Y[i];
        }
        n++;

        // From now Gaussian Elimination starts (can be ignored) to solve the set of linear equations (Pivotisation)
        for (int i = 0; i < n; i++) {
            for (int k = i + 1; k < n; k++) {
                if (augmentedNormals[i][i] < augmentedNormals[k][i]) {
                    for (int j = 0; j <= n; j++) {
                        double temp = augmentedNormals[i][j];
                        augmentedNormals[i][j] = augmentedNormals[k][j];
                        augmentedNormals[k][j] = temp;
                    }
                }
            }
        }

        // Loop to perform the gauss elimination
        for (int i = 0; i < n - 1; i++) {
            for (int k = i + 1; k < n; k++) {
                double t = augmentedNormals[k][i] / augmentedNormals[i][i];
                for (int j = 0; j <= n; j++) {
                    // Make the elements below the pivot elements equal to zero or elimnate the variables
                    augmentedNormals[k][j] = augmentedNormals[k][j] - t * augmentedNormals[i][j];
                }
            }
        }

        // Back-substitution
        // x is an array whose values correspond to the values of x,y,z..
        for (int i = n - 1; i >= 0; i--) {
            // Make the variable to be calculated equal to the rhs of the last equation
            mCoefficients[i] = augmentedNormals[i][n];
            for (int j = 0; j < n; j++) {
                // Then subtract all the lhs values except the coefficient of the variable whose value is being calculated
                if (j != i) {
                    mCoefficients[i] -= augmentedNormals[i][j] * mCoefficients[j];
                }
            }
            // Now finally divide the rhs by the coefficient of the variable to be calculated
            mCoefficients[i] /= augmentedNormals[i][i];
        }
    }

    public int getCoefficientCount() {
        return mCoefficients.length;
    }

    public double getCoefficient(int i) {
        return mCoefficients[i];
    }
}
