package amirz.adaptivestune;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import amirz.adaptivestune.learning.Algorithm;
import amirz.adaptivestune.math.Polynomial;

import static org.junit.Assert.*;

public class AlgorithmUnitTest {
    @Test
    public void getBoostFromParabola() {
        List<Polynomial.Point> points = new ArrayList<>();
        points.add(new Polynomial.Point(0, 3));
        points.add(new Polynomial.Point(1, 0));
        points.add(new Polynomial.Point(2, -1));

        double[] expected = { 3, 1 };
        double[] result = Algorithm.getBoostFromParabola(points);
        assertArrayEquals(expected, result, TestConstants.DELTA);
    }

    @Test
    public void getBoostFromLine() {
        List<Polynomial.Point> points = new ArrayList<>();
        points.add(new Polynomial.Point(0, 3));
        points.add(new Polynomial.Point(2, -3));

        double expected = 1;
        double result = Algorithm.getBoostFromLine(points);
        assertEquals(expected, result, TestConstants.DELTA);
    }

    @Test
    public void getJankTargetOffsetPositive() {
        Algorithm.Measurement m = new Algorithm.Measurement(0);
        m.total = 1;
        m.janky = 1;
        m.perc90 = 100;
        m.perc95 = 100;
        m.perc99 = 100;
        double offset = Algorithm.getJankTargetOffset(m, 10, 1, 1, 1);
        assertTrue(offset > 0);
    }

    @Test
    public void getDurationLarger() {
        Algorithm.Measurement m = new Algorithm.Measurement(0);

        m.total = 1;
        double factor1 = Algorithm.getDurationFactor(m, 1, 1);

        m.total = 2;
        double factor2 = Algorithm.getDurationFactor(m, 1, 1);

        assertTrue(factor2 > factor1);
    }
}
