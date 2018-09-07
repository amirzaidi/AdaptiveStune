package amirz.adaptivestune.su;

import android.util.Log;

import java.util.List;

import static amirz.adaptivestune.settings.Tunable.*;

/**
 * Handles all LibSU interactions.
 */
public class Tweaker {
    private static final String TAG = "Tweaker";

    private static int mCurrentBoost = -1;

    /**
     * Applies default values to the kernel tunables for all parameters.
     */
    public static void setup() {
        applyStaticParams();
        setDynamicStuneBoost(mCurrentBoost == -1
                ? DEFAULT_BOOST.get()
                : mCurrentBoost);
    }

    /**
     * Applies the static values to the kernel tunables.
     */
    public static void applyStaticParams() {
        String inputBoostFreq = getInputBoostFreqForProcessor(INPUT_BOOST_FREQ_LITTLE.get(),
                INPUT_BOOST_FREQ_BIG.get());

        WrapSU.run("echo " + (INPUT_BOOST_ENABLED.get() ? 1 : 0) + " > /sys/module/cpu_boost/parameters/input_boost_enabled",
                "echo " + INPUT_BOOST_MS.get() + " > /sys/module/cpu_boost/parameters/input_boost_ms",
                "echo " + inputBoostFreq.trim() + " > /sys/module/cpu_boost/parameters/input_boost_freq",
                "echo " + IDLE_BOOST.get() + " > /dev/stune/top-app/schedtune.boost");
    }

    private static String getInputBoostFreqForProcessor(int littleBoost, int bigBoost) {
        StringBuilder inputBoostBuilder = new StringBuilder();
        int clusterType = -1;
        int cluster = -1;
        for (Processor.Core core : Processor.getClusterInfo()) {
            Log.w(TAG, core.toString());

            int freq = 0;
            int type = core.getType();
            if (clusterType != type) {
                clusterType = type;
                cluster++;

                if (cluster == 0) {
                    freq = littleBoost;
                } else if (cluster == 1) {
                    freq = bigBoost;
                }

                // It is possible to add more clusters when modern CPUs support it.
            }

            inputBoostBuilder.append(core.getId());
            inputBoostBuilder.append(':');
            inputBoostBuilder.append(freq);
            inputBoostBuilder.append(' ');
        }

        return inputBoostBuilder.toString();
    }

    /**
     * Applies a boost value to the kernel tunables.
     * @param boost Boost value used for sched_boost and dynamic_stune_boost.
     */
    public static void setDynamicStuneBoost(int boost) {
        Log.w(TAG, "Setting dynamic stune boost to " + boost + " (" + boost + ")");
        if (boost != mCurrentBoost) {
            // Calling this separately introduces additional delay, but makes the code cleaner.
            WrapSU.run("echo " + boost + " > /dev/stune/top-app/schedtune.sched_boost",
                    "echo " + boost + " > /sys/module/cpu_boost/parameters/dynamic_stune_boost");
            mCurrentBoost = boost;
        }
    }

    /**
     * Runs gfxinfo commands to collect framerate data and reset it afterwards.
     * @param collectPackage The package to collect framerate data from.
     * @param resetPackage The package that needs to be reset.
     * @return Result of the collection command.
     */
    public static List<String> collectAndReset(String collectPackage, String resetPackage) {
        String reset = "dumpsys gfxinfo " + resetPackage + " reset";
        // It is possible that the new package name is the same,
        // so we need to reset after getting frame time stats.
        return collectPackage == null
                ? WrapSU.run(reset)
                : WrapSU.run("dumpsys gfxinfo " + collectPackage, reset);
    }
}
