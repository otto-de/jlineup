'use strict';

const fs   = require('fs');
const path = require('path');
const util = require('util');

// ── Load the archive script from the shared fragment ─────────────────────────

const fragmentPath = path.resolve(
    __dirname,
    '../../main/resources/templates/archive_download.html'
);
const fragment = fs.readFileSync(fragmentPath, 'utf8');
const scriptMatch = fragment.match(/<script[^>]*>([\s\S]*?)<\/script>/);
if (!scriptMatch) throw new Error('Could not find script in archive_download.html');

if (typeof globalThis.TextEncoder === 'undefined') globalThis.TextEncoder = util.TextEncoder;
if (typeof globalThis.TextDecoder === 'undefined') globalThis.TextDecoder = util.TextDecoder;

// eslint-disable-next-line no-eval
eval(`(function() { ${scriptMatch[1]}\n globalThis.JLineupArchive = JLineupArchive; }())`);

const A = globalThis.JLineupArchive;

// ── helpers ──────────────────────────────────────────────────────────────────

const bytesOf = text => new TextEncoder().encode(text);

function concat(parts) {
    const total = parts.reduce((sum, p) => sum + p.length, 0);
    const out = new Uint8Array(total);
    let offset = 0;
    parts.forEach(p => { out.set(p, offset); offset += p.length; });
    return out;
}

// Minimal zip reader: walks the central directory and returns {name: Uint8Array}
function readZip(bytes) {
    const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
    const eocd = bytes.length - 22;
    expect(view.getUint32(eocd, true)).toBe(0x06054b50);
    const count = view.getUint16(eocd + 10, true);
    let pos = view.getUint32(eocd + 16, true);
    const files = {};
    for (let i = 0; i < count; i++) {
        expect(view.getUint32(pos, true)).toBe(0x02014b50);
        const crc = view.getUint32(pos + 16, true);
        const size = view.getUint32(pos + 24, true);
        const nameLength = view.getUint16(pos + 28, true);
        const localOffset = view.getUint32(pos + 42, true);
        const name = new TextDecoder().decode(bytes.subarray(pos + 46, pos + 46 + nameLength));
        expect(view.getUint32(localOffset, true)).toBe(0x04034b50);
        const localNameLength = view.getUint16(localOffset + 26, true);
        const dataStart = localOffset + 30 + localNameLength;
        const data = bytes.slice(dataStart, dataStart + size);
        expect(A.crc32(data)).toBe(crc);
        files[name] = data;
        pos += 46 + nameLength;
    }
    return files;
}

function memorySink() {
    return {
        files: {},
        add(name, bytes) { this.files[name] = bytes; },
        finish() { return null; }
    };
}

function fakeFetch(responses) {
    return jest.fn(async url => {
        const hit = responses[url];
        if (!hit) return {ok: false, status: 404, headers: {get: () => 'text/plain'}};
        return {
            ok: hit.ok !== false,
            status: hit.status || 200,
            headers: {get: () => hit.contentType || 'image/png'},
            arrayBuffer: async () => {
                const b = hit.body instanceof Uint8Array ? hit.body : bytesOf(hit.body || 'x');
                return b.buffer.slice(b.byteOffset, b.byteOffset + b.byteLength);
            }
        };
    });
}

const PAGE = 'https://example.com/reports/report-1/report/report.html';

function buildPage() {
    document.body.innerHTML = `
        <header class="report-header"><h1>My Job / 1</h1></header>
        <img id="b1" src="../screenshots/ab12/before.png"
             data-src-images="../screenshots/ab12/before.png"
             data-src-noimages="../screenshots/ab12/before_noimages.png"/>
        <img id="clickimage"/>
        <img src="https://cdn.example.org/external.png"/>
        <img src="data:image/png;base64,AAAA"/>
        <a href="../screenshots/ab12/before.png"
           data-href-images="../screenshots/ab12/before.png"
           data-href-noimages="../screenshots/ab12/before_noimages.png">Before</a>
        <a href="../screenshots/ab12/diff.png">Diff</a>
        <a href="https://example.com/some/page">Open page</a>
        <a href="#url-1">toc</a>
    `;
}

