'use strict';

const fs   = require('fs');
const path = require('path');

// ── Load and eval the inline scripts of report.html ──────────────────────────
//
// report.html is a standalone self-contained file once it is rendered, but its
// template is made of report.html itself and the shared fragments of report_common.html.
// We read the <script type="application/javascript"> blocks (shared first, then the
// one of report.html) and run them in the jsdom global context so their functions
// (initClickImage, closeClickDiff, etc.) are available as globals in every test.

const templatesDir = path.resolve(__dirname, '../../main/resources/templates');

function readInlineScript(fileName) {
    const html = fs.readFileSync(path.join(templatesDir, fileName), 'utf8');
    const scriptMatch = html.match(
        /<script\s+type="application\/javascript">([\s\S]*?)<\/script>/
    );
    if (!scriptMatch) throw new Error('Could not find application/javascript script in ' + fileName);
    return scriptMatch[1];
}

const scriptSrc = readInlineScript('report_common.html') + '\n' + readInlineScript('report.html');

// report.html defines JLINEUP_LABELS in a separate Thymeleaf-inlined script block,
// which is not part of the extracted script, so provide it here.
globalThis.JLINEUP_LABELS = { before: 'Before', after: 'After' };

// Run in this context so the function declarations land on the Jest global.
// We wrap them in an IIFE that assigns to globalThis so they're reachable.
const wrappedSrc = `(function(global) { ${scriptSrc}
    global.initClickImage    = initClickImage;
    global.closeClickDiff    = closeClickDiff;
    global.initZoom          = initZoom;
    global.stopZoom          = stopZoom;
    global.imageZoomForContext = imageZoomForContext;
    global.imageZoom         = imageZoom;
    global.zoom              = zoom;
    global.moveLens          = moveLens;
    global.getCursorPosition = getCursorPosition;
    global.openPicturesInTabs = openPicturesInTabs;
    global.setGlobalImageMode = setGlobalImageMode;
    global.setContextImageMode = setContextImageMode;
    global.applyImageMode    = applyImageMode;
    global.imageModeFile     = imageModeFile;
    global.applyLayout       = applyLayout;
    global.toggleLayout      = toggleLayout;
}(globalThis));`;
// eslint-disable-next-line no-eval
eval(wrappedSrc);

// ── DOM fixture helpers ───────────────────────────────────────────────────────

function buildClickDiffDom() {
    document.body.innerHTML = `
        <div id="black"></div>
        <div id="clickdiff" style="display:none">
            <img id="clickimage" />
            <div id="clickimagelabel">Before</div>
        </div>
        <div class="legend" style="display:none"></div>
        <div id="zoombox" class="invisible"></div>
    `;
}

function buildZoomDom(contextId) {
    document.body.innerHTML = `
        <div id="zoombox" class="invisible">
            <div id="zoomfix">
                <div id="zoom_before" class="zoom"></div>
                <div id="zoom_after"  class="zoom"></div>
                <div id="zoom_diff"   class="zoom"></div>
            </div>
        </div>
        <div class="legend" style="display:none"></div>
        <img id="before_${contextId}" src="before.png" width="100" height="200" />
        <img id="after_${contextId}"  src="after.png"  width="100" height="200" />
        <img id="diff_${contextId}"   src="diff.png"   width="100" height="200" />
        <div id="lens-before_${contextId}"    class="zoom-lens lens-${contextId}" style="width:44px;height:44px;"></div>
        <div id="lens-after_${contextId}"     class="zoom-lens lens-${contextId}" style="width:44px;height:44px;"></div>
        <div id="lens-difference_${contextId}" class="zoom-lens lens-${contextId}" style="width:44px;height:44px;"></div>
    `;
}

// ── initClickImage / closeClickDiff ───────────────────────────────────────────

