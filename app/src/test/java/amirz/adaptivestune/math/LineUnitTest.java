package amirz.adaptivestune.math;

import org.junit.Test;

import amirz.adaptivestune.TestConstants;

import static org.junit.Assert.assertEquals;

public class LineUnitTest {
    @Test
    public void zeroIntersect() {
        double intersect = Line.intersect(0, 0, 1);

        assertEquals(Double.POSITIVE_INFINITY, intersect, TestConstants.DELTA);
    }

    @Test
    public void singleIntersect() {
        double intersect = Line.intersect(1, 0, 1);

        assertEquals(1, intersect, TestConstants.DELTA);
    }

    @Test
    public void alwaysIntersect() {
        double intersect = Line.intersect(0, 0, 0);

        assertEquals(Double.NaN, intersect, TestConstants.DELTA);
    }
}
