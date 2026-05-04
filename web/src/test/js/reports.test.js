'use strict';

// Set up the window._jlineup global that reports.js reads via `root`
global._jlineup = { runsUrl: '/runs' };

const {
    ACTIVE_STATES,
    STATE_LABELS,
    rowClass,
    logBtnClass,
    parseMsOrNull,
    durationMs,
    formatDuration,
    getReportUrl,
    getLogUrl,
    isBeforeReport,
    afterRunUrl,
    retryAfterUrl,
    runName,
    runUrls,
    formatStartTime,
    escHtml,
    buildRow,
    findInsertionPoint,
    patchRow,
} = require('../../main/resources/static/js/reports.js');

// ── helpers ──────────────────────────────────────────────────────────────────

function makeRun(overrides) {
    return Object.assign({
        id: 'run-1',
        state: 'FINISHED_WITHOUT_DIFFERENCES',
        startTime: '2024-01-15T10:00:00.000Z',
        endTime:   '2024-01-15T10:05:00.000Z',
        pauseTime: null,
        resumeTime: null,
        jobConfig: { name: 'My App', urls: { 'https://example.com': { paths: ['/'] } } },
        reports: { htmlUrl: '/report/after/index.html', logUrl: '/report/run.log' },
    }, overrides);
}

function makeRow(id, startTimeMs) {
    const tr = document.createElement('tr');
    tr.id = 'run-row-' + id;
    tr.setAttribute('data-start-time', String(startTimeMs));
    return tr;
}

function makeTbody(...rows) {
    const tbody = document.createElement('tbody');
    rows.forEach(r => tbody.appendChild(r));
    return tbody;
}

// ── rowClass ─────────────────────────────────────────────────────────────────

describe('rowClass', () => {
    test.each([
        ['FINISHED_WITHOUT_DIFFERENCES', 'table-success'],
        ['FINISHED_WITH_DIFFERENCES',    'table-warning'],
        ['ERROR',                        'table-danger'],
        ['DEAD',                         'table-danger'],
        ['BEFORE_RUNNING',               'table-info'],
        ['AFTER_RUNNING',                'table-info'],
        ['BEFORE_PENDING',               'table-secondary'],
        ['BEFORE_DONE',                  'table-secondary'],
        ['AFTER_PENDING',                'table-secondary'],
        ['UNKNOWN_STATE',                'table-secondary'],
    ])('state %s → %s', (state, expected) => {
        expect(rowClass(state)).toBe(expected);
    });
});

// ── logBtnClass ───────────────────────────────────────────────────────────────

describe('logBtnClass', () => {
    test('ERROR → btn-danger', () => expect(logBtnClass('ERROR')).toBe('btn-danger'));
    test('DEAD  → btn-danger', () => expect(logBtnClass('DEAD')).toBe('btn-danger'));
    test('other → btn-light',  () => expect(logBtnClass('FINISHED_WITHOUT_DIFFERENCES')).toBe('btn-light'));
});

// ── parseMsOrNull ─────────────────────────────────────────────────────────────

describe('parseMsOrNull', () => {
    test('valid ISO string → epoch ms', () => {
        expect(parseMsOrNull('2024-01-15T10:00:00.000Z')).toBe(new Date('2024-01-15T10:00:00.000Z').getTime());
    });
    test('null → null', () => expect(parseMsOrNull(null)).toBeNull());
    test('empty string → null', () => expect(parseMsOrNull('')).toBeNull());
});

// ── durationMs ────────────────────────────────────────────────────────────────

describe('durationMs', () => {
    test('fully finished run: (pauseTime-startTime) + (endTime-resumeTime)', () => {
        const run = makeRun({
            startTime:  '2024-01-15T10:00:00.000Z',
            pauseTime:  '2024-01-15T10:02:00.000Z',  // before step took 2 min
            resumeTime: '2024-01-15T10:10:00.000Z',  // waiting for after
            endTime:    '2024-01-15T10:13:00.000Z',  // after step took 3 min
        });
        expect(durationMs(run)).toBe((2 + 3) * 60 * 1000);
    });

    test('paused run: pauseTime - startTime', () => {
        const run = makeRun({
            state: 'BEFORE_DONE',
            startTime: '2024-01-15T10:00:00.000Z',
            pauseTime: '2024-01-15T10:02:00.000Z',
            endTime:   null,
        });
        expect(durationMs(run)).toBe(2 * 60 * 1000);
    });

    test('resumed and finished: pause segment + resume→end segment', () => {
        const run = makeRun({
            startTime:  '2024-01-15T10:00:00.000Z',
            pauseTime:  '2024-01-15T10:02:00.000Z',
            resumeTime: '2024-01-15T10:10:00.000Z',
            endTime:    '2024-01-15T10:15:00.000Z',
        });
        // pause segment: 2 min; resume→end segment: 5 min
        expect(durationMs(run)).toBe((2 + 5) * 60 * 1000);
    });

    test('no times → 0', () => {
        const run = makeRun({ startTime: null, endTime: null });
        expect(durationMs(run)).toBe(0);
    });
});