describe('initClickImage', () => {
    const FILE1 = 'before.png';
    const FILE2 = 'after.png';
    const FILE3 = 'diff.png';

    beforeEach(() => {
        buildClickDiffDom();
        // Start with clickimage showing FILE1
        document.getElementById('clickimage').src = FILE1;
    });

    test('shows clickdiff and black overlay', () => {
        initClickImage(FILE1, FILE2, FILE3);
        expect(document.getElementById('clickdiff').style.display).toBe('block');
        expect(document.getElementById('black').style.display).toBe('block');
    });

    test('shows legend', () => {
        initClickImage(FILE1, FILE2, FILE3);
        expect(document.getElementsByClassName('legend')[0].style.display).toBe('block');
    });

    test('cycles: before → after when current src includes FILE1', () => {
        document.getElementById('clickdiff').style.display = 'block';
        document.getElementById('clickimage').src = FILE1;
        initClickImage(FILE1, FILE2, FILE3);
        expect(document.getElementById('clickimage').src).toContain(FILE2);
        expect(document.getElementById('clickimagelabel').innerText).toBe('After');
    });

    test('cycles: after → difference when current src includes FILE2', () => {
        document.getElementById('clickdiff').style.display = 'block';
        document.getElementById('clickimage').src = FILE2;
        initClickImage(FILE1, FILE2, FILE3);
        expect(document.getElementById('clickimage').src).toContain(FILE3);
        expect(document.getElementById('clickimagelabel').innerText).toBe('Difference');
    });

    test('cycles: difference → before', () => {
        document.getElementById('clickdiff').style.display = 'block';
        document.getElementById('clickimage').src = FILE3;
        initClickImage(FILE1, FILE2, FILE3);
        expect(document.getElementById('clickimage').src).toContain(FILE1);
        expect(document.getElementById('clickimagelabel').innerText).toBe('Before');
    });

    test('skips null file2: before → difference when file2 is "null"', () => {
        document.getElementById('clickdiff').style.display = 'block';
        document.getElementById('clickimage').src = FILE1;
        initClickImage(FILE1, 'null', FILE3);
        expect(document.getElementById('clickimage').src).toContain(FILE3);
        expect(document.getElementById('clickimagelabel').innerText).toBe('Difference');
    });

    test('skips null file3: after → before when file3 is "null"', () => {
        document.getElementById('clickdiff').style.display = 'block';
        document.getElementById('clickimage').src = FILE2;
        initClickImage(FILE1, FILE2, 'null');
        expect(document.getElementById('clickimage').src).toContain(FILE1);
    });

    test('registers onclick handler on clickimage', () => {
        initClickImage(FILE1, FILE2, FILE3);
        expect(document.getElementById('clickimage').onclick).toBeInstanceOf(Function);
    });

    test('caps the image at the screenshot display width', () => {
        initClickImage(FILE1, FILE2, FILE3, '320');
        expect(document.getElementById('clickimage').style.getPropertyValue('--clickimage-max-width')).toBe('320px');
    });

    test('keeps the display width when cycling through the images', () => {
        initClickImage(FILE1, FILE2, FILE3, '320');
        document.getElementById('clickimage').style.removeProperty('--clickimage-max-width');
        document.getElementById('clickimage').onclick();
        expect(document.getElementById('clickimage').style.getPropertyValue('--clickimage-max-width')).toBe('320px');
    });

    test('removes the cap when the display width is unknown', () => {
        initClickImage(FILE1, FILE2, FILE3, '320');
        initClickImage(FILE1, FILE2, FILE3, '');
        expect(document.getElementById('clickimage').style.getPropertyValue('--clickimage-max-width')).toBe('');
    });
});

describe('closeClickDiff', () => {
    beforeEach(buildClickDiffDom);

    test('hides clickdiff and black overlay', () => {
        document.getElementById('clickdiff').style.display = 'block';
        document.getElementById('black').style.display = 'block';
        closeClickDiff();
        expect(document.getElementById('clickdiff').style.display).toBe('none');
        expect(document.getElementById('black').style.display).toBe('none');
    });

    test('hides legend', () => {
        document.getElementsByClassName('legend')[0].style.display = 'block';
        closeClickDiff();
        expect(document.getElementsByClassName('legend')[0].style.display).toBe('none');
    });
});

// ── stopZoom ──────────────────────────────────────────────────────────────────

describe('stopZoom', () => {
    beforeEach(() => buildZoomDom('ctx1'));

    test('adds invisible class to zoombox', () => {
        document.getElementById('zoombox').classList.remove('invisible');
        document.getElementById('zoombox').classList.add('visible');
        stopZoom();
        expect(document.getElementById('zoombox').classList.contains('invisible')).toBe(true);
        expect(document.getElementById('zoombox').classList.contains('visible')).toBe(false);
    });

    test('marks all zoom-lenses as invisible', () => {
        document.querySelectorAll('.zoom-lens').forEach(el => {
            el.classList.add('visible');
            el.classList.remove('invisible');
        });
        stopZoom();
        document.querySelectorAll('.zoom-lens').forEach(el => {
            expect(el.classList.contains('invisible')).toBe(true);
        });
    });

    test('hides legend', () => {
        document.getElementsByClassName('legend')[0].style.display = 'block';
        stopZoom();
        expect(document.getElementsByClassName('legend')[0].style.display).toBe('none');
    });
});

// ── getCursorPosition ─────────────────────────────────────────────────────────

