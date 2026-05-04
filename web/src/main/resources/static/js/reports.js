// reports.js — polling + DOM logic for the JLineup reports page.
// Browser: loaded via <script src="...reports.js">, reads window._jlineup.runsUrl.
// Tests:   imported as a CommonJS module; pure helpers are exported at the bottom.

(function (root) {

    // ── State helpers ────────────────────────────────────────────────────────

    var ACTIVE_STATES = new Set([
        'BEFORE_PENDING', 'BEFORE_RUNNING', 'BEFORE_DONE', 'AFTER_PENDING', 'AFTER_RUNNING'
    ]);

    var STATE_LABELS = {
        BEFORE_PENDING:               "'before' pending",
        BEFORE_RUNNING:               "'before' running",
        BEFORE_DONE:                  "'before' done",
        AFTER_PENDING:                "'after' pending",
        AFTER_RUNNING:                "'after' running",
        FINISHED_WITHOUT_DIFFERENCES: "finished without differences",
        FINISHED_WITH_DIFFERENCES:    "finished with differences",
        ERROR:                        "error",
        DEAD:                         "dead"
    };

    var STATE_ROW_CLASS = {
        FINISHED_WITHOUT_DIFFERENCES: 'table-success',
        FINISHED_WITH_DIFFERENCES:    'table-warning',
        ERROR:                        'table-danger',
        DEAD:                         'table-danger',
        BEFORE_RUNNING:               'table-info',
        AFTER_RUNNING:                'table-info'
    };

    function rowClass(state) {
        return STATE_ROW_CLASS[state] || 'table-secondary';
    }

    function logBtnClass(state) {
        return (state === 'ERROR' || state === 'DEAD') ? 'btn-danger' : 'btn-light';
    }

    // ── Duration helper (mirrors server-side getDurationAsString) ────────────

    function parseMsOrNull(str) {
        return str ? new Date(str).getTime() : null;
    }

    function durationMs(run) {
        var startMs  = parseMsOrNull(run.startTime);
        var pauseMs  = parseMsOrNull(run.pauseTime);
        var resumeMs = parseMsOrNull(run.resumeTime);
        var endMs    = parseMsOrNull(run.endTime);
        var total    = 0;
        var now      = Date.now();

        if (pauseMs)  total += pauseMs - startMs;
        if (endMs)    total += endMs   - (resumeMs || endMs);
        if (run.state === 'BEFORE_RUNNING') total += now - startMs;
        if (run.state === 'AFTER_RUNNING')  total += now - (resumeMs || now);
        return total;
    }

    function formatDuration(ms) {
        var s  = Math.floor(ms / 1000);
        var HH = Math.floor(s / 3600);
        var MM = Math.floor((s % 3600) / 60);
        var SS = s % 60;
        return String(HH).padStart(2,'0') + ':' + String(MM).padStart(2,'0') + ':' + String(SS).padStart(2,'0');
    }

    // ── Pure data helpers ────────────────────────────────────────────────────

    function getReportUrl(run) {
        return run.reports && run.reports.htmlUrl ? run.reports.htmlUrl : null;
    }

    function getLogUrl(run) {
        return run.reports && run.reports.logUrl ? run.reports.logUrl : null;
    }

    function isBeforeReport(url) {
        return url && url.includes('before');
    }

    function afterRunUrl(run) {
        return run.state === 'BEFORE_DONE'
            ? root._jlineup.runsUrl + '/' + run.id
            : null;
    }

    var RETRYABLE_STATES = new Set(['ERROR', 'DEAD', 'FINISHED_WITH_DIFFERENCES']);

    function retryAfterUrl(run) {
        return RETRYABLE_STATES.has(run.state)
            ? root._jlineup.runsUrl + '/' + run.id + '/retry'
            : null;
    }

    function runName(run) {
        return run.jobConfig && run.jobConfig.name ? run.jobConfig.name : null;
    }

    function runUrls(run) {
        if (!run.jobConfig || !run.jobConfig.urls) return [];
        var result = [];
        Object.entries(run.jobConfig.urls).forEach(function(entry) {
            var url   = entry[0];
            var cfg   = entry[1];
            var paths = (cfg.paths && cfg.paths.length) ? cfg.paths : ['/'];
            paths.forEach(function(p) { result.push(url + p); });
        });
        return result;
    }

    function formatStartTime(iso) {
        if (!iso) return '';
        var d   = new Date(iso);
        var pad = function(n) { return String(n).padStart(2,'0'); };
        return d.getFullYear() + '-' + pad(d.getMonth()+1) + '-' + pad(d.getDate()) +
               ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    }

    function escHtml(s) {
        if (s == null) return '';
        return String(s)
            .replace(/&/g,'&amp;')
            .replace(/</g,'&lt;')
            .replace(/>/g,'&gt;')
            .replace(/"/g,'&quot;');
    }

    // ── DOM builders ─────────────────────────────────────────────────────────

    // Build a full <tr> for a run (used when inserting newly-seen rows during polling)
    function buildRow(run) {
        var tr  = document.createElement('tr');
        tr.id   = 'run-row-' + run.id;
        tr.className = rowClass(run.state);
        tr.setAttribute('data-start-time', run.startTime ? new Date(run.startTime).getTime() : 0);

        var reportUrl = getReportUrl(run);
        var logUrl    = getLogUrl(run);
        var aUrl      = afterRunUrl(run);
        var rUrl      = retryAfterUrl(run);
        var name      = runName(run);
        var urls      = runUrls(run);

        tr.innerHTML =
            '<td><div>' + escHtml(run.id) + '</div></td>' +
            '<td><div>' + (name ? escHtml(name) : '') + '</div></td>' +
            '<td><pre style="white-space:pre-line;">' + escHtml(urls.join('\n')) + '</pre></td>' +
            '<td class="run-state">' + escHtml(STATE_LABELS[run.state] || run.state) + '</td>' +
            '<td>' + escHtml(formatStartTime(run.startTime)) + '</td>' +
            '<td class="run-duration">' + formatDuration(durationMs(run)) + '</td>' +
            '<td>' + (reportUrl
                ? '<a href="' + escHtml(reportUrl) + '" target="_blank" class="btn ' +
                  (isBeforeReport(reportUrl) ? 'btn-secondary' : 'btn-primary') +
                  '" role="button">Report</a>'
                : '') + '</td>' +
            '<td>' + (logUrl
                ? '<a href="' + escHtml(logUrl) + '" target="_blank" class="btn ' +
                  logBtnClass(run.state) + '" role="button">Log</a>'
                : '') + '</td>' +
            '<td>' + (aUrl
                ? '<button type="button" class="btn btn-warning start-after-btn"' +
                  ' data-run-id="' + escHtml(run.id) + '"' +
                  ' data-run-name="' + escHtml(name || '') + '"' +
                  ' data-after-url="' + escHtml(aUrl) + '">Start \'After\' run</button>'
                : '') +
              (rUrl
                ? '<button type="button" class="btn btn-danger retry-after-btn"' +
                  ' data-run-id="' + escHtml(run.id) + '"' +
                  ' data-run-name="' + escHtml(name || '') + '"' +
                  ' data-retry-url="' + escHtml(rUrl) + '">Retry \'after\' run</button>'
                : '') + '</td>';
        return tr;
    }

    // Find the correct insertion point for a new row so tbody stays sorted
    // by data-start-time descending (newest first).
    // Returns the existing <tr> to insert before, or null (= append at end).
    function findInsertionPoint(tbody, newTs) {
        var rows = Array.from(tbody.querySelectorAll('tr[data-start-time]'));
        for (var i = 0; i < rows.length; i++) {
            var rowTs = parseInt(rows[i].getAttribute('data-start-time'), 10) || 0;
            if (newTs > rowTs) return rows[i];
        }
        return null;
    }

    // Patch an existing row in-place (only touch what changed)
    function patchRow(tr, run) {
        // Row CSS class
        var keep = Array.from(tr.classList).filter(function(c) { return !c.startsWith('table-'); });
        keep.push(rowClass(run.state));
        tr.className = keep.join(' ');

        // State label
        var stateCell = tr.querySelector('.run-state');
        if (stateCell) stateCell.textContent = STATE_LABELS[run.state] || run.state;

        // Duration (ticks for active runs)
        var durCell = tr.querySelector('.run-duration');
        if (durCell) durCell.textContent = formatDuration(durationMs(run));

        // Log button class
        var logBtn = tr.querySelector('td:nth-child(8) a.btn');
        if (logBtn) {
            logBtn.classList.remove('btn-danger', 'btn-light');
            logBtn.classList.add(logBtnClass(run.state));
        }

        // Report button: add if missing, update href+class if already present
        var reportCell = tr.querySelector('td:nth-child(7)');
        if (reportCell) {
            var reportUrl = getReportUrl(run);
            var existingReport = reportCell.querySelector('a');
            if (reportUrl && !existingReport) {
                var a = document.createElement('a');
                a.href = reportUrl;
                a.target = '_blank';
                a.className = 'btn ' + (isBeforeReport(reportUrl) ? 'btn-secondary' : 'btn-primary');
                a.setAttribute('role', 'button');
                a.textContent = 'Report';
                reportCell.appendChild(a);
            } else if (reportUrl && existingReport) {
                existingReport.href = reportUrl;
                existingReport.classList.remove('btn-secondary', 'btn-primary');
                existingReport.classList.add(isBeforeReport(reportUrl) ? 'btn-secondary' : 'btn-primary');
            }
        }

        // Log button: add if it just appeared
        var logCell = tr.querySelector('td:nth-child(8)');
        if (logCell && !logCell.querySelector('a')) {
            var logUrl = getLogUrl(run);
            if (logUrl) {
                var la = document.createElement('a');
                la.href = logUrl;
                la.target = '_blank';
                la.className = 'btn ' + logBtnClass(run.state);
                la.setAttribute('role', 'button');
                la.textContent = 'Log';
                logCell.appendChild(la);
            }
        }

        // After-run button and Retry button: add or remove as state changes
        var afterCell = tr.querySelector('td:nth-child(9)');
        if (afterCell) {
            var existingAfter = afterCell.querySelector('.start-after-btn');
            var aUrl     = afterRunUrl(run);
            if (aUrl && !existingAfter) {
                var btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'btn btn-warning start-after-btn';
                btn.setAttribute('data-run-id',    run.id);
                btn.setAttribute('data-run-name',  runName(run) || '');
                btn.setAttribute('data-after-url', aUrl);
                btn.textContent = "Start 'after' run";
                wireAfterBtn(btn);
                afterCell.appendChild(btn);
            } else if (!aUrl && existingAfter) {
                existingAfter.remove();
            }

            var existingRetry = afterCell.querySelector('.retry-after-btn');
            var rUrl = retryAfterUrl(run);
            if (rUrl && !existingRetry) {
                var rbtn = document.createElement('button');
                rbtn.type = 'button';
                rbtn.className = 'btn btn-danger retry-after-btn';
                rbtn.setAttribute('data-run-id',    run.id);
                rbtn.setAttribute('data-run-name',  runName(run) || '');
                rbtn.setAttribute('data-retry-url', rUrl);
                rbtn.textContent = "Retry 'after' run";
                wireRetryBtn(rbtn);
                afterCell.appendChild(rbtn);
            } else if (!rUrl && existingRetry) {
                existingRetry.remove();
            }
        }
    }

    // ── After-run modal wiring ───────────────────────────────────────────────

    var pendingAfterUrl = null;
    var pendingRetryUrl = null;

    function wireAfterBtn(btn) {
        btn.addEventListener('click', function () {
            var runId   = btn.getAttribute('data-run-id');
            var rName   = btn.getAttribute('data-run-name');
            pendingAfterUrl = btn.getAttribute('data-after-url');
            document.getElementById('modal-run-id').textContent = runId;
            var nameWrap = document.getElementById('modal-run-name-wrap');
            if (rName) {
                document.getElementById('modal-run-name').textContent = rName;
                nameWrap.style.display = '';
            } else {
                nameWrap.style.display = 'none';
            }
            new bootstrap.Modal(document.getElementById('afterRunModal')).show();
        });
    }

    function wireRetryBtn(btn) {
        btn.addEventListener('click', function () {
            var runId   = btn.getAttribute('data-run-id');
            var rName   = btn.getAttribute('data-run-name');
            pendingRetryUrl = btn.getAttribute('data-retry-url');
            document.getElementById('retry-modal-run-id').textContent = runId;
            var nameWrap = document.getElementById('retry-modal-run-name-wrap');
            if (rName) {
                document.getElementById('retry-modal-run-name').textContent = rName;
                nameWrap.style.display = '';
            } else {
                nameWrap.style.display = 'none';
            }
            new bootstrap.Modal(document.getElementById('retryAfterModal')).show();
        });
    }

    // ── Polling ──────────────────────────────────────────────────────────────

    var POLL_INTERVAL_MS = 3000;
    var pollTimer        = null;

    function poll() {
        fetch(root._jlineup.runsUrl)
            .then(function(resp) { return resp.json(); })
            .then(function(runs) {
                var tbody      = document.getElementById('runs-tbody');
                var noRunsRow  = tbody && tbody.querySelector('.no-runs-row');
                var anyActive  = false;

                runs.forEach(function(run) {
                    if (ACTIVE_STATES.has(run.state)) anyActive = true;

                    var tr = document.getElementById('run-row-' + run.id);
                    if (tr) {
                        patchRow(tr, run);
                    } else {
                        // Brand-new run — insert in startTime-descending order
                        var newRow = buildRow(run);
                        newRow.querySelectorAll('.start-after-btn').forEach(wireAfterBtn);
                        newRow.querySelectorAll('.retry-after-btn').forEach(wireRetryBtn);
                        var newTs = parseInt(newRow.getAttribute('data-start-time'), 10) || 0;
                        tbody.insertBefore(newRow, findInsertionPoint(tbody, newTs));
                        if (noRunsRow) noRunsRow.style.display = 'none';
                    }
                });

                if (!anyActive) {
                    clearInterval(pollTimer);
                    pollTimer = null;
                }
            })
            .catch(function() { /* network hiccup — keep polling */ });
    }

    // ── Bootstrap init (browser only) ────────────────────────────────────────

    function init() {
        document.querySelectorAll('.start-after-btn').forEach(wireAfterBtn);
        document.querySelectorAll('.retry-after-btn').forEach(wireRetryBtn);

        document.getElementById('modal-confirm-btn').addEventListener('click', function () {
            if (!pendingAfterUrl) return;

            var modalEl = document.getElementById('afterRunModal');
            document.activeElement && document.activeElement.blur();
            bootstrap.Modal.getInstance(modalEl).hide();

            fetch(pendingAfterUrl, { method: 'POST', headers: { 'Content-Type': 'application/json' } })
                .then(function (resp) {
                    var alertEl = document.getElementById('after-run-alert');
                    if (resp.status === 202) {
                        alertEl.className = 'alert alert-success mt-2';
                        alertEl.textContent = 'After run started successfully.';
                        alertEl.classList.remove('d-none');
                        setTimeout(function () { alertEl.classList.add('d-none'); }, 3000);
                    } else {
                        resp.text().then(function (body) {
                            alertEl.className = 'alert alert-danger mt-2';
                            alertEl.textContent = 'Failed to start after run: ' + body;
                            alertEl.classList.remove('d-none');
                        });
                    }
                })
                .catch(function (err) {
                    var alertEl = document.getElementById('after-run-alert');
                    alertEl.className = 'alert alert-danger mt-2';
                    alertEl.textContent = 'Network error: ' + err.message;
                    alertEl.classList.remove('d-none');
                });

            pendingAfterUrl = null;
        });

        document.getElementById('retry-modal-confirm-btn').addEventListener('click', function () {
            if (!pendingRetryUrl) return;

            var modalEl = document.getElementById('retryAfterModal');
            document.activeElement && document.activeElement.blur();
            bootstrap.Modal.getInstance(modalEl).hide();

            fetch(pendingRetryUrl, { method: 'POST', headers: { 'Content-Type': 'application/json' } })
                .then(function (resp) {
                    var alertEl = document.getElementById('after-run-alert');
                    if (resp.status === 202) {
                        alertEl.className = 'alert alert-success mt-2';
                        alertEl.textContent = 'Retry of after run started successfully.';
                        alertEl.classList.remove('d-none');
                        setTimeout(function () { alertEl.classList.add('d-none'); }, 3000);
                        // Restart polling since we now have an active run
                        if (!pollTimer) pollTimer = setInterval(poll, POLL_INTERVAL_MS);
                    } else {
                        resp.text().then(function (body) {
                            alertEl.className = 'alert alert-danger mt-2';
                            alertEl.textContent = 'Failed to retry after run: ' + body;
                            alertEl.classList.remove('d-none');
                        });
                    }
                })
                .catch(function (err) {
                    var alertEl = document.getElementById('after-run-alert');
                    alertEl.className = 'alert alert-danger mt-2';
                    alertEl.textContent = 'Network error: ' + err.message;
                    alertEl.classList.remove('d-none');
                });

            pendingRetryUrl = null;
        });

        // Always start polling; it will stop itself once no active runs remain.
        pollTimer = setInterval(poll, POLL_INTERVAL_MS);
    }

    // ── Export for CommonJS (Jest) or auto-init in browser ───────────────────

    if (typeof module !== 'undefined' && module.exports) {
        module.exports = {
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
        };
    } else {
        // Browser: auto-init when DOM is ready
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', init);
        } else {
            init();
        }
    }

}(typeof window !== 'undefined' ? window : global));