// ── tests ────────────────────────────────────────────────────────────────────

describe('crc32', () => {
    test('matches the known check value', () => {
        expect(A.crc32(bytesOf('123456789'))).toBe(0xCBF43926);
    });

    test('of empty input is 0', () => {
        expect(A.crc32(new Uint8Array(0))).toBe(0);
    });
});

describe('ZipBuilder', () => {
    test('writes a readable store-only zip with utf-8 names', () => {
        const builder = new A.ZipBuilder(new Date(2024, 4, 17, 13, 37, 42));
        builder.add('root/a.txt', bytesOf('hello'));
        builder.add('root/sub/ü.bin', new Uint8Array([0, 1, 2, 255]));
        builder.add('root/empty', new Uint8Array(0));

        const files = readZip(concat(builder.finish()));

        expect(Object.keys(files)).toEqual(['root/a.txt', 'root/sub/ü.bin', 'root/empty']);
        expect(new TextDecoder().decode(files['root/a.txt'])).toBe('hello');
        expect(Array.from(files['root/sub/ü.bin'])).toEqual([0, 1, 2, 255]);
        expect(files['root/empty'].length).toBe(0);
    });
});

describe('collectResourceUrls', () => {
    test('collects same-origin screenshots including the images / noimages variants', () => {
        buildPage();

        const urls = A.collectResourceUrls(document, PAGE);

        expect(urls.sort()).toEqual([
            'https://example.com/reports/report-1/screenshots/ab12/before.png',
            'https://example.com/reports/report-1/screenshots/ab12/before_noimages.png',
            'https://example.com/reports/report-1/screenshots/ab12/diff.png'
        ]);
    });
});

describe('computeEntryNames', () => {
    test('keeps layout below the deepest shared directory', () => {
        const names = A.computeEntryNames([
            PAGE,
            'https://example.com/reports/report-1/report/report.json',
            'https://example.com/reports/report-1/screenshots/ab12/before.png'
        ]);

        expect(Array.from(names.values())).toEqual([
            'report/report.html',
            'report/report.json',
            'screenshots/ab12/before.png'
        ]);
    });

    test('uses plain file names if everything is in one directory', () => {
        const names = A.computeEntryNames([
            'https://example.com/a/report.html',
            'https://example.com/a/img%20one.png'
        ]);

        expect(Array.from(names.values())).toEqual(['report.html', 'img one.png']);
    });
});

describe('archiveBaseName', () => {
    test('contains job name, kind of report and timestamp', () => {
        buildPage();

        expect(A.archiveBaseName(document, PAGE, new Date(2024, 0, 2, 3, 4, 5)))
            .toBe('jlineup-My-Job-1-report-20240102-030405');
    });

    test('works for the reference report without a heading', () => {
        document.body.innerHTML = '';

        expect(A.archiveBaseName(document, 'https://example.com/x/report_before.html', new Date(2024, 0, 2, 3, 4, 5)))
            .toBe('jlineup-report_before-20240102-030405');
    });
});

