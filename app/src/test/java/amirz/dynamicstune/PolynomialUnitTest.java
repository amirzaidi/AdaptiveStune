package amirz.dynamicstune;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class PolynomialUnitTest {
    @Test
    public void coefficientCount() {
        List<Polynomial.Point> points = new ArrayList<>();
        points.add(new Polynomial.Point(0, 0));
        points.add(new Polynomial.Point(1, 0));

        Polynomial pol = new Polynomial(points, 1);

        assertEquals(2, pol.getCoefficientCount());
    }

    @Test
    public void flatLine() {
        List<Polynomial.Point> points = new ArrayList<>();
        points.add(new Polynomial.Point(0, 0));
        points.add(new Polynomial.Point(1, 0));

        Polynomial pol = new Polynomial(points, 1);

        assertEquals(0, pol.getCoefficient(0), 0.01);
        assertEquals(0, pol.getCoefficient(1), 0.01);
    }

    @Test
    public void approximatedFlatLine() {
        List<Polynomial.Point> points = new ArrayList<>();
        points.add(new Polynomial.Point(0, -1));
        points.add(new Polynomial.Point(1, 1));
        points.add(new Polynomial.Point(2, 1));
        points.add(new Polynomial.Point(3, -1));

        Polynomial pol = new Polynomial(points, 1);

        assertEquals(0, pol.getCoefficient(0), 0.01);
        assertEquals(0, pol.getCoefficient(1), 0.01);
    }

    @Test
    public void parabola() {
        List<Polynomial.Point> points = new ArrayList<>();
        points.add(new Polynomial.Point(0, 0));
        points.add(new Polynomial.Point(1, 1));
        points.add(new Polynomial.Point(2, 4));

        Polynomial pol = new Polynomial(points, 2);

        assertEquals(0, pol.getCoefficient(0), 0.01);
        assertEquals(0, pol.getCoefficient(1), 0.01);
        assertEquals(1, pol.getCoefficient(2), 0.01);
    }
}
