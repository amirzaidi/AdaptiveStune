package amirz.adaptivestune;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.List;

import amirz.adaptivestune.database.Boost;
import amirz.adaptivestune.database.Measure;
import amirz.adaptivestune.learning.Algorithm;
import amirz.adaptivestune.learning.GfxInfo;
import amirz.adaptivestune.settings.Tunable;
import amirz.adaptivestune.su.Tweaker;

import static amirz.adaptivestune.database.Settings.prefs;
import static amirz.adaptivestune.settings.Tunable.*;

/**
 * The most important class of the application.
 * Handles all logging and adjusting of tunables.
 */
public class StuneService extends AccessibilityService
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "StuneService";

    public static boolean sIsRunning = false;

    private Handler mHandler;
    private Measure mDB;

    // Save boost so we only write on change
    private ComponentName mCurrentComponent;
    private long mCurrentTime = Long.MAX_VALUE;

    @Override
    public void onServiceConnected() {
        mHandler = new Handler();
        mDB = new Measure(new Measure.Helper(this));

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.DEFAULT;
        info.notificationTimeout = 100;
        setServiceInfo(info);

        Tunable.applyAll(prefs(this), getResources());
        Tweaker.applyDefaultParams();
        prefs(this).registerOnSharedPreferenceChangeListener(this);

        sIsRunning = true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        // Only restore default values if any of the tunables changed.
        if (Tunable.apply(prefs, getResources(), key)) {
            Tweaker.applyDefaultParams();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sIsRunning = false;

        prefs(this).unregisterOnSharedPreferenceChangeListener(this);

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
            Tweaker.setDynamicStuneBoost(Boost.getBoostInt(this, mCurrentComponent));

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // Use and reset the stats after opening a new component.
                    optimizeAndReset(oldComponent, newComponent);
                }
            });
        }
    }

    private void optimizeAndReset(ComponentName oldComponent, ComponentName newComponent) {
        String resetPackage = newComponent.getPackageName();

        // No optimization is necessary if this is the first opened activity or blacklisted.
        if (oldComponent == null) {
            Tweaker.collectAndReset(null, resetPackage);
        } else {
            String collectPackage = oldComponent.getPackageName();
            List<String> stats = Tweaker.collectAndReset(collectPackage, resetPackage);
            Algorithm.Measurement m = new Algorithm.Measurement(Boost.getBoost(this, oldComponent));
            GfxInfo.parse(stats, m);

            // Do not print logs when there was not even a single frame captured.
            if (m.total >= MIN_FRAMES.get()) {
                mDB.insert(oldComponent, m);
                Log.w(TAG, "Rendered " + m.total + " (" + Algorithm.getJankTargetOffset(m) +
                        ", " + Algorithm.getDurationFactor(m) + ") with " + m.janky +
                        " perc (" + m.perc90 + "ms, " + m.perc95 + "ms, " + m.perc99 +
                        "ms) for " +  oldComponent.flattenToShortString());

                double componentBoost = Algorithm.getBoost(mDB.select(oldComponent));
                Boost.setBoost(this, oldComponent, (float) componentBoost);
                Log.w(TAG, "Boost updated to " + componentBoost + " for " +
                        oldComponent.flattenToShortString());
            }
        }
    }

    @Override
    public void onInterrupt() {
    }
}