// ── formatDuration ────────────────────────────────────────────────────────────

describe('formatDuration', () => {
    test('zero',               () => expect(formatDuration(0)).toBe('00:00:00'));
    test('59 seconds',         () => expect(formatDuration(59000)).toBe('00:00:59'));
    test('1 minute',           () => expect(formatDuration(60000)).toBe('00:01:00'));
    test('1h 2m 3s',           () => expect(formatDuration(3723000)).toBe('01:02:03'));
    test('more than 24 hours', () => expect(formatDuration(90000000)).toBe('25:00:00'));
});

// ── getReportUrl / getLogUrl ──────────────────────────────────────────────────

describe('getReportUrl', () => {
    test('present', () => expect(getReportUrl(makeRun())).toBe('/report/after/index.html'));
    test('missing reports', () => expect(getReportUrl(makeRun({ reports: null }))).toBeNull());
    test('no htmlUrl',      () => expect(getReportUrl(makeRun({ reports: { logUrl: '/x' } }))).toBeNull());
});

describe('getLogUrl', () => {
    test('present', () => expect(getLogUrl(makeRun())).toBe('/report/run.log'));
    test('missing', () => expect(getLogUrl(makeRun({ reports: null }))).toBeNull());
});

// ── isBeforeReport ────────────────────────────────────────────────────────────

describe('isBeforeReport', () => {
    test('url containing "before" → true',  () => expect(isBeforeReport('/report/before/index.html')).toBe(true));
    test('url without "before" → false',    () => expect(isBeforeReport('/report/after/index.html')).toBe(false));
    test('null → falsy',                    () => expect(isBeforeReport(null)).toBeFalsy());
});

// ── afterRunUrl ───────────────────────────────────────────────────────────────

describe('afterRunUrl', () => {
    test('BEFORE_DONE → constructed URL', () => {
        const run = makeRun({ id: 'abc123', state: 'BEFORE_DONE' });
        expect(afterRunUrl(run)).toBe('/runs/abc123');
    });
    test('other state → null', () => {
        expect(afterRunUrl(makeRun({ state: 'BEFORE_RUNNING' }))).toBeNull();
    });
});

// ── runName ───────────────────────────────────────────────────────────────────

describe('runName', () => {
    test('present', () => expect(runName(makeRun())).toBe('My App'));
    test('no jobConfig', () => expect(runName(makeRun({ jobConfig: null }))).toBeNull());
    test('no name',      () => expect(runName(makeRun({ jobConfig: { urls: {} } }))).toBeNull());
});

// ── runUrls ───────────────────────────────────────────────────────────────────

describe('runUrls', () => {
    test('single url with default path', () => {
        const run = makeRun({ jobConfig: { urls: { 'https://example.com': {} } } });
        expect(runUrls(run)).toEqual(['https://example.com/']);
    });

    test('single url with explicit paths', () => {
        const run = makeRun({ jobConfig: { urls: { 'https://example.com': { paths: ['/foo', '/bar'] } } } });
        expect(runUrls(run)).toEqual(['https://example.com/foo', 'https://example.com/bar']);
    });

    test('multiple urls', () => {
        const run = makeRun({ jobConfig: { urls: {
            'https://a.com': { paths: ['/'] },
            'https://b.com': { paths: ['/x'] },
        } } });
        expect(runUrls(run)).toEqual(['https://a.com/', 'https://b.com/x']);
    });

    test('no jobConfig → []', () => expect(runUrls(makeRun({ jobConfig: null }))).toEqual([]));
});

// ── formatStartTime ───────────────────────────────────────────────────────────

