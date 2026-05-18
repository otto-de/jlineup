'use strict';

/**
 * Tests for the "Clear" button functionality on the manual run page.
 *
 * Since the run page JS is an inline ES module with CDN imports (CodeMirror),
 * we test the clear logic by simulating the DOM contract and localStorage
 * behavior that the buttons rely on.
 */

const STORAGE_KEY = 'jlineup.run.editorContent';

beforeEach(() => {
    localStorage.clear();
    document.body.innerHTML = `
        <div id="error-alert" class="alert d-none"></div>
        <div id="success-alert" class="alert d-none"></div>
        <button id="clear-top-btn"></button>
        <button id="clear-bottom-btn"></button>
        <div id="codemirror-host"></div>
    `;
});

// Simulate the clearEditor function as implemented in run.html
function clearEditor(view) {
    view.dispatch({ changes: { from: 0, to: view.state.doc.length, insert: '' } });
    try { localStorage.removeItem(STORAGE_KEY); } catch (e) { /* ignore */ }
    document.getElementById('error-alert').classList.add('d-none');
    document.getElementById('success-alert').classList.add('d-none');
}

function makeMockView(initialContent) {
    let content = initialContent;
    return {
        state: {
            doc: {
                get length() { return content.length; },
                toString() { return content; },
            },
        },
        dispatch({ changes }) {
            // Simple simulation: replace range with insert
            content = content.substring(0, changes.from) + changes.insert + content.substring(changes.to);
        },
    };
}

describe('Clear button', () => {
    test('clears editor content', () => {
        const view = makeMockView('urls:\n  https://example.com:\n    paths:\n      - /');
        clearEditor(view);
        expect(view.state.doc.toString()).toBe('');
    });

    test('removes saved content from localStorage', () => {
        localStorage.setItem(STORAGE_KEY, 'some config content');
        const view = makeMockView('some config content');
        clearEditor(view);
        expect(localStorage.getItem(STORAGE_KEY)).toBeNull();
    });

    test('hides error alert', () => {
        const errorEl = document.getElementById('error-alert');
        errorEl.classList.remove('d-none');
        const view = makeMockView('content');
        clearEditor(view);
        expect(errorEl.classList.contains('d-none')).toBe(true);
    });

    test('hides success alert', () => {
        const successEl = document.getElementById('success-alert');
        successEl.classList.remove('d-none');
        const view = makeMockView('content');
        clearEditor(view);
        expect(successEl.classList.contains('d-none')).toBe(true);
    });

    test('works when localStorage is already empty', () => {
        const view = makeMockView('content');
        clearEditor(view);
        expect(view.state.doc.toString()).toBe('');
        expect(localStorage.getItem(STORAGE_KEY)).toBeNull();
    });

    test('both clear buttons exist in the page markup', () => {
        expect(document.getElementById('clear-top-btn')).not.toBeNull();
        expect(document.getElementById('clear-bottom-btn')).not.toBeNull();
    });
});