describe('getCursorPosition', () => {
    test('returns position relative to element', () => {
        const img = document.createElement('img');
        // Mock getBoundingClientRect
        img.getBoundingClientRect = () => ({ left: 10, top: 20 });
        const pos = getCursorPosition({ pageX: 50, pageY: 80 }, img);
        // x = 50 - 10 - pageXOffset(0) = 40; y = 80 - 20 - pageYOffset(0) = 60
        expect(pos.x).toBe(40);
        expect(pos.y).toBe(60);
    });

    test('accounts for page scroll offsets', () => {
        const img = document.createElement('img');
        img.getBoundingClientRect = () => ({ left: 0, top: 0 });
        window.pageXOffset = 5;
        window.pageYOffset = 10;
        const pos = getCursorPosition({ pageX: 30, pageY: 40 }, img);
        expect(pos.x).toBe(25);
        expect(pos.y).toBe(30);
        window.pageXOffset = 0;
        window.pageYOffset = 0;
    });
});

// ── openPicturesInTabs ────────────────────────────────────────────────────────

describe('openPicturesInTabs', () => {
    test('calls window.open for each file', () => {
        const spy = jest.spyOn(window, 'open').mockImplementation(() => {});
        openPicturesInTabs('before.png', 'after.png', 'diff.png');
        expect(spy).toHaveBeenCalledTimes(3);
        expect(spy).toHaveBeenCalledWith('before.png', 'before');
        expect(spy).toHaveBeenCalledWith('after.png',  'after');
        expect(spy).toHaveBeenCalledWith('diff.png',   'difference');
        spy.mockRestore();
    });
});

// ── Screenshots with / without images (ignore-image-content) ─────────────────

function buildImageModeDom() {
    delete document.body.dataset.imageMode;
    document.body.innerHTML = `
        <div id="black"></div>
        <div id="clickdiff" style="display:none">
            <img id="clickimage" />
            <div id="clickimagelabel">Before</div>
        </div>
        <div class="legend" style="display:none"></div>
        <span class="image-mode-toggle global-image-mode">
            <button class="image-mode-chip active" data-image-mode="images"></button>
            <button class="image-mode-chip" data-image-mode="noimages"></button>
        </span>
        <details class="urlReport" id="ctx1">
            <span class="image-mode-toggle context-image-mode">
                <button class="image-mode-chip active" data-image-mode="images" id="ctx1-images"></button>
                <button class="image-mode-chip" data-image-mode="noimages" id="ctx1-noimages"></button>
            </span>
            <a id="link1" href="b1.png" data-href-images="b1.png" data-href-noimages="b1_noimages.png">Before</a>
            <img id="before1" src="b1.png" data-src-images="b1.png" data-src-noimages="b1_noimages.png"/>
            <img id="after1" src="a1.png" data-src-images="a1.png" data-src-noimages="a1_noimages.png"/>
            <img id="diff1" src="d1.png" data-src-images="d1.png" data-src-noimages="d1_noimages.png" data-diff-images="d1.png" data-diff-noimages="d1_noimages.png"/>
        </details>
        <details class="urlReport" id="ctx2">
            <img id="before2" src="b2.png" data-src-images="b2.png" data-src-noimages="b2_noimages.png"/>
            <img id="plain2" src="p2.png"/>
        </details>
    `;
}

describe('image mode', () => {
    beforeEach(() => {
        buildImageModeDom();
        localStorage.clear();
    });

    test('switches all screenshots and links globally and remembers it', () => {
        setGlobalImageMode('noimages');
        expect(document.getElementById('before1').getAttribute('src')).toBe('b1_noimages.png');
        expect(document.getElementById('after1').getAttribute('src')).toBe('a1_noimages.png');
        expect(document.getElementById('before2').getAttribute('src')).toBe('b2_noimages.png');
        expect(document.getElementById('link1').getAttribute('href')).toBe('b1_noimages.png');
        expect(document.getElementById('plain2').getAttribute('src')).toBe('p2.png');
        expect(localStorage.getItem('jlineup-image-mode')).toBe('noimages');
        expect(document.querySelector('.global-image-mode [data-image-mode="noimages"]').classList.contains('active')).toBe(true);
        expect(document.getElementById('ctx1-noimages').classList.contains('active')).toBe(true);

        setGlobalImageMode('images');
        expect(document.getElementById('before1').getAttribute('src')).toBe('b1.png');
        expect(document.getElementById('link1').getAttribute('href')).toBe('b1.png');
        expect(localStorage.getItem('jlineup-image-mode')).toBe('');
    });

    test('switches a single context only', () => {
        setContextImageMode(document.getElementById('ctx1-noimages'), 'noimages');
        expect(document.getElementById('before1').getAttribute('src')).toBe('b1_noimages.png');
        expect(document.getElementById('before2').getAttribute('src')).toBe('b2.png');
        expect(document.getElementById('ctx1-noimages').classList.contains('active')).toBe(true);
        expect(document.getElementById('ctx1-images').classList.contains('active')).toBe(false);
    });

    test('global switch resets single contexts', () => {
        setContextImageMode(document.getElementById('ctx1-noimages'), 'noimages');
        setGlobalImageMode('images');
        expect(document.getElementById('before1').getAttribute('src')).toBe('b1.png');
        expect(document.getElementById('ctx1').dataset.imageMode).toBeUndefined();
    });

    test('diff clicker shows the screenshots of the current mode', () => {
        setGlobalImageMode('noimages');
        initClickImage('b1.png', 'a1.png', 'd1.png', '0');
        expect(document.getElementById('clickimage').getAttribute('src')).toBe('d1_noimages.png');
        initClickImage('b1.png', 'a1.png', 'd1.png', '0');
        expect(document.getElementById('clickimage').getAttribute('src')).toBe('b1_noimages.png');
    });

    test('maps files only in no-images mode', () => {
        expect(imageModeFile('b1.png', false)).toBe('b1.png');
        setContextImageMode(document.getElementById('ctx1-noimages'), 'noimages');
        expect(imageModeFile('b1.png', false)).toBe('b1_noimages.png');
        expect(imageModeFile('d1.png', true)).toBe('d1_noimages.png');
        expect(imageModeFile('unknown.png', false)).toBe('unknown.png');
        expect(imageModeFile('null', false)).toBe('null');
    });
});