describe('formatStartTime', () => {
    test('formats ISO to local datetime string', () => {
        // Use a fixed UTC time and check the output matches the pattern YYYY-MM-DD HH:MM
        const result = formatStartTime('2024-06-15T00:00:00.000Z');
        expect(result).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$/);
    });
    test('null → empty string', () => expect(formatStartTime(null)).toBe(''));
    test('empty string → empty string', () => expect(formatStartTime('')).toBe(''));
});

// ── escHtml ───────────────────────────────────────────────────────────────────

describe('escHtml', () => {
    test('escapes all special chars', () => {
        expect(escHtml('<script>"a" & \'b\'</script>')).toBe('&lt;script&gt;&quot;a&quot; &amp; \'b\'&lt;/script&gt;');
    });
    test('null → empty string', () => expect(escHtml(null)).toBe(''));
    test('numbers coerced', () => expect(escHtml(42)).toBe('42'));
});

// ── buildRow ──────────────────────────────────────────────────────────────────

describe('buildRow', () => {
    test('sets id and class', () => {
        const tr = buildRow(makeRun({ id: 'x1', state: 'FINISHED_WITHOUT_DIFFERENCES' }));
        expect(tr.id).toBe('run-row-x1');
        expect(tr.className).toBe('table-success');
    });

    test('data-start-time attribute is epoch ms of startTime', () => {
        const iso = '2024-01-15T10:00:00.000Z';
        const tr = buildRow(makeRun({ startTime: iso }));
        expect(tr.getAttribute('data-start-time')).toBe(String(new Date(iso).getTime()));
    });

    test('data-start-time is 0 when startTime is null', () => {
        const tr = buildRow(makeRun({ startTime: null }));
        expect(tr.getAttribute('data-start-time')).toBe('0');
    });

    test('shows state label in run-state cell', () => {
        const tr = buildRow(makeRun({ state: 'BEFORE_RUNNING' }));
        expect(tr.querySelector('.run-state').textContent).toBe("'before' running");
    });

    test('report button uses btn-primary for after report', () => {
        const tr = buildRow(makeRun({ reports: { htmlUrl: '/report/after/index.html', logUrl: '/log' } }));
        const a = tr.querySelector('td:nth-child(7) a');
        expect(a.classList.contains('btn-primary')).toBe(true);
    });

    test('report button uses btn-secondary for before report', () => {
        const tr = buildRow(makeRun({ reports: { htmlUrl: '/report/before/index.html', logUrl: '/log' } }));
        const a = tr.querySelector('td:nth-child(7) a');
        expect(a.classList.contains('btn-secondary')).toBe(true);
    });

    test('log button uses btn-danger for ERROR', () => {
        const tr = buildRow(makeRun({ state: 'ERROR', reports: { logUrl: '/log' } }));
        const a = tr.querySelector('td:nth-child(8) a');
        expect(a.classList.contains('btn-danger')).toBe(true);
    });

    test('after-run button present for BEFORE_DONE', () => {
        const tr = buildRow(makeRun({ id: 'r1', state: 'BEFORE_DONE', reports: null }));
        const btn = tr.querySelector('.start-after-btn');
        expect(btn).not.toBeNull();
        expect(btn.getAttribute('data-after-url')).toBe('/runs/r1');
    });

    test('no after-run button for FINISHED state', () => {
        const tr = buildRow(makeRun({ state: 'FINISHED_WITHOUT_DIFFERENCES' }));
        expect(tr.querySelector('.start-after-btn')).toBeNull();
    });

    test('escapes HTML in id field', () => {
        const tr = buildRow(makeRun({ id: '<b>evil</b>' }));
        expect(tr.innerHTML).toContain('&lt;b&gt;evil&lt;/b&gt;');
    });
});

// ── findInsertionPoint ────────────────────────────────────────────────────────

