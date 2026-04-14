package de.otto.jlineup.service;

import de.otto.jlineup.web.JLineupRunStatus;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static de.otto.jlineup.web.State.DEAD;
import static java.lang.invoke.MethodHandles.lookup;

@Service
public class RunPersistenceService {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());
    private static final String RUNS_FILENAME = "runs.json";
    private static final String LOCK_FILENAME = "runs.json.lock";

    // Java NIO FileLock is per-JVM (not per-thread), so concurrent threads within the
    // same JVM would get OverlappingFileLockException. This ReentrantLock serializes
    // intra-process access; the FileLock then guards inter-process access.
    private final ReentrantLock inProcessLock = new ReentrantLock();

    private final JLineupWebProperties jLineupWebProperties;
    private final JsonMapper jsonMapper;

    @Autowired
    public RunPersistenceService(JLineupWebProperties jLineupWebProperties, @Lazy JsonMapper jsonMapper) {
        this.jLineupWebProperties = jLineupWebProperties;
        this.jsonMapper = jsonMapper;
    }


    void persistRuns(Map<String, JLineupRunStatus> runs) {

        Comparator<JLineupRunStatus> comparator = Comparator.comparing(JLineupRunStatus::getStartTime);

        inProcessLock.lock();
        try {
            Path runsPath = getRunsFilePath();
            Path lockPath = getLockFilePath();

            try (FileChannel lockChannel = FileChannel.open(lockPath,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock ignored = lockChannel.lock()) {

                // Read existing runs from disk (written by this or another instance)
                // and merge them with the in-memory runs. In-memory entries take precedence
                // because they may reflect more recent state (e.g. a running job).
                Map<String, JLineupRunStatus> mergedRuns = new HashMap<>(readRunsFromFile(runsPath));
                mergedRuns.putAll(runs);

                Map<String, JLineupRunStatus> mapWithLatestEntries = mergedRuns.entrySet().stream()
                        .sorted(Map.Entry.<String, JLineupRunStatus>comparingByValue(comparator).reversed())
                        .limit(jLineupWebProperties.getMaxPersistedRuns())
                        .collect(Collectors.toMap(Map.Entry<String, JLineupRunStatus>::getKey, Map.Entry<String, JLineupRunStatus>::getValue));
                String serializedRuns = jsonMapper.writeValueAsString(mapWithLatestEntries);

                // Write to a temporary file first, then atomically move it into place.
                // This ensures readers never see a half-written runs.json.
                Path tempFile = Files.createTempFile(runsPath.getParent(), "runs", ".tmp");
                try {
                    Files.write(tempFile, serializedRuns.getBytes(StandardCharsets.UTF_8));
                    Files.move(tempFile, runsPath,
                            StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    // Fallback: non-atomic move (still under lock, so safe against concurrent access)
                    Files.move(tempFile, runsPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    // Clean up temp file on failure
                    Files.deleteIfExists(tempFile);
                    throw e;
                }
            }
        } catch (IOException e) {
            LOG.error("Serialization of runs failed. Please file a bug report at https://github.com/otto-de/jlineup/issues", e);
        } finally {
            inProcessLock.unlock();
        }
    }

    Map<String, JLineupRunStatus> readRuns() {
        Path runsPath = getRunsFilePath();
        Path lockPath = getLockFilePath();

        if (Files.exists(runsPath)) {
            inProcessLock.lock();
            try (FileChannel lockChannel = FileChannel.open(lockPath,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock ignored = lockChannel.lock()) {

                return readRunsFromFile(runsPath);

            } catch (JacksonException e) {
                LOG.error("Could not read runs file, it seems to be broken or incompatible. If this problem reappears, delete '{}'.", runsPath, e);
            } catch (IOException e) {
                LOG.error("Could not acquire lock or read runs file.", e);
            } finally {
                inProcessLock.unlock();
            }
        }
        return Collections.emptyMap();
    }

    /**
     * Reads and deserializes runs from the given file path. Does NOT acquire a lock --
     * callers are responsible for holding the file lock before calling this method.
     * Any run in a non-persistable state (i.e. was in-progress when the JVM shut down)
     * is converted to DEAD.
     */
    private Map<String, JLineupRunStatus> readRunsFromFile(Path runsPath) {
        if (!Files.exists(runsPath)) {
            return Collections.emptyMap();
        }
        try {
            TypeReference<Map<String, JLineupRunStatus>> typeRef = new TypeReference<>() {};
            Map<String, JLineupRunStatus> loadedRuns = jsonMapper.readValue(runsPath.toFile(), typeRef);
            return loadedRuns
                    .entrySet()
                    .stream()
                    .map(entry -> {
                        if (entry.getValue().getState().isNonPersistable()) {
                            return new AbstractMap.SimpleEntry<>(entry.getKey(), JLineupRunStatus.copyOfRunStatusBuilder(entry.getValue()).withState(DEAD).build());
                        } else return entry;
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (JacksonException e) {
            LOG.error("Could not read runs file, it seems to be broken or incompatible. If this problem reappears, delete '{}'.", runsPath, e);
            return Collections.emptyMap();
        }
    }

    private Path getRunsFilePath() {
        return Paths.get(jLineupWebProperties.getWorkingDirectory(), RUNS_FILENAME);
    }

    private Path getLockFilePath() {
        return Paths.get(jLineupWebProperties.getWorkingDirectory(), LOCK_FILENAME);
    }

}
