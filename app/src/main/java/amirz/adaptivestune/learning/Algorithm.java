package amirz.adaptivestune.learning;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import amirz.adaptivestune.database.Boost;
import amirz.adaptivestune.math.Line;
import amirz.adaptivestune.math.Parabola;
import amirz.adaptivestune.math.Polynomial;

import static amirz.adaptivestune.math.MathUtils.between;
import static amirz.adaptivestune.settings.Tunable.*;

/**
 * This class takes measurements into account and returns a new boost value to try.
 * From that selected boost value new measurements are done, and eventually all data is
 * inserted into a polynomial. The class then selects a boost from this polynomial.
 * The goal is to get the parseJankFactor method to return zero, meaning, there is no jank.
 * Note that this does not directly imply there are no frame drops, but that they are insignificant.
 */
public class Algorithm {
    private static final String TAG = "Algorithm";

    /**
     * Subclass that acts as a data wrapper for a measurement or an aggregate of measurements.
     */
    public static class Measurement extends GfxInfo.Measurement {
        public final float boost;

        public Measurement(float boost) {
            this.boost = boost;
        }
    }

    public static double getBoost(List<Measurement> measurements) {
        return getBoost(measurements, IDLE_BOOST.get(), MAX_BOOST.get(),
                MIN_DATA_POINTS_LINE.get(), MIN_DATA_POINTS_PARABOLA.get());
    }

    private static double getBoost(List<Measurement> measurements, int idleBoost, int maxBoost,
                                   int minPointsLine, int minPointsParabola) {
        // Save the min and max for further value checking.
        int minMeasuredBoost = maxBoost;
        int maxMeasuredBoost = idleBoost;

        Set<Integer> boosts = new HashSet<>();
        List<Polynomial.Point> points = new ArrayList<>();

        for (Measurement m : measurements) {
            int effectiveBoost = Boost.roundToInteger(m.boost);

            minMeasuredBoost = Math.min(minMeasuredBoost, effectiveBoost);
            maxMeasuredBoost = Math.max(maxMeasuredBoost, effectiveBoost);

            // Transform the data set into points for the Polynomial class.
            boosts.add(effectiveBoost);
            points.add(new Polynomial.Point(effectiveBoost, getJankTargetOffset(m)));
        }

        if (boosts.size() >= minPointsParabola) {
            double[] intersect = getBoostFromParabola(points);
            if (intersect.length == 2) {
                // Never extrapolate, only interpolate.
                boolean firstBetween = between(intersect[0], minMeasuredBoost, maxMeasuredBoost);
                boolean secondBetween = between(intersect[1], minMeasuredBoost, maxMeasuredBoost);
                if (firstBetween && !secondBetween) {
                    Log.d(TAG, "Parabola result 1: boost = " + intersect[0]);
                    return intersect[0];
                }
                if (!firstBetween && secondBetween) {
                    Log.d(TAG, "Parabola result 2: boost = " + intersect[1]);
                    return intersect[1];
                }
            }
        }

        if (boosts.size() >= minPointsLine) {
            double intersect = getBoostFromLine(points);
            if (between(intersect, minMeasuredBoost, maxMeasuredBoost)) {
                Log.d(TAG, "Line result: boost = " + intersect);
                return intersect;
            }
        }

        // Fallback implementation when not enough data has been collected.
        // Adjust based on the last data point.
        Measurement m = measurements.get(measurements.size() - 1);
        double offset = getJankTargetOffset(m) * getDurationFactor(m);
        double boost = m.boost + offset;
        Log.d(TAG, "Point result: boost = " + offset + " " + boost);
        return boost;
    }

    public static double[] getBoostFromParabola(List<Polynomial.Point> points) {
        Polynomial parabola = new Polynomial(points, 2);

        double a = parabola.getCoefficient(2);
        double b = parabola.getCoefficient(1);
        double c = parabola.getCoefficient(0);

        Log.w(TAG, "Parabola: " + a + " " + b + " " + c);

        return Parabola.root(a, b, c);
    }

    public static double getBoostFromLine(List<Polynomial.Point> points) {
        Polynomial line = new Polynomial(points, 1);

        double a = line.getCoefficient(1);
        double b = line.getCoefficient(0);

        Log.d(TAG, "Line: " + a + " " + b);

        // Line data does not make sense if this is not negative, because there should be a
        // downwards trend for jank values at higher boosts.
        return a < 0
                ? Line.root(a, b)
                : Double.NaN;
    }

    /**
     * Converts a measurement into a single factor that judges the smoothness.
     * @param measurement The measurement from which to create a factor.
     * @return Zero when the frame rate is equal to the target.
     * A positive value when it is too janky, a negative value when it is too smooth.
     */
    public static double getJankTargetOffset(Measurement measurement) {
        return getJankTargetOffset(measurement, TARGET_FRAME_TIME_MS.get(),
                PERC_90_TARGET_WEIGHT.get(), PERC_95_TARGET_WEIGHT.get(),
                PERC_99_TARGET_WEIGHT.get());
    }

    public static double getJankTargetOffset(Measurement measurement, int targetFrameTime,
                                              float weightPerc90, float weightPerc95,
                                              float weightPerc99) {
        return measurement.janky / measurement.total +
                (measurement.perc90 - targetFrameTime) * weightPerc90 +
                (measurement.perc95 - targetFrameTime) * weightPerc95 +
                (measurement.perc99 - targetFrameTime) * weightPerc99;
    }

    public static double getDurationFactor(Measurement measurement) {
        return getDurationFactor(measurement, DURATION_COEFFICIENT.get(), DURATION_POW.get());
    }

    public static double getDurationFactor(Measurement measurement, float durationCoefficient,
                                            float durationPow) {
        return durationCoefficient * Math.pow(measurement.total, durationPow);
    }
}
