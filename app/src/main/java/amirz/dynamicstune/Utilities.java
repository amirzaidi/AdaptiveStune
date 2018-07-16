package amirz.dynamicstune;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;

public class Utilities {
    public static final int DEFAULT_BOOST = 20;
    private static final String PREFIX = "boost_";

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
    }

    public static int getBoost(Context context, ComponentName component) {
        SharedPreferences prefs = prefs(context);

        // Default
        int boost = DEFAULT_BOOST;

        // Package
        boost = prefs.getInt(PREFIX + component.getPackageName(), boost);

        // Activity
        boost = prefs.getInt(PREFIX + component.flattenToShortString(), boost);

        return boost;
    }

    public static void setBoost(Context context, ComponentName component, int stune) {
        setBoost(context, component.flattenToShortString(), stune);
    }

    public static void setBoost(Context context, String string, int stune) {
        prefs(context).edit().putInt(PREFIX + string, stune).apply();
    }
}
