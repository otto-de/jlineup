package de.otto.jlineup.report;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Image URLs of a page that were only found before ('removed') or only after ('added').
 */
public record ImageUrlChanges(List<String> added, List<String> removed) {

    /**
     * @return the changes, or null if one of the sets is unknown or both are equal
     */
    public static ImageUrlChanges between(Set<String> before, Set<String> after) {
        if (before == null || after == null || before.equals(after)) {
            return null;
        }
        Set<String> added = new TreeSet<>(after);
        added.removeAll(before);
        Set<String> removed = new TreeSet<>(before);
        removed.removeAll(after);
        return new ImageUrlChanges(List.copyOf(added), List.copyOf(removed));
    }
}
