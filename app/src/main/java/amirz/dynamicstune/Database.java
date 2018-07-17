package amirz.dynamicstune;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

public class Database {
    public static final int IDLE_BOOST = 5;
    public static final int DEFAULT_BOOST = 20;
    public static final int MAX_BOOST = 50;

    public static final double JANK_FACTOR_QUICK_BOOST = 0.35;
    public static final double JANK_FACTOR_STEADY_INCREASE = 0.20;
    public static final double JANK_FACTOR_STEADY_DECREASE = 0.05;

    private static final String PREFIX = "boost_";
    private static final String DEFAULT_BOOST_PREF = "default_boost";

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
    }

    private static float getBoost(Context context, ComponentName component) {
        SharedPreferences prefs = prefs(context);

        // Default
        float boost = getDefaultBoost(context);

        // Package override
        boost = prefs.getFloat(PREFIX + component.getPackageName(), boost);

        // Activity override
        boost = prefs.getFloat(PREFIX + component.flattenToShortString(), boost);

        return boost;
    }

    // Can later be overwritten
    public static int getDefaultBoost(Context context) {
        return prefs(context).getInt(DEFAULT_BOOST_PREF, DEFAULT_BOOST);
    }

    public static int getBoostInt(Context context, ComponentName component) {
        return Math.round(getBoost(context, component));
    }

    public static float offsetBoost(Context context, ComponentName component, float offset) {
        // Previous boost
        float boost = Database.getBoost(context, component) + offset;

        // Ensure within bounds
        boost = Math.max(Database.IDLE_BOOST, boost);
        boost = Math.min(Database.MAX_BOOST, boost);

        // Save the boost for the current activity first.
        // Also keep track of applied boost for other unvisited activities.
        // This will be overwritten after the first launch of those activities.
        prefs(context).edit()
                .putFloat(PREFIX + component.flattenToShortString(), boost)
                .putFloat(PREFIX + component.getPackageName(), boost)
                .apply();

        return boost;
    }
}
