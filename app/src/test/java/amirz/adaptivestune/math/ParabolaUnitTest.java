package amirz.adaptivestune.math;

import org.junit.Test;

import amirz.adaptivestune.TestConstants;

import static org.junit.Assert.*;

public class ParabolaUnitTest {
    @Test
    public void zeroIntersect() {
        double[] intersect = Parabola.intersect(1, 0, 0, -1);

        assertArrayEquals(new double[0], intersect, TestConstants.DELTA);
    }

    @Test
    public void singleIntersect() {
        double[] intersect = Parabola.intersect(1, 0, 0, 0);

        assertArrayEquals(new double[] { 0 }, intersect, TestConstants.DELTA);
    }

    @Test
    public void doubleIntersect() {
        double[] intersect = Parabola.intersect(1, 0, 0, 1);

        assertArrayEquals(new double[] { 1, -1 }, intersect, TestConstants.DELTA);
    }

    @Test
    public void doubleRoot() {
        double[] intersect = Parabola.root(1, 0, -1);

        assertArrayEquals(new double[] { 1, -1 }, intersect, TestConstants.DELTA);
    }
}