describe('findInsertionPoint', () => {
    test('empty tbody → null (append)', () => {
        expect(findInsertionPoint(makeTbody(), 1000)).toBeNull();
    });

    test('newest run goes before all existing rows', () => {
        const old1 = makeRow('a', 1000);
        const old2 = makeRow('b', 500);
        const tbody = makeTbody(old1, old2);
        expect(findInsertionPoint(tbody, 2000)).toBe(old1);
    });

    test('run goes between existing rows', () => {
        const newer = makeRow('a', 3000);
        const older = makeRow('b', 1000);
        const tbody = makeTbody(newer, older);
        // newTs=2000 > 1000(older), but 2000 < 3000(newer) so insert before older
        expect(findInsertionPoint(tbody, 2000)).toBe(older);
    });

    test('oldest run is appended (returns null)', () => {
        const row = makeRow('a', 3000);
        const tbody = makeTbody(row);
        expect(findInsertionPoint(tbody, 500)).toBeNull();
    });

    test('equal timestamp: new row goes after existing (returns null on tie-at-end)', () => {
        const row = makeRow('a', 1000);
        const tbody = makeTbody(row);
        // newTs === rowTs → not strictly greater, so scan continues → null (append)
        expect(findInsertionPoint(tbody, 1000)).toBeNull();
    });
});

// ── patchRow ──────────────────────────────────────────────────────────────────

describe('patchRow', () => {
    function buildExistingRow(state) {
        return buildRow(makeRun({ state }));
    }

    test('updates row CSS class when state changes', () => {
        const tr = buildExistingRow('BEFORE_RUNNING');
        patchRow(tr, makeRun({ state: 'FINISHED_WITHOUT_DIFFERENCES' }));
        expect(tr.className).toContain('table-success');
        expect(tr.className).not.toContain('table-info');
    });

    test('updates state label text', () => {
        const tr = buildExistingRow('BEFORE_RUNNING');
        patchRow(tr, makeRun({ state: 'FINISHED_WITH_DIFFERENCES' }));
        expect(tr.querySelector('.run-state').textContent).toBe('finished with differences');
    });

    test('adds report button when it appears mid-run', () => {
        const tr = buildRow(makeRun({ state: 'BEFORE_RUNNING', reports: null }));
        expect(tr.querySelector('td:nth-child(7) a')).toBeNull();
        patchRow(tr, makeRun({ state: 'BEFORE_DONE', reports: { htmlUrl: '/before/index.html' } }));
        const a = tr.querySelector('td:nth-child(7) a');
        expect(a).not.toBeNull();
        expect(a.href).toContain('/before/index.html');
    });

    test('adds log button when it appears mid-run', () => {
        const tr = buildRow(makeRun({ state: 'BEFORE_RUNNING', reports: null }));
        expect(tr.querySelector('td:nth-child(8) a')).toBeNull();
        patchRow(tr, makeRun({ state: 'BEFORE_DONE', reports: { logUrl: '/run.log' } }));
        expect(tr.querySelector('td:nth-child(8) a')).not.toBeNull();
    });

    test('log button class updated to btn-danger on ERROR', () => {
        const tr = buildExistingRow('BEFORE_RUNNING');
        // patch to ERROR with existing log button
        patchRow(tr, makeRun({ state: 'ERROR', reports: { logUrl: '/run.log' } }));
        // Log btn may be added by patchRow; also verify it exists
        const logBtn = tr.querySelector('td:nth-child(8) a');
        expect(logBtn).not.toBeNull();
        expect(logBtn.classList.contains('btn-danger')).toBe(true);
    });

    test('removes after-run button when state leaves BEFORE_DONE', () => {
        const tr = buildRow(makeRun({ id: 'r1', state: 'BEFORE_DONE', reports: null }));
        expect(tr.querySelector('.start-after-btn')).not.toBeNull();
        patchRow(tr, makeRun({ id: 'r1', state: 'AFTER_RUNNING', reports: null }));
        expect(tr.querySelector('.start-after-btn')).toBeNull();
    });
});

// ── ACTIVE_STATES / STATE_LABELS completeness ─────────────────────────────────

describe('ACTIVE_STATES', () => {
    test('contains all expected active states', () => {
        ['BEFORE_PENDING','BEFORE_RUNNING','BEFORE_DONE','AFTER_PENDING','AFTER_RUNNING'].forEach(s => {
            expect(ACTIVE_STATES.has(s)).toBe(true);
        });
    });
    test('does not contain terminal states', () => {
        ['FINISHED_WITHOUT_DIFFERENCES','FINISHED_WITH_DIFFERENCES','ERROR','DEAD'].forEach(s => {
            expect(ACTIVE_STATES.has(s)).toBe(false);
        });
    });
});