// ── Layout: two columns (slider) / three columns (side by side) ──────────────

describe('layout', () => {
    beforeEach(() => {
        document.body.className = '';
        document.body.innerHTML = `
            <button class="layout-toggle">2 columns</button>
            <button class="layout-toggle">2 columns</button>
        `;
        localStorage.clear();
    });

    test('toggles between two and three columns and remembers it', () => {
        toggleLayout();
        expect(document.body.classList.contains('layout-three')).toBe(true);
        expect(localStorage.getItem('jlineup-layout')).toBe('three');
        document.querySelectorAll('.layout-toggle').forEach(btn => {
            expect(btn.classList.contains('active')).toBe(true);
            expect(btn.textContent).toContain('3 columns');
        });

        toggleLayout();
        expect(document.body.classList.contains('layout-three')).toBe(false);
        expect(localStorage.getItem('jlineup-layout')).toBe('');
        document.querySelectorAll('.layout-toggle').forEach(btn => {
            expect(btn.classList.contains('active')).toBe(false);
            expect(btn.textContent).toContain('2 columns');
        });
    });

    test('notifies about layout changes', () => {
        const listener = jest.fn();
        document.addEventListener('jlineup-layout-changed', listener);
        applyLayout('three');
        document.removeEventListener('jlineup-layout-changed', listener);
        expect(listener).toHaveBeenCalledTimes(1);
    });

    test('zoom uses the side by side images in the three column layout', () => {
        const ctx = 'ctx-0';
        document.body.innerHTML = `
            <div id="zoombox" class="invisible"></div>
            <div class="legend" style="display:none"></div>
            <div id="zoom_before" class="zoom"></div>
            <div id="zoom_after" class="zoom"></div>
            <div id="zoom_diff" class="zoom"></div>
            <img id="before_${ctx}" src="before.png" width="100" height="200" />
            <img id="after_${ctx}" src="after.png" width="100" height="200" />
            <img id="sbs-before_${ctx}" src="before.png" width="100" height="200" />
            <img id="sbs-after_${ctx}" src="after.png" width="100" height="200" />
            <img id="diff_${ctx}" src="diff.png" width="100" height="200" />
            <div id="lens-before_${ctx}" class="zoom-lens"></div>
            <div id="lens-after_${ctx}" class="zoom-lens"></div>
            <div id="lens-sbs-before_${ctx}" class="zoom-lens"></div>
            <div id="lens-sbs-after_${ctx}" class="zoom-lens"></div>
            <div id="lens-difference_${ctx}" class="zoom-lens"></div>
        `;
        document.body.classList.add('layout-three');
        imageZoomForContext(ctx);
        expect(document.getElementById('lens-sbs-before_' + ctx).classList.contains('visible')).toBe(true);
        expect(document.getElementById('lens-sbs-after_' + ctx).classList.contains('visible')).toBe(true);
        expect(document.getElementById('lens-before_' + ctx).classList.contains('visible')).toBe(false);
        expect(document.getElementById('lens-after_' + ctx).classList.contains('visible')).toBe(false);
        expect(document.getElementById('lens-difference_' + ctx).classList.contains('visible')).toBe(true);
    });
});
