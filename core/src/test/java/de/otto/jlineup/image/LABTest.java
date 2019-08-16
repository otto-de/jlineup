package de.otto.jlineup.image;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class LABTest {

    @Test
    public void shouldCalculateCIEDE2000() {

        LAB lab1 = LAB.fromRGB(55,44,33, 0);
        LAB lab2 = LAB.fromRGB(66,55,44, 0);

        double v = LAB.ciede2000(lab1, lab2);

        assertThat(v, is(3.533443206558854d));
    }
}