package amirz.adaptivestune.math;

import org.junit.Test;

import static org.junit.Assert.*;

public class ParabolaUnitTest {
    @Test
    public void zeroIntersect() {
        double[] intersect = Parabola.intersect(1, 0, 0, -1);

        assertArrayEquals(new double[0], intersect, 0.01);
    }

    @Test
    public void singleIntersect() {
        double[] intersect = Parabola.intersect(1, 0, 0, 0);

        assertArrayEquals(new double[] { 0 }, intersect, 0.01);
    }

    @Test
    public void doubleIntersect() {
        double[] intersect = Parabola.intersect(1, 0, 0, 1);

        assertArrayEquals(new double[] { 1, -1 }, intersect, 0.01);
    }
}
