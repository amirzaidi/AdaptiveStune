package amirz.dynamicstune;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.List;

import amirz.dynamicstune.database.BoostDB;
import amirz.dynamicstune.database.MeasureDB;

public class StuneService extends AccessibilityService {
    private static final String TAG = "StuneService";

    private Handler mHandler;
    private MeasureDB mDB;
    private Tweaker mTweaker;

    // Save boost so we only write on change
    private ComponentName mCurrentComponent;
    private long mCurrentTime = Long.MAX_VALUE;

    @Override
    public void onServiceConnected() {
        mHandler = new Handler();
        mDB = new MeasureDB(new MeasureDB.Helper(this));
        mTweaker = new Tweaker(this);

        mTweaker.setDynamicStuneBoost(BoostDB.getDefaultBoost(this));

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.DEFAULT;
        info.notificationTimeout = 100;
        setServiceInfo(info);

        mTweaker.setDefaultParams();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDB.close();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        final ComponentName newComponent = new ComponentName(event.getPackageName().toString(),
                    event.getClassName().toString());

        // If we are still in the same component or this is an overlay, do not do anything.
        if (!newComponent.equals(mCurrentComponent) && getPackageManager()
                .resolveActivity(new Intent().setComponent(newComponent), 0) != null) {

            // User must at least be in this component for a full second before applying data.
            final ComponentName oldComponent =
                    mCurrentTime + 1000L < System.currentTimeMillis() ?
                            mCurrentComponent :
                            null;

            mCurrentComponent = newComponent;
            mCurrentTime = System.currentTimeMillis();

            Log.w(TAG, "Detected launch of " + mCurrentComponent.flattenToShortString());
            mTweaker.setDynamicStuneBoost(BoostDB.getBoost(this, mCurrentComponent));

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // Use and reset the stats after opening a new component.
                    optimizeAndReset(oldComponent, newComponent);
                }
            });
        }
    }

    private int readInt(String line) {
        // Split by :, then take the first word before the space, then remove ms
        return Integer.valueOf(line.split(":")[1].trim()
                .split(" ")[0]
                .replace("ms", ""));
    }

    private void optimizeAndReset(ComponentName oldComponent, ComponentName newComponent) {
        String resetPackage = newComponent.getPackageName();

        // No optimization is necessary if this is the first opened activity or blacklisted.
        if (oldComponent == null) {
            mTweaker.collectAndReset(null, resetPackage);
        } else {
            String collectPackage = oldComponent.getPackageName();
            List<String> stats = mTweaker.collectAndReset(collectPackage, resetPackage);

            Algorithm.Measurement info = new Algorithm.Measurement(BoostDB.getBoost(this, oldComponent));
            for (String line : stats) {
                if (line.contains("Number Missed Vsync")) {
                    break;
                }

                // Parse data into GfxInfo object
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

            // Do not print logs when there was not even a single frame captured.
            if (info.total > 0) {
                Log.w(TAG, "Rendered " + info.total + " (" + Algorithm.getJankTargetOffset(info) +
                        ", " + Algorithm.getDurationFactor(info) + ") with " + info.janky +
                        " perc (" + info.perc90 + "ms, " + info.perc95 + "ms, " + info.perc99 +
                        "ms) for " +  oldComponent.flattenToShortString());

                mDB.insert(oldComponent, info);

                double componentBoost = Algorithm.getBoost(mDB.select(oldComponent));
                double packageBoost = Algorithm.getBoost(mDB.select(collectPackage));

                BoostDB.setBoost(this, oldComponent, (float) componentBoost, (float) packageBoost);

                Log.w(TAG, "Boost updated to " + componentBoost + "/" + packageBoost +
                        " for " + oldComponent.flattenToShortString());
            }
        }
    }

    @Override
    public void onInterrupt() {
    }
}