describe('createArchive', () => {
    const base = 'https://example.com/reports/report-1/';

    test('archives page, siblings and screenshots and writes INDEX.md', async () => {
        buildPage();
        const fetch = fakeFetch({
            [PAGE]: {body: '<html></html>', contentType: 'text/html'},
            [base + 'report/report.json']: {body: '{}', contentType: 'application/json'},
            [base + 'screenshots/ab12/before.png']: {body: 'B'},
            [base + 'screenshots/ab12/before_noimages.png']: {body: 'BN'},
            [base + 'screenshots/ab12/diff.png']: {body: 'D'}
        });
        const sink = memorySink();
        const progress = [];

        const result = await A.createArchive({
            doc: document, pageUrl: PAGE + '#url-1', sink, fetch,
            title: 'My report', now: new Date('2024-01-02T03:04:05Z'),
            onProgress: (done, total) => progress.push([done, total])
        });

        expect(Object.keys(sink.files).sort()).toEqual([
            'INDEX.md',
            'report/report.html',
            'report/report.json',
            'screenshots/ab12/before.png',
            'screenshots/ab12/before_noimages.png',
            'screenshots/ab12/diff.png'
        ]);
        expect(result.files).toBe(5);
        expect(result.failed).toEqual([]);
        expect(result.skipped.map(s => s.name).sort()).toEqual(['report/files.json', 'report/jlineup.log']);
        expect(progress[0]).toEqual([0, 7]);
        expect(progress[progress.length - 1]).toEqual([7, 7]);

        const index = new TextDecoder().decode(sink.files['INDEX.md']);
        expect(index).toContain('Open this file in a browser: `report/report.html`');
        expect(index).toContain('Archived at: 2024-01-02T03:04:05.000Z');
        expect(index).not.toContain('could not be downloaded');
        expect(fetch).not.toHaveBeenCalledWith(expect.stringContaining('external.png'), expect.anything());
    });

    test('lists screenshots that could not be fetched, but keeps going', async () => {
        buildPage();
        const fetch = fakeFetch({
            [PAGE]: {body: '<html></html>', contentType: 'text/html'},
            [base + 'screenshots/ab12/before.png']: {body: 'B'},
            [base + 'screenshots/ab12/before_noimages.png']: {body: '<html>login</html>', contentType: 'text/html'}
        });
        const sink = memorySink();

        const result = await A.createArchive({doc: document, pageUrl: PAGE, sink, fetch});

        expect(result.failed.map(f => f.name).sort()).toEqual([
            'screenshots/ab12/before_noimages.png',
            'screenshots/ab12/diff.png'
        ]);
        expect(result.failed.find(f => f.name.endsWith('before_noimages.png')).error).toMatch(/not logged in/);
        const index = new TextDecoder().decode(sink.files['INDEX.md']);
        expect(index).toContain('## Files that could not be downloaded');
        expect(index).toContain('screenshots/ab12/diff.png');
    });

    test('fails if the report page itself cannot be fetched', async () => {
        buildPage();
        const sink = memorySink();

        await expect(A.createArchive({doc: document, pageUrl: PAGE, sink, fetch: fakeFetch({})}))
            .rejects.toThrow('HTTP 404');
    });

    test('fails if the sink fails', async () => {
        buildPage();
        const fetch = fakeFetch({[PAGE]: {body: 'x', contentType: 'text/html'}});
        const sink = {add() { throw new Error('disk full'); }};

        await expect(A.createArchive({doc: document, pageUrl: PAGE, sink, fetch}))
            .rejects.toThrow('disk full');
    });
});

describe('ZipSink', () => {
    test('puts all files below the root folder', () => {
        const sink = new A.ZipSink('jlineup-x');
        sink.add('report.html', bytesOf('<html/>'));

        const files = readZip(concat(sink.builder.finish()));

        expect(Object.keys(files)).toEqual(['jlineup-x/report.html']);
    });
});

describe('FolderSink', () => {
    function fakeDirectory(tree) {
        return {
            async getDirectoryHandle(name) {
                tree[name] = tree[name] || {};
                return fakeDirectory(tree[name]);
            },
            async getFileHandle(name) {
                return {
                    async createWritable() {
                        return {
                            async write(bytes) { tree[name] = Array.from(bytes); },
                            async close() {}
                        };
                    }
                };
            }
        };
    }

    test('creates the directories of the entry names and writes the files', async () => {
        const tree = {};
        const sink = new A.FolderSink(fakeDirectory(tree));

        await sink.add('report/report.html', new Uint8Array([1]));
        await sink.add('screenshots/ab/x.png', new Uint8Array([2, 3]));
        await sink.add('INDEX.md', new Uint8Array([4]));

        expect(tree).toEqual({
            report: {'report.html': [1]},
            screenshots: {ab: {'x.png': [2, 3]}},
            'INDEX.md': [4]
        });
    });
});