describe('STATE_LABELS', () => {
    test('all active states have labels', () => {
        ACTIVE_STATES.forEach(s => {
            expect(STATE_LABELS[s]).toBeDefined();
        });
    });
});

// ── retryAfterUrl ─────────────────────────────────────────────────────────────

describe('retryAfterUrl', () => {
    test('ERROR → constructed retry URL', () => {
        const run = makeRun({ id: 'abc123', state: 'ERROR' });
        expect(retryAfterUrl(run)).toBe('/runs/abc123/retry');
    });

    test('DEAD → constructed retry URL', () => {
        const run = makeRun({ id: 'abc123', state: 'DEAD' });
        expect(retryAfterUrl(run)).toBe('/runs/abc123/retry');
    });

    test('FINISHED_WITH_DIFFERENCES → constructed retry URL', () => {
        const run = makeRun({ id: 'abc123', state: 'FINISHED_WITH_DIFFERENCES' });
        expect(retryAfterUrl(run)).toBe('/runs/abc123/retry');
    });

    test('FINISHED_WITHOUT_DIFFERENCES → null', () => {
        expect(retryAfterUrl(makeRun({ state: 'FINISHED_WITHOUT_DIFFERENCES' }))).toBeNull();
    });

    test('BEFORE_DONE → null', () => {
        expect(retryAfterUrl(makeRun({ state: 'BEFORE_DONE' }))).toBeNull();
    });

    test('BEFORE_RUNNING → null', () => {
        expect(retryAfterUrl(makeRun({ state: 'BEFORE_RUNNING' }))).toBeNull();
    });
});

// ── buildRow retry button ────────────────────────────────────────────────────

describe('buildRow retry button', () => {
    test('retry button present for ERROR state', () => {
        const tr = buildRow(makeRun({ id: 'r1', state: 'ERROR', reports: { logUrl: '/log' } }));
        const btn = tr.querySelector('.retry-after-btn');
        expect(btn).not.toBeNull();
        expect(btn.getAttribute('data-retry-url')).toBe('/runs/r1/retry');
    });

    test('retry button present for DEAD state', () => {
        const tr = buildRow(makeRun({ id: 'r1', state: 'DEAD', reports: null }));
        const btn = tr.querySelector('.retry-after-btn');
        expect(btn).not.toBeNull();
        expect(btn.getAttribute('data-retry-url')).toBe('/runs/r1/retry');
    });

    test('retry button present for FINISHED_WITH_DIFFERENCES', () => {
        const tr = buildRow(makeRun({ id: 'r1', state: 'FINISHED_WITH_DIFFERENCES' }));
        const btn = tr.querySelector('.retry-after-btn');
        expect(btn).not.toBeNull();
    });

    test('no retry button for FINISHED_WITHOUT_DIFFERENCES', () => {
        const tr = buildRow(makeRun({ state: 'FINISHED_WITHOUT_DIFFERENCES' }));
        expect(tr.querySelector('.retry-after-btn')).toBeNull();
    });

    test('no retry button for BEFORE_DONE', () => {
        const tr = buildRow(makeRun({ state: 'BEFORE_DONE', reports: null }));
        expect(tr.querySelector('.retry-after-btn')).toBeNull();
    });
});

// ── patchRow retry button ────────────────────────────────────────────────────

describe('patchRow retry button', () => {
    test('adds retry button when state changes to ERROR', () => {
        const tr = buildRow(makeRun({ id: 'r1', state: 'AFTER_RUNNING', reports: null }));
        expect(tr.querySelector('.retry-after-btn')).toBeNull();
        patchRow(tr, makeRun({ id: 'r1', state: 'ERROR', reports: { logUrl: '/log' } }));
        const btn = tr.querySelector('.retry-after-btn');
        expect(btn).not.toBeNull();
        expect(btn.getAttribute('data-retry-url')).toBe('/runs/r1/retry');
    });

    test('removes retry button when state changes from ERROR to AFTER_RUNNING (re-running)', () => {
        const tr = buildRow(makeRun({ id: 'r1', state: 'ERROR', reports: { logUrl: '/log' } }));
        expect(tr.querySelector('.retry-after-btn')).not.toBeNull();
        patchRow(tr, makeRun({ id: 'r1', state: 'AFTER_RUNNING', reports: { logUrl: '/log' } }));
        expect(tr.querySelector('.retry-after-btn')).toBeNull();
    });
});
