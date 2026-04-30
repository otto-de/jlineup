'use strict';

const fs   = require('fs');
const path = require('path');

// ── Load and eval the inline script from report.html ─────────────────────────
//
// report.html is a standalone self-contained file.
// We read its <script type="application/javascript"> block and run it in the
// jsdom global context so its functions (initClickImage, closeClickDiff, etc.)
// are available as globals in every test.

const htmlPath = path.resolve(
    __dirname,
    '../../main/resources/templates/report.html'
);
const html = fs.readFileSync(htmlPath, 'utf8');

// Extract the first <script type="application/javascript"> … </script> block
const scriptMatch = html.match(
    /<script\s+type="application\/javascript">([\s\S]*?)<\/script>/
);
if (!scriptMatch) throw new Error('Could not find application/javascript script in report.html');
const scriptSrc = scriptMatch[1];

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
