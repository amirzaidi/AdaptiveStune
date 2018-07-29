package amirz.adaptivestune.database;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

import amirz.adaptivestune.R;
import amirz.adaptivestune.Tunable;

import static amirz.adaptivestune.database.Settings.prefs;

/**
 * Keeps track of previously calculated boost values and stores them in a {@link SharedPreferences}
 * instance for quick retrieval when an activity is launched again.
 */
public class Boost {
    /** Lowest boost possible, that is also applied when idle. */
    public static final Tunable.IntegerRef IDLE_BOOST =
            new Tunable.IntegerRef(R.string.pref_idle_boost,
                    R.integer.default_idle_boost);

    /** The default boost used for unvisited activities. */
    public static final Tunable.IntegerRef DEFAULT_BOOST =
            new Tunable.IntegerRef(R.string.pref_default_boost,
                    R.integer.default_default_boost);

    /** Highest boost possible, to prevent boosts from going up to 100. */
    public static final Tunable.IntegerRef MAX_BOOST =
            new Tunable.IntegerRef(R.string.pref_max_boost,
                    R.integer.default_max_boost);

    private static final String PREFIX = "boost_";

    public static void init() {}

    /**
     * Retrieves the last calculated boost for the component.
     * @param context Context instance used to retrieve the {@link SharedPreferences} object.
     * @param component Key for which boost to use.
     * @return Floating point boost value between IDLE_BOOST and MAX_BOOST.
     */
    public static float getBoost(Context context, ComponentName component) {
        return prefs(context).getFloat(PREFIX + component.flattenToShortString(), DEFAULT_BOOST.get());
    }

    /**
     * Retrieves the last calculated boost for the component and rounds it using the round method.
     * @param context Context instance used to retrieve the {@link SharedPreferences} object.
     * @param component Key for which the boost value is requested.
     * @return Integer boost value between IDLE_BOOST and MAX_BOOST.
     */
    public static int getBoostInt(Context context, ComponentName component) {
        return round(getBoost(context, component));
    }

    /**
     * Rounds a boost using a redistribution algorithm.
     * @param boost Boost value from the getBoost method.
     * @return Integer boost value.
     */
    public static int round(float boost) {
        // ToDo: Replace with redistribution algorithm.
        return Math.round(boost);
    }

    /**
     * Updates the boost for a component.
     * @param context Context instance used to retrieve the SharedPreferences object.
     * @param component Key for which the boost value is saved.
     * @param boost Floating point boost value.
     */
    public static void setBoost(Context context, ComponentName component, float boost) {
        // Ensure within bounds
        boost = Math.max(IDLE_BOOST.get(), boost);
        boost = Math.min(MAX_BOOST.get(), boost);

        // Save the new boost for the component.
        prefs(context).edit()
                .putFloat(PREFIX + component.flattenToShortString(), boost)
                .apply();
    }
}
