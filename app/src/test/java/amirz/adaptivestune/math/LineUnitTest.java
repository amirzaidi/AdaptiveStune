package amirz.adaptivestune.math;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LineUnitTest {
    @Test
    public void zeroIntersect() {
        double intersect = Line.intersect(0, 0, 1);

        assertEquals(Double.NaN, intersect, 0.01);
    }

    @Test
    public void singleIntersect() {
        double intersect = Line.intersect(1, 0, 1);

        assertEquals(1, intersect, 0.01);
    }

    @Test
    public void alwaysIntersect() {
        double intersect = Line.intersect(0, 0, 0);

        assertEquals(Double.NaN, intersect, 0.01);
    }
}
