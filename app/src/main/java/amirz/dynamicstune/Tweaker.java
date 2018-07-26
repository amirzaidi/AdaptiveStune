package amirz.dynamicstune;

import android.content.Context;
import android.util.Log;

import com.topjohnwu.superuser.Shell;

import java.util.List;

import amirz.dynamicstune.database.BoostDB;

public class Tweaker {
    private static final String TAG = "Tweaker";

    private final Context mContext;
    private float mCurrentBoost = Float.NaN;

    public Tweaker(Context context) {
        mContext = context;
    }

    public void setDefaultParams() {
        runSU("echo 1 > /sys/module/cpu_boost/parameters/input_boost_enabled",
                "echo 1500 > /sys/module/cpu_boost/parameters/input_boost_ms",
                "echo 0:1000000 1:0 2:1000000 3:0 > /sys/module/cpu_boost/parameters/input_boost_freq",
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
