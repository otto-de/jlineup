package de.otto.jlineup.report;

/**
 * Explains for a screenshot comparison with 'ignore-image-content', whether differences inside images were accepted
 * or why not.
 */
public enum ImageContentHint {

    /** Differences inside unchanged images were accepted. */
    IMAGE_CONTENT_ACCEPTED("Differences inside images were accepted"),
    /** The image URLs of the page differ between the runs, so nothing was accepted. */
    IMAGE_URLS_CHANGED("Image URLs of the page changed, differences inside images were not accepted"),
    /** The screenshots without images differ, so layout, text or overlays changed. */
    LAYOUT_CHANGED("Layout or content outside of images changed (see screenshots without images)"),
    /** A screenshot without images or the image URLs are missing, i.e. because the before step ran without the option. */
    NO_IMAGE_DATA("No screenshots without images available, rerun both steps with 'ignore-image-content'");

    private final String description;

    ImageContentHint(String description) {
        this.description = description;
    }

    @UsedInTemplate
    public String getDescription() {
        return description;
    }
}
