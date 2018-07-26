package amirz.dynamicstune.database;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

public class BoostDB {
    // ToDo: Make these constants tunable with a settings activity.
    public static final int IDLE_BOOST = 5;
    public static final int MAX_BOOST = 50;

    // ToDo: Make these constants tunable with a settings activity.
    private static final int DEFAULT_BOOST = 20;

    private static final String PREFIX = "boost_";
    private static final String DEFAULT_BOOST_PREF = "default_boost";

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
    }

    public static float getBoost(Context context, ComponentName component) {
        SharedPreferences prefs = prefs(context);

        // Default
        float boost = getDefaultBoost(context);

        // Package override
        boost = prefs.getFloat(PREFIX + component.getPackageName(), boost);

        // Activity override
        boost = prefs.getFloat(PREFIX + component.flattenToShortString(), boost);

        return boost;
    }

    public static int getBoostInt(float boost) {
        return Math.round(boost);
    }

    // Can later be overwritten
    public static int getDefaultBoost(Context context) {
        return prefs(context).getInt(DEFAULT_BOOST_PREF, DEFAULT_BOOST);
    }

    public static void setBoost(Context context, ComponentName component, float componentBoost, float packageBoost) {
        // Ensure within bounds
        componentBoost = Math.max(BoostDB.IDLE_BOOST, componentBoost);
        componentBoost = Math.min(BoostDB.MAX_BOOST, componentBoost);
        packageBoost = Math.max(BoostDB.IDLE_BOOST, packageBoost);
        packageBoost = Math.min(BoostDB.MAX_BOOST, packageBoost);

        // Save the boost for the current activity first.
        // Also keep track of applied boost for other unvisited activities.
        // This will be overwritten after the first launch of those activities.
        prefs(context).edit()
                .putFloat(PREFIX + component.flattenToShortString(), componentBoost)
                .putFloat(PREFIX + component.getPackageName(), packageBoost)
                .apply();
    }
}
