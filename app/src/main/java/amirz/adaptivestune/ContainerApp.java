package amirz.adaptivestune;

import com.topjohnwu.superuser.Shell;

import amirz.adaptivestune.database.Boost;

public class ContainerApp extends Shell.ContainerApp {
    // Call these classes once so the fields get loaded by the JVM.
    // If they are not loaded, the Tunable class will not be populated in time.
    static {
        Boost.init();
        Algorithm.init();
        Tweaker.init();
    }
}
