package amirz.adaptivestune;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.TypedValue;

import java.util.ArrayList;
import java.util.List;

public class Tunable {
    private final static List<Ref> sTunables = new ArrayList<>();

    private static void register(Ref tunable) {
        sTunables.add(tunable);
    }

    public static void applyAll(SharedPreferences prefs, Resources res) {
        for (Ref tunable : sTunables) {
            apply(prefs, res, tunable);
        }
    }

    public static boolean apply(SharedPreferences prefs, Resources res, String key) {
        for (Ref tunable : sTunables) {
            if (key.equals(res.getString(tunable.settingId))) {
                apply(prefs, res, tunable);
                return true;
            }
        }
        return false;
    }

    private static void apply(SharedPreferences prefs, Resources res, Ref tunable) {
        String key = res.getString(tunable.settingId);
        TypedValue defaultValue = new TypedValue();
        res.getValue(tunable.defaultId, defaultValue, false);
        if (tunable instanceof BooleanRef) {
            tunable.value = prefs.getBoolean(key, defaultValue.data == 1);
        } else if (tunable instanceof FloatRef) {
            String defaultString = defaultValue.coerceToString().toString();
            tunable.value = Float.valueOf(prefs.getString(key, defaultString));
        } else if (tunable instanceof IntegerRef) {
            String defaultString = defaultValue.coerceToString().toString();
            tunable.value = Integer.valueOf(prefs.getString(key, defaultString));
        }
    }

    public static class BooleanRef extends Ref<Boolean> {
        public BooleanRef(int settingId, int defaultId) {
            super(settingId, defaultId);
        }
    }

    public static class FloatRef extends Ref<Float> {
        public FloatRef(int settingId, int defaultId) {
            super(settingId, defaultId);
        }
    }

    public static class IntegerRef extends Ref<Integer> {
        public IntegerRef(int settingId, int defaultId) {
            super(settingId, defaultId);
        }
    }

    private abstract static class Ref<T> {
        T value;
        final int settingId;
        final int defaultId;

        public T get() {
            return value;
        }

        Ref(int settingId, int defaultId) {
            this.settingId = settingId;
            this.defaultId = defaultId;
            register(this);
        }
    }
}
