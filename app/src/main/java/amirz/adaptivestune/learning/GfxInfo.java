package amirz.adaptivestune.learning;

import java.util.List;

public class GfxInfo {
    /**
     * Subclass that contains the aggregate results of a gfxinfo command line call.
     */
    public static class Measurement {
        public double total;
        public double janky;
        public double perc90;
        public double perc95;
        public double perc99;
    }

    public static void parse(List<String> stats, Measurement info) {
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
    }

    private static int readInt(String line) {
        // Split by :, then take the first word before the space, then remove ms
        return Integer.valueOf(line.split(":")[1].trim()
                .split(" ")[0]
                .replace("ms", ""));
    }
}
