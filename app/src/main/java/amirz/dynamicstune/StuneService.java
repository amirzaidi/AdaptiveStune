package amirz.dynamicstune;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.topjohnwu.superuser.Shell;

import java.util.List;

public class StuneService extends AccessibilityService {
    private static final String TAG = "StuneService";
    private static final String[] EXCEPT = { "com.android.systemui", "com.oneplus.aod" };

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

        Log.w(TAG, "Detected launch of " + mCurrentComponent.flattenToShortString());
        setDynamicStuneBoost(Database.getBoostInt(this, mCurrentComponent));

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // Use and reset the stats after opening a new component.
                optimizeAndReset(oldComponent, newComponent);
            }
        });
    }

    // Should not be measured at all
    private boolean isException(String packageName) {
        for (String exception : EXCEPT) {
            if (exception.equals(packageName)) {
                return false;
            }
        }
        return true;
    }

    private int readInt(String line) {
        // Split by :, then take the first word before the space, then remove ms
        return Integer.valueOf(line.split(":")[1].trim()
                .split(" ")[0]
                .replace("ms", ""));
    }

    private void optimizeAndReset(ComponentName oldComponent, ComponentName newComponent) {
        String reset = "dumpsys gfxinfo " + newComponent.getPackageName() + " reset";

        // No optimization is necessary if this is the first opened activity or blacklisted.
        if (oldComponent == null || isException(oldComponent.getPackageName())) {
            runSU(reset);
        } else {
            // It is possible that the new package name is the same,
            // so we need to reset after getting frametime stats.
            List<String> stats = runSU("dumpsys gfxinfo " + oldComponent.getPackageName(), reset);

            Algorithm.GfxInfo info = new Algorithm.GfxInfo();
            for (String line : stats) {
                if (line.contains("Number Missed Vsync")) {
                    break;
                }
                if (line.contains("Total frames rendered")) {
                    info.total = readInt(line);
                } else if (line.contains("Janky frames")) {
                    info.janky = readInt(line);
                } else if (line.contains("90th percentile")) {
                    info.perc90 = readInt(line);
                } else if (line.contains("95th percentile")) {
                    info.perc95 = readInt(line);
                } else if (line.contains("99th percentile")) {
                    info.perc99 = readInt(line);
                }
            }

            if (info.total > 0) {
                Log.w(TAG, "Rendered " + info.total + " with " + info.janky + " janks (" +
                        info.getJankFactor() + ") perc (" + info.perc90 + "ms, " + info.perc95 +
                        "ms, " + info.perc99 + "ms) for " +  oldComponent.flattenToShortString());

                float offset = Algorithm.getBoostOffset(info);
                if (offset != 0) {
                    float boost = Database.offsetBoost(this, oldComponent, offset);
                    Log.w(TAG, "Boost updated to " + boost + " (" + (offset >= 0 ? "+" : "-") +
                            offset + ") for " + oldComponent.flattenToShortString());
                }
            }
        }
    }

    private void setDefaultParams() {
        runSU("echo 1 > /sys/module/cpu_boost/parameters/input_boost_enabled",
                "echo 1500 > /sys/module/cpu_boost/parameters/input_boost_ms",
                "echo 0:1000000 1:0 2:1000000 3:0 > /sys/module/cpu_boost/parameters/input_boost_freq",
                "echo " + Database.IDLE_BOOST + " > /dev/stune/top-app/schedtune.boost");

        setDynamicStuneBoost(Database.getDefaultBoost(this));
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
        return Shell.Sync.su(command);
    }

    @Override
    public void onInterrupt() {
    }
}
