package amirz.adaptivestune.su;

import android.util.Log;

import com.topjohnwu.superuser.Shell;

import java.util.List;

public class WrapSU {
    private static final String TAG = "WrapSU";

    public static List<String> run(String... command) {
        for (String str : command) {
            Log.d(TAG, "SU: " + str);
        }
        return Shell.su(command).exec().getOut();
    }

    public static int parseInt(String line) {
        // Split by :, then take the first word before the space, then remove ms
        return Integer.valueOf(line.split(":")[1].trim()
                .split(" ")[0]
                .replace("ms", ""));
    }

    public static int parseIntHex(String line) {
        // Split by :, then take the first word before the space, then remove ms
        return Integer.valueOf(line.split(":")[1].trim()
                .split(" ")[0]
                .replace("ms", "")
                .replace("0x", ""),
                16);
    }
}
