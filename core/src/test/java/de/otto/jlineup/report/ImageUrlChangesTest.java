package de.otto.jlineup.report;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

class ImageUrlChangesTest {

    @Test
    void shouldListAddedAndRemovedImageUrls() {
        ImageUrlChanges changes = ImageUrlChanges.between(Set.of("a.webp", "b.webp", "c.webp"), Set.of("b.webp", "c.webp", "d.webp", "e.webp"));

        assertThat(changes.added(), is(List.of("d.webp", "e.webp")));
        assertThat(changes.removed(), is(List.of("a.webp")));
    }

    @Test
    void shouldHaveNoChangesForEqualOrUnknownImageUrls() {
        assertThat(ImageUrlChanges.between(Set.of("a.webp"), Set.of("a.webp")), is(nullValue()));
        assertThat(ImageUrlChanges.between(null, Set.of("a.webp")), is(nullValue()));
        assertThat(ImageUrlChanges.between(Set.of("a.webp"), null), is(nullValue()));
    }
}
