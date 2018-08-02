package amirz.adaptivestune.math;

import org.junit.Test;

import static org.junit.Assert.*;

public class MathUtilsUnitTest {
    @Test
    public void between_NaN() {
        boolean isBetween = MathUtils.between(Double.NaN,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY);
        assertFalse(isBetween);
    }

    @Test
    public void between_lowerCornerCase() {
        boolean isBetween = MathUtils.between(0, 0, 1);
        assertTrue(isBetween);
    }

    @Test
    public void between_upperCornerCase() {
        boolean isBetween = MathUtils.between(1, 0, 1);
        assertTrue(isBetween);
    }

    @Test
    public void between_lower() {
        boolean isBetween = MathUtils.between(-1, 0, 1);
        assertFalse(isBetween);
    }

    @Test
    public void between_higher() {
        boolean isBetween = MathUtils.between(2, 0, 1);
        assertFalse(isBetween);
    }
}
