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

    function logButtonClass(state) {
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
        var seconds = Math.floor(ms / 1000);
        var HH = Math.floor(seconds / 3600);
        var MM = Math.floor((seconds % 3600) / 60);
        var SS = seconds % 60;
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

    var RERUNNABLE_STATES = new Set([
        'FINISHED_WITHOUT_DIFFERENCES', 'FINISHED_WITH_DIFFERENCES', 'ERROR', 'DEAD'
    ]);

    function rerunAfterUrl(run) {
        return RERUNNABLE_STATES.has(run.state)
            ? root._jlineup.runsUrl + '/' + run.id + '/rerun-after'
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
            var config = entry[1];
            var paths = (config.paths && config.paths.length) ? config.paths : ['/'];
            paths.forEach(function(path) { result.push(url + path); });
        });
        return result;
    }

    function formatStartTime(iso) {
        if (!iso) return '';
        var date = new Date(iso);
        var pad  = function(n) { return String(n).padStart(2,'0'); };
        return date.getFullYear() + '-' + pad(date.getMonth()+1) + '-' + pad(date.getDate()) +
               ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes());
    }

    function escHtml(s) {
        if (s == null) return '';
        return String(s)
            .replace(/&/g,'&amp;')
            .replace(/</g,'&lt;')
            .replace(/>/g,'&gt;')
            .replace(/"/g,'&quot;');
    }

    function latestTimestampMs(run) {
        return parseMsOrNull(run.endTime)
            || parseMsOrNull(run.resumeTime)
            || parseMsOrNull(run.pauseTime)
            || parseMsOrNull(run.startTime)
            || 0;
    }

    // ── DOM builders ─────────────────────────────────────────────────────────

    // Build the retry/rerun action buttons for the actions cell.
    // When both retry and rerun are available, renders a Bootstrap split-button dropdown.
    // When only one is available, renders a standalone button.
    function buildActionButtons(runId, name, afterUrl, retryUrl, rerunUrl, state) {
        var escapedId   = escHtml(runId);
        var escapedName = escHtml(name || '');
        var items = [];

        if (afterUrl) {
            items.push('<li><a class="dropdown-item start-after-btn" href="#"' +
                ' data-run-id="' + escapedId + '"' +
                ' data-run-name="' + escapedName + '"' +
                ' data-after-url="' + escHtml(afterUrl) + '">Start \'after\' run</a></li>');
        }
        if (retryUrl) {
            items.push('<li><a class="dropdown-item retry-after-btn" href="#"' +
                ' data-run-id="' + escapedId + '"' +
                ' data-run-name="' + escapedName + '"' +
                ' data-retry-url="' + escHtml(retryUrl) + '">Retry \'after\'</a></li>');
        }
        if (rerunUrl) {
            items.push('<li><a class="dropdown-item rerun-after-btn" href="#"' +
                ' data-run-id="' + escapedId + '"' +
                ' data-run-name="' + escapedName + '"' +
                ' data-rerun-url="' + escHtml(rerunUrl) + '">Rerun \'after\' as new</a></li>');
        }

        if (items.length === 0) return '';

        return '<div class="dropdown action-dropdown">' +
            '<button class="btn btn-sm btn-outline-secondary dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">' +
            '<i class="bi bi-three-dots-vertical"></i></button>' +
            '<ul class="dropdown-menu">' + items.join('') + '</ul></div>';
    }

    // Build a full <tr> for a run (used when inserting newly-seen rows during polling)
    function buildRow(run) {
        var row = document.createElement('tr');
        row.id  = 'run-row-' + run.id;
        row.className = rowClass(run.state);
        row.setAttribute('data-sort-time', latestTimestampMs(run));

        var reportUrl = getReportUrl(run);
        var logUrl    = getLogUrl(run);
        var afterUrl  = afterRunUrl(run);
        var retryUrl  = retryAfterUrl(run);
        var rerunUrl  = rerunAfterUrl(run);
        var name      = runName(run);
        var urls      = runUrls(run);

        row.innerHTML =
            '<td><div title="' + escHtml(run.id) + '">' + escHtml(run.id.substring(0, 8)) + '</div></td>' +
            '<td><div>' + (name ? escHtml(name) : '') + '</div></td>' +
            '<td style="max-width:400px;" title="' + escHtml(urls.join('\n')) + '"><pre style="white-space:pre-line; word-break:break-all; overflow:auto; max-height:4em; margin:0;">' + escHtml(urls.join('\n')) + '</pre></td>' +
            '<td class="run-state" style="white-space:nowrap">' + escHtml(STATE_LABELS[run.state] || run.state) + '</td>' +
            '<td class="run-start" style="white-space:nowrap">' + escHtml(formatStartTime(run.startTime)) + '</td>' +
            '<td class="run-duration">' + formatDuration(durationMs(run)) + '</td>' +
            '<td>' + (reportUrl
                ? '<a href="' + escHtml(reportUrl) + '" target="_blank" class="btn btn-sm ' +
                  (isBeforeReport(reportUrl) ? 'btn-secondary' : 'btn-primary') +
                  '" role="button">Report</a>'
                : '') + '</td>' +
            '<td>' + (logUrl
                ? '<a href="' + escHtml(logUrl) + '" target="_blank" class="btn btn-sm ' +
                  logButtonClass(run.state) + '" role="button">Log</a>'
                : '') + '</td>' +
            '<td>' + buildActionButtons(run.id, name, afterUrl, retryUrl, rerunUrl, run.state) + '</td>';
        return row;
    }

    // Find the correct insertion point for a new row so tbody stays sorted
    // by data-start-time descending (newest first).
    // Returns the existing <tr> to insert before, or null (= append at end).
    function findInsertionPoint(tbody, newTimestamp) {
        var rows = Array.from(tbody.querySelectorAll('tr[data-sort-time]'));
        for (var i = 0; i < rows.length; i++) {
            var rowTimestamp = parseInt(rows[i].getAttribute('data-sort-time'), 10) || 0;
            if (newTimestamp > rowTimestamp) return rows[i];
        }
        return null;
    }

    // Patch an existing row in-place (only touch what changed)
    function patchRow(row, run) {
        // Update sort time
        row.setAttribute('data-sort-time', latestTimestampMs(run));

        // Row CSS class
        var keep = Array.from(row.classList).filter(function(c) { return !c.startsWith('table-'); });
        keep.push(rowClass(run.state));
        row.className = keep.join(' ');

        // State label
        var stateCell = row.querySelector('.run-state');
        if (stateCell) stateCell.textContent = STATE_LABELS[run.state] || run.state;

        // Duration (ticks for active runs)
        var durationCell = row.querySelector('.run-duration');
        if (durationCell) durationCell.textContent = formatDuration(durationMs(run));

        // Log button class
        var logButton = row.querySelector('td:nth-child(8) a.btn');
        if (logButton) {
            logButton.classList.remove('btn-danger', 'btn-light');
            logButton.classList.add(logButtonClass(run.state));
        }

        // Report button: add if missing, update href+class if already present
        var reportCell = row.querySelector('td:nth-child(7)');
        if (reportCell) {
            var reportUrl = getReportUrl(run);
            var existingReport = reportCell.querySelector('a');
            if (reportUrl && !existingReport) {
                var reportLink = document.createElement('a');
                reportLink.href = reportUrl;
                reportLink.target = '_blank';
                reportLink.className = 'btn btn-sm ' + (isBeforeReport(reportUrl) ? 'btn-secondary' : 'btn-primary');
                reportLink.setAttribute('role', 'button');
                reportLink.textContent = 'Report';
                reportCell.appendChild(reportLink);
            } else if (reportUrl && existingReport) {
                existingReport.href = reportUrl;
                existingReport.classList.remove('btn-secondary', 'btn-primary');
                existingReport.classList.add(isBeforeReport(reportUrl) ? 'btn-secondary' : 'btn-primary');
            }
        }

        // Log button: add if it just appeared
        var logCell = row.querySelector('td:nth-child(8)');
        if (logCell && !logCell.querySelector('a')) {
            var logUrl = getLogUrl(run);
            if (logUrl) {
                var logLink = document.createElement('a');
                logLink.href = logUrl;
                logLink.target = '_blank';
                logLink.className = 'btn btn-sm ' + logButtonClass(run.state);
                logLink.setAttribute('role', 'button');
                logLink.textContent = 'Log';
                logCell.appendChild(logLink);
            }
        }

        // Action dropdown: rebuild as state changes
        var afterCell = row.querySelector('td:nth-child(9)');
        if (afterCell) {
            // Skip if dropdown is currently open to avoid destroying the menu
            var oldDropdown = afterCell.querySelector('.action-dropdown');
            if (oldDropdown && oldDropdown.querySelector('.dropdown-menu.show')) return;

            // Clear and rebuild
            afterCell.innerHTML = '';
            var afterUrl = afterRunUrl(run);
            var retryUrl = retryAfterUrl(run);
            var rerunUrl = rerunAfterUrl(run);
            var html = buildActionButtons(run.id, runName(run), afterUrl, retryUrl, rerunUrl, run.state);
            if (html) {
                afterCell.innerHTML = html;
                afterCell.querySelectorAll('.start-after-btn').forEach(wireAfterButton);
                afterCell.querySelectorAll('.retry-after-btn').forEach(wireRetryButton);
                afterCell.querySelectorAll('.rerun-after-btn').forEach(wireRerunButton);
            }
        }
    }

    // ── After-run modal wiring ───────────────────────────────────────────────

    var pendingAfterUrl = null;
    var pendingRetryUrl = null;
    var pendingRerunUrl = null;
    var pendingRetryRunId = null;
    var pendingRerunRunId = null;

    function disableActionButtons(runId) {
        var row = document.getElementById('run-row-' + runId);
        if (!row) return;
        var lastCell = row.querySelector('td:last-child');
        if (!lastCell) return;
        lastCell.querySelectorAll('.btn').forEach(function (button) {
            button.disabled = true;
            button.classList.add('disabled');
        });
        // Add spinner to the dropdown toggle
        var toggleBtn = lastCell.querySelector('.dropdown-toggle');
        if (toggleBtn) {
            toggleBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>';
        }
    }

    function wireAfterButton(button) {
        button.addEventListener('click', function (e) {
            e.preventDefault();
            var runId   = button.getAttribute('data-run-id');
            var runName = button.getAttribute('data-run-name');
            pendingAfterUrl = button.getAttribute('data-after-url');
            document.getElementById('modal-run-id').textContent = runId;
            var nameWrap = document.getElementById('modal-run-name-wrap');
            if (runName) {
                document.getElementById('modal-run-name').textContent = runName;
                nameWrap.style.display = '';
            } else {
                nameWrap.style.display = 'none';
            }
            new bootstrap.Modal(document.getElementById('afterRunModal')).show();
        });
    }

    function wireRetryButton(button) {
        button.addEventListener('click', function (e) {
            e.preventDefault();
            var runId   = button.getAttribute('data-run-id');
            var runName = button.getAttribute('data-run-name');
            pendingRetryUrl = button.getAttribute('data-retry-url');
            pendingRetryRunId = runId;
            document.getElementById('retry-modal-run-id').textContent = runId;
            var nameWrap = document.getElementById('retry-modal-run-name-wrap');
            if (runName) {
                document.getElementById('retry-modal-run-name').textContent = runName;
                nameWrap.style.display = '';
            } else {
                nameWrap.style.display = 'none';
            }
            new bootstrap.Modal(document.getElementById('retryAfterModal')).show();
        });
    }

    function wireRerunButton(button) {
        button.addEventListener('click', function (e) {
            e.preventDefault();
            var runId   = button.getAttribute('data-run-id');
            var runName = button.getAttribute('data-run-name');
            pendingRerunUrl = button.getAttribute('data-rerun-url');
            pendingRerunRunId = runId;
            document.getElementById('rerun-modal-run-id').textContent = runId;
            var nameWrap = document.getElementById('rerun-modal-run-name-wrap');
            if (runName) {
                document.getElementById('rerun-modal-run-name').textContent = runName;
                nameWrap.style.display = '';
            } else {
                nameWrap.style.display = 'none';
            }
            new bootstrap.Modal(document.getElementById('rerunAfterModal')).show();
        });
    }

    // ── Polling ──────────────────────────────────────────────────────────────

    var POLL_INTERVAL_MS = 3000;
    var pollTimer        = null;

    function poll() {
        fetch(root._jlineup.runsUrl)
            .then(function(response) { return response.json(); })
            .then(function(runs) {
                var tbody      = document.getElementById('runs-tbody');
                var noRunsRow  = tbody && tbody.querySelector('.no-runs-row');
                var anyActive  = false;

                runs.forEach(function(run) {
                    if (ACTIVE_STATES.has(run.state)) anyActive = true;

                    var existingRow = document.getElementById('run-row-' + run.id);
                    if (existingRow) {
                        patchRow(existingRow, run);
                    } else {
                        // Brand-new run — insert in startTime-descending order
                        var newRow = buildRow(run);
                        newRow.querySelectorAll('.start-after-btn').forEach(wireAfterButton);
                        newRow.querySelectorAll('.retry-after-btn').forEach(wireRetryButton);
                        newRow.querySelectorAll('.rerun-after-btn').forEach(wireRerunButton);
                        var newTimestamp = parseInt(newRow.getAttribute('data-sort-time'), 10) || 0;
                        tbody.insertBefore(newRow, findInsertionPoint(tbody, newTimestamp));
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
        // Convert server-rendered start times to browser local time
        document.querySelectorAll('.run-start[data-epoch]').forEach(function (cell) {
            var epoch = parseInt(cell.getAttribute('data-epoch'), 10);
            if (!isNaN(epoch)) cell.textContent = formatStartTime(new Date(epoch).toISOString());
        });

        document.querySelectorAll('.start-after-btn').forEach(wireAfterButton);
        document.querySelectorAll('.retry-after-btn').forEach(wireRetryButton);
        document.querySelectorAll('.rerun-after-btn').forEach(wireRerunButton);

        document.getElementById('modal-confirm-btn').addEventListener('click', function () {
            if (!pendingAfterUrl) return;

            var modalElement = document.getElementById('afterRunModal');
            document.activeElement && document.activeElement.blur();
            bootstrap.Modal.getInstance(modalElement).hide();

            fetch(pendingAfterUrl, { method: 'POST', headers: { 'Content-Type': 'application/json' } })
                .then(function (response) {
                    var alertElement = document.getElementById('after-run-alert');
                    if (response.status === 202) {
                        alertElement.className = 'alert alert-success mt-2';
                        alertElement.textContent = 'After run started successfully.';
                        alertElement.classList.remove('d-none');
                        setTimeout(function () { alertElement.classList.add('d-none'); }, 3000);
                    } else {
                        response.text().then(function (body) {
                            alertElement.className = 'alert alert-danger mt-2';
                            alertElement.textContent = 'Failed to start after run: ' + body;
                            alertElement.classList.remove('d-none');
                        });
                    }
                })
                .catch(function (error) {
                    var alertElement = document.getElementById('after-run-alert');
                    alertElement.className = 'alert alert-danger mt-2';
                    alertElement.textContent = 'Network error: ' + error.message;
                    alertElement.classList.remove('d-none');
                });

            pendingAfterUrl = null;
        });

        document.getElementById('retry-modal-confirm-btn').addEventListener('click', function () {
            if (!pendingRetryUrl) return;

            var modalElement = document.getElementById('retryAfterModal');
            document.activeElement && document.activeElement.blur();
            bootstrap.Modal.getInstance(modalElement).hide();

            var retryRunId = pendingRetryRunId;
            disableActionButtons(retryRunId);

            fetch(pendingRetryUrl, { method: 'POST', headers: { 'Content-Type': 'application/json' } })
                .then(function (response) {
                    var alertElement = document.getElementById('after-run-alert');
                    if (response.status === 202) {
                        alertElement.className = 'alert alert-success mt-2';
                        alertElement.textContent = 'Retry of after run started successfully.';
                        alertElement.classList.remove('d-none');
                        setTimeout(function () { alertElement.classList.add('d-none'); }, 3000);
                        // Restart polling since we now have an active run
                        if (!pollTimer) pollTimer = setInterval(poll, POLL_INTERVAL_MS);
                    } else {
                        response.text().then(function (body) {
                            alertElement.className = 'alert alert-danger mt-2';
                            alertElement.textContent = 'Failed to retry after run: ' + body;
                            alertElement.classList.remove('d-none');
                        });
                    }
                })
                .catch(function (error) {
                    var alertElement = document.getElementById('after-run-alert');
                    alertElement.className = 'alert alert-danger mt-2';
                    alertElement.textContent = 'Network error: ' + error.message;
                    alertElement.classList.remove('d-none');
                });

            pendingRetryUrl = null;
            pendingRetryRunId = null;
        });

        document.getElementById('rerun-modal-confirm-btn').addEventListener('click', function () {
            if (!pendingRerunUrl) return;

            var modalElement = document.getElementById('rerunAfterModal');
            document.activeElement && document.activeElement.blur();
            bootstrap.Modal.getInstance(modalElement).hide();

            var rerunRunId = pendingRerunRunId;
            disableActionButtons(rerunRunId);

            fetch(pendingRerunUrl, { method: 'POST', headers: { 'Content-Type': 'application/json' } })
                .then(function (response) {
                    var alertElement = document.getElementById('after-run-alert');
                    if (response.status === 202) {
                        response.json().then(function (body) {
                            alertElement.className = 'alert alert-success mt-2';
                            alertElement.textContent = 'Rerun started as new run ' + body.id + '.';
                            alertElement.classList.remove('d-none');
                            setTimeout(function () { alertElement.classList.add('d-none'); }, 5000);
                        });
                        // Restart polling since we now have an active run
                        if (!pollTimer) pollTimer = setInterval(poll, POLL_INTERVAL_MS);
                    } else {
                        response.text().then(function (body) {
                            alertElement.className = 'alert alert-danger mt-2';
                            alertElement.textContent = 'Failed to start rerun: ' + body;
                            alertElement.classList.remove('d-none');
                        });
                    }
                })
                .catch(function (error) {
                    var alertElement = document.getElementById('after-run-alert');
                    alertElement.className = 'alert alert-danger mt-2';
                    alertElement.textContent = 'Network error: ' + error.message;
                    alertElement.classList.remove('d-none');
                });

            pendingRerunUrl = null;
            pendingRerunRunId = null;
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
            logButtonClass,
            parseMsOrNull,
            durationMs,
            formatDuration,
            getReportUrl,
            getLogUrl,
            isBeforeReport,
            afterRunUrl,
            retryAfterUrl,
            rerunAfterUrl,
            runName,
            runUrls,
            formatStartTime,
            escHtml,
            latestTimestampMs,
            buildActionButtons,
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
