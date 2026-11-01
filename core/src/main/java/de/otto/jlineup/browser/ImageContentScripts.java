package de.otto.jlineup.browser;

/**
 * JavaScript for 'ignore-image-content': replaces all images of a page by a checkerboard without changing the
 * layout, restores them, and collects the URLs and areas of all images of the page. Works in every browser engine.
 * <p>
 * Covered: img, input[type=image], SVG image, CSS background-image and border-image (also on ::before and ::after),
 * in the document and in open shadow roots. Gradients and other non-url background layers are kept.
 * Not covered (stay visible, so their pixels are compared as usual): video, canvas, iframes, content: url(),
 * list-style-image, mask-image, closed shadow roots.
 */
final class ImageContentScripts {

    private ImageContentScripts() {
    }

    private static final String COMMON = """
            const JLINEUP_ATTR = 'data-jlineup-image-content';
            const jlineupRoots = () => {
                const roots = [document];
                const collect = (root) => root.querySelectorAll('*').forEach(el => {
                    if (el.shadowRoot) {
                        roots.push(el.shadowRoot);
                        collect(el.shadowRoot);
                    }
                });
                collect(document);
                return roots;
            };
            const jlineupSplitLayers = (value) => {
                const layers = [];
                let depth = 0, quote = null, start = 0;
                for (let i = 0; i < value.length; i++) {
                    const c = value[i];
                    if (quote) {
                        if (c === '\\\\') { i++; continue; }
                        if (c === quote) quote = null;
                        continue;
                    }
                    if (c === '"' || c === "'") quote = c;
                    else if (c === '(') depth++;
                    else if (c === ')') depth--;
                    else if (c === ',' && depth === 0) { layers.push(value.substring(start, i)); start = i + 1; }
                }
                layers.push(value.substring(start));
                return layers.map(l => l.trim());
            };
            const jlineupHasUrl = (value) => !!value && value !== 'none' && /url\\(/i.test(value);
            """;

    // Black and white checkerboard with 2x2 pixel squares (small enough to show thin overlays, large enough not to
    // flicker on LCDs when scrolling). Any overlay color equals at most one of the two colors, so text or
    // badges on top of images stay visible in the screenshot without images, no matter which color they have.
    private static final String CHECKERBOARD = "url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAQAAAAECAIAAAAmkwkpAAAAIElEQVR42mNkYGBgYGD4//8/AwMDEwMy+P//P0QYXQYACtQI/Yp64GMAAAAASUVORK5CYII=)";

    /**
     * Replaces all images by a checkerboard without changing the layout. Background layers with urls are replaced,
     * other layers (i.e. gradients) are kept, so only pixels painted by image files change.
     */
    static final String HIDE_IMAGES = "(function() {" + COMMON + "const CHECKERBOARD = '" + CHECKERBOARD + "';" + """
                const cycle = (list, length) => Array.from({length: length}, (_, i) => list[i % list.length]);
                let counter = 0;
                jlineupRoots().forEach(root => {
                    const rules = [
                        'img, input[type="image"] { object-position: -100000px -100000px !important; background: ' + CHECKERBOARD + ' repeat 0 0 / 4px 4px !important; image-rendering: pixelated !important; }',
                        'image { visibility: hidden !important; }'
                    ];
                    root.querySelectorAll('*').forEach(el => {
                        [null, '::before', '::after'].forEach(pseudo => {
                            const style = getComputedStyle(el, pseudo);
                            const declarations = [];
                            if (jlineupHasUrl(style.backgroundImage)) {
                                const layers = jlineupSplitLayers(style.backgroundImage);
                                const sizes = cycle(jlineupSplitLayers(style.backgroundSize), layers.length);
                                const repeats = cycle(jlineupSplitLayers(style.backgroundRepeat), layers.length);
                                layers.forEach((layer, i) => {
                                    if (/url\\(/i.test(layer)) {
                                        layers[i] = CHECKERBOARD;
                                        sizes[i] = '4px 4px';
                                        repeats[i] = 'repeat';
                                    }
                                });
                                declarations.push('background-image: ' + layers.join(', ') + ' !important;');
                                declarations.push('background-size: ' + sizes.join(', ') + ' !important;');
                                declarations.push('background-repeat: ' + repeats.join(', ') + ' !important;');
                            }
                            if (jlineupHasUrl(style.borderImageSource)) {
                                declarations.push('border-image-source: none !important;');
                            }
                            if (declarations.length > 0) {
                                let id = el.getAttribute(JLINEUP_ATTR);
                                if (!id) {
                                    id = String(++counter);
                                    el.setAttribute(JLINEUP_ATTR, id);
                                }
                                rules.push('[' + JLINEUP_ATTR + '="' + id + '"]' + (pseudo || '') + ' { ' + declarations.join(' ') + ' }');
                            }
                        });
                    });
                    const styleElement = document.createElement('style');
                    styleElement.setAttribute(JLINEUP_ATTR + '-style', '');
                    styleElement.textContent = rules.join('\\n');
                    (root === document ? (document.head || document.documentElement) : root).appendChild(styleElement);
                });
            })();""";

