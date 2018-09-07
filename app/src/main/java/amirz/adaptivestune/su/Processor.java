package amirz.adaptivestune.su;

import java.util.ArrayList;
import java.util.List;

public class Processor {
    public static class Core {
        private int mId;
        private int mPart;

        public int getId() {
            return mId;
        }

        public int getType() {
            return mPart;
        }

        public String toString() {
            return "Core(" + mId + ", 0x" + Integer.toHexString(mPart) + ")";
        }
    }

    public static List<Core> getClusterInfo() {
        List<Core> cores = new ArrayList<>();
        Core core = new Core();
        for (String line : WrapSU.run("cat /proc/cpuinfo")) {
            if (line.startsWith("processor")) {
                core.mId = WrapSU.parseInt(line);
            } else if (line.startsWith("CPU part")) {
                core.mPart = WrapSU.parseIntHex(line);
            } else if (line.isEmpty()) {
                cores.add(core);
                core = new Core();
            }
        }

        return cores;
    }
}
