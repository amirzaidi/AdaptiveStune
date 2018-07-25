package amirz.dynamicstune;

import android.util.Log;
import android.util.SparseArray;

import java.util.List;

public class Algorithm {
    private static final String TAG = "Algorithm";

    public static class Measurement {
        public final int boost;
        public double total;
        public double janky;
        public double perc90;
        public double perc95;
        public double perc99;

        public Measurement(int boost) {
            this.boost = boost;
        }

        public double getJankFactor() {
            return janky / total;
        }
    }

    public static int getBoost(List<Measurement> measurements) {
        Log.d(TAG, "Measurement count " + measurements.size());

        int[] boostCount = new int[BoostDB.MAX_BOOST + 1];
        for (Measurement m : measurements) {
            boostCount[m.boost]++;
        }

        SparseArray<Measurement> sums = new SparseArray<>();

        // Initialize all measurement sum objects.
        for (int i = 0; i < boostCount.length; i++) {
            if (boostCount[i] != 0) {
                sums.put(i, new Measurement(i));
            }
        }

        // Count sums
        for (Measurement m : measurements) {
            Measurement sum = sums.get(m.boost);
            sum.total += m.total;
            sum.janky += m.janky;
            sum.perc90 += m.perc90;
            sum.perc95 += m.perc95;
            sum.perc99 += m.perc99;
        }

        // Normalize to averages
        for (int i = 0; i < boostCount.length; i++) {
            int count = boostCount[i];
            if (count != 0) {
                Measurement sum = sums.get(i);
                sum.total /= count;
                sum.janky /= count;
                sum.perc90 /= count;
                sum.perc95 /= count;
                sum.perc99 /= count;
            }
        }



        // ToDo: Use results
        return measurements.get(0).boost;
    }
}
