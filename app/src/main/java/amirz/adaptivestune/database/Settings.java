package amirz.adaptivestune.database;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
    /**
     * Globally used preferences.
     * @param context Context instance used to retrieve the {@link SharedPreferences} instance.
     * @return Single {@link SharedPreferences} instance that is used by the application.
     */
    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
    }
}
