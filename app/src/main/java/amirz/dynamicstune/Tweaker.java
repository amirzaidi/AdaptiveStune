package amirz.dynamicstune;

import android.content.Context;
import android.util.Log;

import com.topjohnwu.superuser.Shell;

import java.util.List;

import amirz.dynamicstune.database.BoostDB;

public class Tweaker {
    private static final String TAG = "Tweaker";

    // ToDo: Make these constants tunable with a settings activity.
    private static final int INPUT_BOOST_ENABLED = 1;
    private static final int INPUT_BOOST_MS = 1500;
    private static final int INPUT_BOOST_FREQ_LITTLE = 1000000;
    private static final int INPUT_BOOST_FREQ_BIG = 1000000;

    private final Context mContext;
    private float mCurrentBoost = Float.NaN;

    public Tweaker(Context context) {
        mContext = context;
    }

    public void setDefaultParams() {
        runSU("echo " + INPUT_BOOST_ENABLED + " > /sys/module/cpu_boost/parameters/input_boost_enabled",
                "echo " + INPUT_BOOST_MS + " > /sys/module/cpu_boost/parameters/input_boost_ms",
                "echo 0:" + INPUT_BOOST_FREQ_LITTLE + " 1:0 2:" +
                        INPUT_BOOST_FREQ_BIG + " 3:0 > /sys/module/cpu_boost/parameters/input_boost_freq",
                "echo " + BoostDB.IDLE_BOOST + " > /dev/stune/top-app/schedtune.boost");

        setDynamicStuneBoost(BoostDB.getDefaultBoost(mContext));
    }

    public void setDynamicStuneBoost(float boost) {
        int roundedBoost = BoostDB.getBoostInt(boost);
        if (BoostDB.getBoostInt(mCurrentBoost) != roundedBoost) {
            Log.w(TAG, "Setting dynamic stune boost to " + roundedBoost + " (" + boost + ")");

            // Calling this separately introduces additional delay, but makes the code cleaner.
            runSU("echo " + roundedBoost + " > /dev/stune/top-app/schedtune.sched_boost",
                    "echo " + roundedBoost + " > /sys/module/cpu_boost/parameters/dynamic_stune_boost");
        }
        mCurrentBoost = boost;
    }

    public List<String> collectAndReset(String collectPackage, String resetPackage) {
        String reset = "dumpsys gfxinfo " + resetPackage + " reset";
        // It is possible that the new package name is the same,
        // so we need to reset after getting frame time stats.
        return collectPackage == null
                ? runSU(reset)
                : runSU("dumpsys gfxinfo " + collectPackage, reset);
    }

    private List<String> runSU(String... command) {
        for (String str : command) {
            Log.d(TAG, "SU: " + str);
        }
        return Shell.Sync.su(command);
    }
}
