package amirz.dynamicstune;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

public class Utilities {
    public static final int IDLE_BOOST = 5;
    public static final int DEFAULT_BOOST = 20;
    public static final int MAX_BOOST = 50;

    public static final int JANK_FACTOR_MIN_FRAMES = 90;
    public static final double JANK_FACTOR_QUICK_BOOST = 0.35;
    public static final double JANK_FACTOR_STEADY_INCREASE = 0.20;
    public static final double JANK_FACTOR_STEADY_DECREASE = 0.05;

    private static final String PREFIX = "boost_";

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
    }

    public static int getBoost(Context context, ComponentName component) {
        SharedPreferences prefs = prefs(context);

        // Default
        int boost = DEFAULT_BOOST;

        // Package override
        boost = prefs.getInt(PREFIX + component.getPackageName(), boost);

        // Activity override
        boost = prefs.getInt(PREFIX + component.flattenToShortString(), boost);

        return boost;
    }

    public static void setBoost(Context context, ComponentName component, int boost) {
        // Save the boost for the current activity first.
        setBoost(context, component.flattenToShortString(), boost);

        // Also keep track of applied boost for other unvisited activities.
        // This will be overwritten after the first launch of those activities.
        setBoost(context, component.getPackageName(), boost);
    }

    private static void setBoost(Context context, String string, int boost) {
        prefs(context).edit()
                .putInt(PREFIX + string, boost)
                .apply();
    }
}
