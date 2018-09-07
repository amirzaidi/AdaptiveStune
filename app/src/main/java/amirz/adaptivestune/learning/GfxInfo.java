package amirz.adaptivestune.learning;

import java.util.List;

import amirz.adaptivestune.su.WrapSU;

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
                info.total = WrapSU.parseInt(line);
            } else if (line.contains("Janky frames")) {
                info.janky = WrapSU.parseInt(line);
            } else if (line.contains("90th percentile")) {
                info.perc90 = WrapSU.parseInt(line);
            } else if (line.contains("95th percentile")) {
                info.perc95 = WrapSU.parseInt(line);
            } else if (line.contains("99th percentile")) {
                info.perc99 = WrapSU.parseInt(line);
            }
        }
    }
}