    static final String RESTORE_IMAGES = "(function() {" + COMMON + """
                jlineupRoots().forEach(root => {
                    root.querySelectorAll('[' + JLINEUP_ATTR + '-style]').forEach(el => el.remove());
                    root.querySelectorAll('[' + JLINEUP_ATTR + ']').forEach(el => el.removeAttribute(JLINEUP_ATTR));
                });
            })();""";

    /**
     * Returns the absolute URLs of all images of the page. data: urls are shortened to their length and a hash.
     */
    static final String COLLECT_IMAGE_URLS = "return (function() {" + COMMON + """
                const urls = new Set();
                const add = (url) => {
                    if (!url) return;
                    if (url.startsWith('data:')) {
                        let hash = 0;
                        for (let i = 0; i < url.length; i++) hash = (hash * 31 + url.charCodeAt(i)) | 0;
                        urls.add('data:[' + url.length + ' chars, hash ' + (hash >>> 0).toString(16) + ']');
                        return;
                    }
                    try {
                        urls.add(new URL(url, document.baseURI).href);
                    } catch (e) {
                        urls.add(url);
                    }
                };
                const addCss = (value) => {
                    if (!jlineupHasUrl(value)) return;
                    const regex = /url\\(\\s*(['"]?)(.*?)\\1\\s*\\)/gi;
                    let match;
                    while ((match = regex.exec(value)) !== null) add(match[2]);
                };
                jlineupRoots().forEach(root => {
                    root.querySelectorAll('*').forEach(el => {
                        const tag = el.tagName.toLowerCase();
                        if (tag === 'img') add(el.currentSrc || el.src);
                        else if (tag === 'input' && (el.getAttribute('type') || '').toLowerCase() === 'image') add(el.src);
                        else if (tag === 'image' && el.namespaceURI === 'http://www.w3.org/2000/svg') {
                            add((el.href && el.href.baseVal) || el.getAttribute('href') || el.getAttributeNS('http://www.w3.org/1999/xlink', 'href'));
                        }
                        [null, '::before', '::after'].forEach(pseudo => {
                            const style = getComputedStyle(el, pseudo);
                            addCss(style.backgroundImage);
                            addCss(style.borderImageSource);
                        });
                    });
                });
                return Array.from(urls);
            })();""";

    /**
     * Returns the areas of all image elements (see above) that intersect the viewport, as [left, top, right, bottom]
     * in CSS pixels relative to the viewport. Elements with an image on ::before or ::after count with their own area.
     */
    static final String COLLECT_IMAGE_AREAS = "return (function() {" + COMMON + """
                const areas = [];
                const viewportWidth = window.innerWidth, viewportHeight = window.innerHeight;
                const addArea = (rect) => {
                    if (rect.width <= 0 || rect.height <= 0) return;
                    if (rect.right <= 0 || rect.bottom <= 0 || rect.left >= viewportWidth || rect.top >= viewportHeight) return;
                    areas.push([rect.left, rect.top, rect.right, rect.bottom]);
                };
                jlineupRoots().forEach(root => {
                    root.querySelectorAll('*').forEach(el => {
                        const tag = el.tagName.toLowerCase();
                        if (tag === 'img'
                            || (tag === 'input' && (el.getAttribute('type') || '').toLowerCase() === 'image')
                            || (tag === 'image' && el.namespaceURI === 'http://www.w3.org/2000/svg')) {
                            addArea(el.getBoundingClientRect());
                            return;
                        }
                        for (const pseudo of [null, '::before', '::after']) {
                            const style = getComputedStyle(el, pseudo);
                            if (jlineupHasUrl(style.backgroundImage) || jlineupHasUrl(style.borderImageSource)) {
                                addArea(el.getBoundingClientRect());
                                return;
                            }
                        }
                    });
                });
                return areas;
            })();""";

    /**
     * Hides the scrollbar of the page. Overlay scrollbars (i.e. in Chrome) fade out about a second after scrolling,
     * so whether they are visible in a screenshot depends on timing, which makes screenshots differ from each other.
     */
    static final String HIDE_SCROLLBARS_STYLE = "html { scrollbar-width: none !important; } html::-webkit-scrollbar, body::-webkit-scrollbar { display: none !important; }";

    /**
     * Async script: calls back after the browser rendered two more frames, or after one second at the latest.
     */
    static final String WAIT_FOR_REPAINT = """
            const done = arguments[arguments.length - 1];
            let called = false;
            const finish = () => { if (!called) { called = true; done(); } };
            requestAnimationFrame(() => requestAnimationFrame(finish));
            setTimeout(finish, 1000);
            """;
}
