package amirz.dynamicstune;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import eu.chainfire.libsuperuser.Shell;

import static android.content.ComponentName.unflattenFromString;

public class StuneService extends AccessibilityService {
    private static final String TAG = "StuneService";

    // Save boost so we only write on change
    private int mCurrentBoost = -1;

    @Override
    public void onServiceConnected() {
        Utilities.setBoost(this,
                "com.android.settings",
                10);

        Utilities.setBoost(this,
                unflattenFromString("com.android.settings/.SubSettings"),
                5);

        Utilities.setBoost(this,
                unflattenFromString("com.oneplus.aod/android.widget.RelativeLayout"),
                0);

        Utilities.setBoost(this,
                unflattenFromString("com.google.android.apps.nexuslauncher/.NexusLauncherActivity"),
                50);

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
        Log.d(TAG, "onAccessibilityEvent " +
                event.getEventType() + " " +
                event.getPackageName() + " " +
                event.getClassName());

        int stune = Utilities.getBoost(this,
                new ComponentName(event.getPackageName().toString(), event.getClassName().toString()));

        setDynamicStuneBoost(stune);
    }

    private void setDefaultParams() {
        Shell.SU.run("echo 1 > /sys/module/cpu_boost/parameters/input_boost_enabled");
        Shell.SU.run("echo 1500 > /sys/module/cpu_boost/parameters/input_boost_ms");
        Shell.SU.run("echo 0:1000000 1:0 2:1000000 3:0 > /sys/module/cpu_boost/parameters/input_boost_freq");

        Shell.SU.run("echo 5 > /dev/stune/top-app/schedtune.boost");
        setDynamicStuneBoost(Utilities.DEFAULT_BOOST);
    }

    private void setDynamicStuneBoost(int boost) {
        if (mCurrentBoost != boost) {
            mCurrentBoost = boost;
            Log.w(TAG, "Setting stune to " + boost);
            Shell.SU.run("echo " + boost + " > /dev/stune/top-app/schedtune.sched_boost");
            Shell.SU.run("echo " + boost + " > /sys/module/cpu_boost/parameters/dynamic_stune_boost");
        }
    }

    @Override
    public void onInterrupt() {
    }
}
