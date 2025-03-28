package de.otto.jlineup.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.otto.jlineup.web.JLineupRunStatus;
import de.otto.jlineup.web.configuration.JLineupWebProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import static de.otto.jlineup.web.State.DEAD;
import static java.lang.invoke.MethodHandles.lookup;

@Service
public class RunPersistenceService {

    private final static Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());
    private static final String RUNS_FILENAME = "runs.json";

    private final JLineupWebProperties jLineupWebProperties;
    private final ObjectMapper objectMapper;

    @Autowired
    public RunPersistenceService(JLineupWebProperties jLineupWebProperties, ObjectMapper objectMapper) {
        this.jLineupWebProperties = jLineupWebProperties;
        this.objectMapper = objectMapper;
    }


    void persistRuns(Map<String, JLineupRunStatus> runs) {

        Comparator<JLineupRunStatus> comparator = Comparator.comparing(JLineupRunStatus::getStartTime);

        try {
            Map<String, JLineupRunStatus> mapWithLatestEntries = runs.entrySet().stream()
                    .sorted(Map.Entry.<String, JLineupRunStatus>comparingByValue(comparator).reversed())
                    .limit(jLineupWebProperties.getMaxPersistedRuns())
                    .collect(Collectors.toMap(Map.Entry<String, JLineupRunStatus>::getKey, Map.Entry<String, JLineupRunStatus>::getValue));
            String serializedRuns = objectMapper.writeValueAsString(mapWithLatestEntries);
            Path path = getRunsFilePath();
            Files.write(path, serializedRuns.getBytes());
        } catch (IOException e) {
            LOG.error("Serialization of runs failed. Please file a bug report at https://github.com/otto-de/jlineup/issues", e);
        }
    }

    Map<String, JLineupRunStatus> readRuns() {
        if (Files.exists(getRunsFilePath())) {
            try {
                TypeReference<Map<String, JLineupRunStatus>> typeRef = new TypeReference<Map<String, JLineupRunStatus>>() {
                };
                Map<String, JLineupRunStatus> loadedRuns = objectMapper.readValue(getRunsFilePath().toFile(), typeRef);
                return loadedRuns
                        .entrySet()
                        .stream()
                        .map(entry -> {
                            if (entry.getValue().getState().isNonPersistable()) {
                                return new AbstractMap.SimpleEntry<>(entry.getKey(), JLineupRunStatus.copyOfRunStatusBuilder(entry.getValue()).withState(DEAD).build());
                            } else return entry;
                        })
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            } catch (IOException e) {
                LOG.error("Could not read runs file, it seems to be broken or incompatible. If this problem reappears, delete '{}'.", getRunsFilePath().toString(), e);
            }
        }
        return Collections.emptyMap();
    }

    private Path getRunsFilePath() {
        return Paths.get(jLineupWebProperties.getWorkingDirectory(), RUNS_FILENAME);
    }


}
