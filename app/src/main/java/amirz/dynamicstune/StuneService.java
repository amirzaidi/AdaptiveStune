package amirz.dynamicstune;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class StuneService extends AccessibilityService {
    private static final String TAG = "StuneService";

    private Handler mHandler;

    // Save boost so we only write on change
    private int mCurrentBoost = -1;
    private ComponentName mCurrentComponent;
    private long mCurrentTime = Long.MAX_VALUE;

    @Override
    public void onServiceConnected() {
        mHandler = new Handler();

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.DEFAULT;
        info.notificationTimeout = 100;
        setServiceInfo(info);

        setDefaultParams();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        final ComponentName newComponent =
                new ComponentName(event.getPackageName().toString(),
                    event.getClassName().toString());

        // If we are still in the same component, do not do anything.
        if (newComponent.equals(mCurrentComponent)) {
            return;
        }

        // User must at least be in this component for a full second before applying data.
        final ComponentName oldComponent =
                mCurrentTime + 1000L < System.currentTimeMillis() ?
                        mCurrentComponent :
                        null;

        mCurrentComponent = newComponent;
        mCurrentTime = System.currentTimeMillis();
        setDynamicStuneBoost(Utilities.getBoost(this, mCurrentComponent));

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // Use and reset the stats after opening a new component.
                optimizeAndReset(oldComponent, newComponent);
            }
        });
    }

    private void optimizeAndReset(ComponentName oldComponent, ComponentName newComponent) {
        String reset = "dumpsys gfxinfo " + newComponent.getPackageName() + " reset";

        // No optimization is necessary if this is the first opened activity.
        if (oldComponent == null) {
            runSU(reset);
        } else {
            // It is possible that the new package name is the same,
            // so we need to reset after getting frametime stats.
            List<String> stats = runSU("dumpsys gfxinfo " + oldComponent.getPackageName(), reset);

            int total = 0;
            int janky = 0;
            for (String line : stats) {
                if (line.contains("Number Missed Vsync")) {
                    break;
                }
                if (line.contains("Total frames rendered")) {
                    String[] parse = line.split(":");
                    total = Integer.valueOf(parse[1].trim());
                } else if (line.contains("Janky frames")) {
                    String[] parse = line.split(":");
                    janky = Integer.valueOf(parse[1].trim().split(" ")[0]);
                }
            }

            if (total > 0) {
                double jankFactor = (double) janky / total;

                Log.e(TAG, "Rendered " + total + " with " + janky + " janks (" + jankFactor + ") for " +
                        oldComponent.flattenToShortString());

                // Discard results if not enough information is collected.
                if (total > Utilities.JANK_FACTOR_MIN_FRAMES) {
                    int offset = 0;

                    if (jankFactor >= Utilities.JANK_FACTOR_QUICK_BOOST) {
                        offset = 5;
                    } else if (jankFactor >= Utilities.JANK_FACTOR_STEADY_INCREASE) {
                        offset = 1;
                    } else if (jankFactor <= Utilities.JANK_FACTOR_STEADY_DECREASE) {
                        offset = -1;
                    }

                    if (offset != 0) {
                        int boost = Utilities.getBoost(this, oldComponent) + offset;
                        boost = Math.max(Utilities.IDLE_BOOST, boost);
                        boost = Math.min(Utilities.MAX_BOOST, boost);
                        Utilities.setBoost(this, oldComponent, boost);

                        Log.e(TAG, "Boost updated to " + boost + " for " + oldComponent.flattenToShortString());
                    }
                }
            }
        }
    }

    private void setDefaultParams() {
        runSU("echo 1 > /sys/module/cpu_boost/parameters/input_boost_enabled",
                "echo 1500 > /sys/module/cpu_boost/parameters/input_boost_ms",
                "echo 0:1000000 1:0 2:1000000 3:0 > /sys/module/cpu_boost/parameters/input_boost_freq",
                "echo " + Utilities.IDLE_BOOST + " > /dev/stune/top-app/schedtune.boost");

        setDynamicStuneBoost(Utilities.DEFAULT_BOOST);
    }

    private void setDynamicStuneBoost(int boost) {
        if (mCurrentBoost != boost) {
            mCurrentBoost = boost;
            Log.w(TAG, "Setting dynamic stune boost to " + boost);

            // Calling this separately introduces additional delay, but makes the code cleaner.
            runSU("echo " + boost + " > /dev/stune/top-app/schedtune.sched_boost",
                    "echo " + boost + " > /sys/module/cpu_boost/parameters/dynamic_stune_boost");
        }
    }

    private List<String> runSU(String... command) {
        for (String str : command) {
            Log.d(TAG, "SU: " + str);
        }
        return Shell.SU.run(command);
    }

    @Override
    public void onInterrupt() {
    }
}
