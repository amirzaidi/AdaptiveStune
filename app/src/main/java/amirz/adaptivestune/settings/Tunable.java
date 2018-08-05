package amirz.adaptivestune.settings;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.TypedValue;

import java.util.ArrayList;
import java.util.List;

import amirz.adaptivestune.R;

public class Tunable {
    private final static List<Ref> sTunables = new ArrayList<>();

    public static final Tunable.BooleanRef INPUT_BOOST_ENABLED =
            new Tunable.BooleanRef(R.string.pref_input_boost_enabled,
                    R.bool.default_input_boost_enabled);

    public static final Tunable.IntegerRef INPUT_BOOST_MS =
            new Tunable.IntegerRef(R.string.pref_input_boost_ms,
                    R.integer.default_input_boost_ms);

    public static final Tunable.IntegerRef INPUT_BOOST_FREQ_LITTLE =
            new Tunable.IntegerRef(R.string.pref_input_boost_freq_little,
                    R.integer.default_input_boost_freq_little);

    public static final Tunable.IntegerRef INPUT_BOOST_FREQ_BIG =
            new Tunable.IntegerRef(R.string.pref_input_boost_freq_big,
                    R.integer.default_input_boost_freq_big);

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

    public static final Tunable.IntegerRef MIN_FRAMES =
            new Tunable.IntegerRef(R.string.pref_measurement_min_frames,
                    R.integer.default_measurement_min_frames);

    public static final Tunable.IntegerRef MIN_DURATION =
            new Tunable.IntegerRef(R.string.pref_measurement_min_duration,
                    R.integer.default_measurement_min_duration);

    public static final Tunable.IntegerRef DECAY_TRIGGER =
            new Tunable.IntegerRef(R.string.pref_measurement_decay_trigger,
                    R.integer.default_measurement_decay_trigger);

    public static final Tunable.IntegerRef DECAY_KEEP =
            new Tunable.IntegerRef(R.string.pref_measurement_decay_keep,
                    R.integer.default_measurement_decay_keep);

    // Frame time target for all percentiles.
    public static final Tunable.IntegerRef TARGET_FRAME_TIME_MS =
            new Tunable.IntegerRef(R.string.pref_target_frame_time_ms,
                    R.integer.default_target_frame_time_ms);

    // Influence on the jank factor is stronger when these are higher.
    public static final Tunable.FloatRef PERC_90_TARGET_WEIGHT =
            new Tunable.FloatRef(R.string.pref_perc_90_target_weight,
                    R.integer.default_perc_90_target_weight);

    public static final Tunable.FloatRef PERC_95_TARGET_WEIGHT =
            new Tunable.FloatRef(R.string.pref_perc_95_target_weight,
                    R.integer.default_perc_95_target_weight);

    public static final Tunable.FloatRef PERC_99_TARGET_WEIGHT =
            new Tunable.FloatRef(R.string.pref_perc_99_target_weight,
                    R.integer.default_perc_99_target_weight);

    // When this many different data points exist, use the parabola fitting algorithm.
    public static final Tunable.IntegerRef MIN_DATA_POINTS_PARABOLA =
            new Tunable.IntegerRef(R.string.pref_min_data_points_parabola,
                    R.integer.default_min_data_points_parabola);

    // When this many different data points exist, use the line fitting algorithm.
    public static final Tunable.IntegerRef MIN_DATA_POINTS_LINE =
            new Tunable.IntegerRef(R.string.pref_min_data_points_line,
                    R.integer.default_min_data_points_line);

    // Used to determine how important the latest result is for fallback algorithm.
    public static final Tunable.FloatRef DURATION_COEFFICIENT =
            new Tunable.FloatRef(R.string.pref_duration_coefficient,
                    R.integer.default_duration_coefficient);

    public static final Tunable.FloatRef DURATION_POW =
            new Tunable.FloatRef(R.string.pref_duration_pow,
                    R.integer.default_duration_pow);

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
        private BooleanRef(int settingId, int defaultId) {
            super(settingId, defaultId);
        }
    }

    public static class FloatRef extends Ref<Float> {
        private FloatRef(int settingId, int defaultId) {
            super(settingId, defaultId);
        }
    }

    public static class IntegerRef extends Ref<Integer> {
        private IntegerRef(int settingId, int defaultId) {
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
            sTunables.add(this);
        }
    }
}
