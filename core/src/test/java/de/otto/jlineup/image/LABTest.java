package de.otto.jlineup.image;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LABTest {

    @Test
    public void shouldCalculateCIEDE2000() {

        LAB lab1 = LAB.fromRGB(55,44,33, 0);
        LAB lab2 = LAB.fromRGB(66,55,44, 0);

        double v = Math.round(LAB.ciede2000(lab1, lab2) * Math.pow(10, 12)) / Math.pow(10, 12);

        assertThat(v, is(3.533443206559));
    }
}