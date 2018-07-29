package amirz.adaptivestune;

import android.util.Log;

import com.topjohnwu.superuser.Shell;

import java.util.List;

import amirz.adaptivestune.database.Boost;

public class Tweaker {
    private static final String TAG = "Tweaker";

    private static final Tunable.BooleanRef INPUT_BOOST_ENABLED =
            new Tunable.BooleanRef(R.string.pref_input_boost_enabled,
                    R.bool.default_input_boost_enabled);

    private static final Tunable.IntegerRef INPUT_BOOST_MS =
            new Tunable.IntegerRef(R.string.pref_input_boost_ms,
                    R.integer.default_input_boost_ms);

    private static final Tunable.IntegerRef INPUT_BOOST_FREQ_LITTLE =
            new Tunable.IntegerRef(R.string.pref_input_boost_freq_little,
                    R.integer.default_input_boost_freq_little);

    private static final Tunable.IntegerRef INPUT_BOOST_FREQ_BIG =
            new Tunable.IntegerRef(R.string.pref_input_boost_freq_big,
                    R.integer.default_input_boost_freq_big);

    private static int mCurrentBoost = -1;

    public static void init() {}

    public static void applyDefaultParams() {
        runSU("echo " + (INPUT_BOOST_ENABLED.get() ? 1 : 0) + " > /sys/module/cpu_boost/parameters/input_boost_enabled",
                "echo " + INPUT_BOOST_MS.get() + " > /sys/module/cpu_boost/parameters/input_boost_ms",
                "echo 0:" + INPUT_BOOST_FREQ_LITTLE.get() + " 1:0 2:" +
                        INPUT_BOOST_FREQ_BIG.get() + " 3:0 > /sys/module/cpu_boost/parameters/input_boost_freq",
                "echo " + Boost.IDLE_BOOST + " > /dev/stune/top-app/schedtune.boost");

        setDynamicStuneBoost(Boost.DEFAULT_BOOST.get());
    }

    public static void setDynamicStuneBoost(int boost) {
        if (boost != mCurrentBoost) {
            Log.w(TAG, "Setting dynamic stune boost to " + boost + " (" + boost + ")");

            // Calling this separately introduces additional delay, but makes the code cleaner.
            runSU("echo " + boost + " > /dev/stune/top-app/schedtune.sched_boost",
                    "echo " + boost + " > /sys/module/cpu_boost/parameters/dynamic_stune_boost");
        }
        mCurrentBoost = boost;
    }

    public static List<String> collectAndReset(String collectPackage, String resetPackage) {
        String reset = "dumpsys gfxinfo " + resetPackage + " reset";
        // It is possible that the new package name is the same,
        // so we need to reset after getting frame time stats.
        return collectPackage == null
                ? runSU(reset)
                : runSU("dumpsys gfxinfo " + collectPackage, reset);
    }

    private static List<String> runSU(String... command) {
        for (String str : command) {
            Log.d(TAG, "SU: " + str);
        }
        return Shell.Sync.su(command);
    }
}
