package com.chutneytesting.scenario.infra.raw;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import com.chutneytesting.scenario.domain.gwt.GwtTestCase;
import com.chutneytesting.server.core.domain.scenario.AggregatedRepository;
import com.chutneytesting.server.core.domain.scenario.ScenarioNotFoundException;
import com.chutneytesting.server.core.domain.scenario.TestCase;
import com.chutneytesting.server.core.domain.scenario.TestCaseMetadata;
import com.chutneytesting.server.core.domain.scenario.TestCaseMetadataImpl;
import com.chutneytesting.server.core.domain.security.User;
import com.chutneytesting.tools.Try;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DatabaseTestCaseRepository implements AggregatedRepository<GwtTestCase> {

    private static final ScenarioMetadataRowMapper SCENARIO_INDEX_ROW_MAPPER = new ScenarioMetadataRowMapper();
    private final ScenarioRowMapper scenario_row_mapper;

    private final NamedParameterJdbcTemplate uiNamedParameterJdbcTemplate;
    private final ObjectMapper mapper;

    public DatabaseTestCaseRepository(NamedParameterJdbcTemplate uiNamedParameterJdbcTemplate,
                                      @Qualifier("persistenceObjectMapper") ObjectMapper objectMapper) {

        this.uiNamedParameterJdbcTemplate = uiNamedParameterJdbcTemplate;
        this.mapper = objectMapper;
        this.scenario_row_mapper = new ScenarioRowMapper(mapper);
    }

    @Override
    public String save(GwtTestCase testCase) {
        TestCaseData testCaseData = TestCaseDataMapper.toDto(testCase);
        if (isNewScenario(testCaseData)) {
            return doSave(testCaseData);
        }
        return doUpdate(testCaseData);
    }

    @Override
    public Optional<GwtTestCase> findById(String scenarioId) {
        try {
            TestCaseData testCaseData = uiNamedParameterJdbcTemplate.queryForObject("SELECT * FROM SCENARIO WHERE ID = :id and ACTIVATED = TRUE", buildIdParameterMap(scenarioId), scenario_row_mapper);
            return Optional.ofNullable(testCaseData).map(TestCaseDataMapper::fromDto);
        } catch (IncorrectResultSizeDataAccessException e) {
            return empty();
        }
    }

    @Override
    public Optional<TestCase> findExecutableById(String id) {
        Optional<GwtTestCase> byId = findById(id);
        if (byId.isPresent()) {
            return of(byId.get());
        } else {
            return empty();
        }
    }

    @Override
    public Optional<TestCaseMetadata> findMetadataById(String testCaseId) {
        return findById(testCaseId).map(t -> t.metadata());
    }

    @Override
    public List<TestCaseMetadata> findAll() {
        return uiNamedParameterJdbcTemplate.query("SELECT ID, TITLE, DESCRIPTION, TAGS, CREATION_DATE, USER_ID, UPDATE_DATE, VERSION FROM SCENARIO where ACTIVATED is TRUE", emptyMap(), SCENARIO_INDEX_ROW_MAPPER);
    }

    @Override
    public void removeById(String scenarioId) {
        // TODO - Refactor - Use CampaignRepository up in callstack
        uiNamedParameterJdbcTemplate.update("DELETE FROM CAMPAIGN_EXECUTION_HISTORY WHERE SCENARIO_ID = :id", buildIdParameterMap(scenarioId));
        uiNamedParameterJdbcTemplate.update("DELETE FROM CAMPAIGN_SCENARIOS WHERE SCENARIO_ID = :id", buildIdParameterMap(scenarioId));
        uiNamedParameterJdbcTemplate.update("UPDATE SCENARIO SET ACTIVATED = FALSE WHERE ID = :id", buildIdParameterMap(scenarioId));
    }

    @Override
    public Optional<Integer> lastVersion(String scenarioId) {
        try {
            return of(uiNamedParameterJdbcTemplate.queryForObject("SELECT VERSION FROM SCENARIO WHERE ID = :id", buildIdParameterMap(scenarioId), Integer.class));
        } catch (IncorrectResultSizeDataAccessException e) {
            return empty();
        }
    }

    @Override
    public List<TestCaseMetadata> search(String textFilter) {
        if (!textFilter.isEmpty()) {
            String[] words = StringEscapeUtils.escapeSql(textFilter).split("\\s");
            String sqlSearch = Arrays.stream(words).map(w -> " CONTENT LIKE '%" + w + "%' ").collect(Collectors.joining(" AND "));
            return uiNamedParameterJdbcTemplate.query(
                "SELECT ID, TITLE, DESCRIPTION, TAGS, CREATION_DATE, USER_ID, UPDATE_DATE, VERSION " +
                    "FROM SCENARIO " +
                    "WHERE ACTIVATED is TRUE " +
                    "AND " + sqlSearch, emptyMap(), SCENARIO_INDEX_ROW_MAPPER);
        } else {
            return findAll();
        }
    }

    private boolean isNewScenario(TestCaseData scenario) {
        return scenario.id == null || uiNamedParameterJdbcTemplate.queryForObject("SELECT COUNT(ID) FROM SCENARIO WHERE ID = :id", buildIdParameterMap(scenario.id), int.class) == 0;
    }

    private String doSave(TestCaseData scenario) {
        String nextId = uiNamedParameterJdbcTemplate.queryForObject("SELECT nextval('SCENARIO_SEQ')", emptyMap(), String.class);
        uiNamedParameterJdbcTemplate.update("INSERT INTO SCENARIO(CONTENT_VERSION, ID, TITLE, DESCRIPTION, CONTENT, TAGS, CREATION_DATE, DATASET, ACTIVATED, USER_ID, UPDATE_DATE, VERSION) VALUES (:contentVersion, :id, :title, :description, :content, :tags, :creationDate, :dataSet, TRUE, :author, :creationDate, 1)",
            scenarioQueryParameterMap(nextId, scenario));
        return nextId;
    }

    private String doUpdate(TestCaseData scenario) {
        int update = uiNamedParameterJdbcTemplate.update("UPDATE SCENARIO SET CONTENT_VERSION = :contentVersion, TITLE = :title, DESCRIPTION = :description, CONTENT = :content, TAGS = :tags, CREATION_DATE = :creationDate, DATASET = :dataSet, USER_ID = :author, UPDATE_DATE = CURRENT_TIMESTAMP, VERSION = VERSION+1 WHERE ID = :id AND VERSION = :version",
            scenarioQueryParameterMap(scenario.id, scenario));
        if (update == 0) {
            throw new ScenarioNotFoundException(scenario.id, scenario.version);
        }
        return scenario.id;
    }

    private MapSqlParameterSource scenarioQueryParameterMap(String nextId, TestCaseData scenario) {
        return Try.exec(() ->
            new MapSqlParameterSource()
                .addValue("contentVersion", scenario.contentVersion)
                .addValue("id", nextId)
                .addValue("title", scenario.title)
                .addValue("description", scenario.description)
                .addValue("dataSet", mapper.writeValueAsString(scenario.executionParameters))
                .addValue("content", scenario.rawScenario)
                .addValue("creationDate", Date.from(scenario.creationDate))
                .addValue("tags", TagListMapper.tagsListToString(scenario.tags))
                .addValue("author", User.isAnonymous(scenario.author) ? null : scenario.author)
                .addValue("version", scenario.version)
        ).runtime();
    }

    private static class ScenarioMetadataRowMapper implements RowMapper<TestCaseMetadata> {
        @Override
        public TestCaseMetadata mapRow(ResultSet rs, int rowNum) throws SQLException {
            String id = rs.getString("ID");
            String title = rs.getString("TITLE");
            String description = rs.getString("DESCRIPTION");
            Timestamp creationDate = rs.getTimestamp("CREATION_DATE");
            List<String> tags = TagListMapper.tagsStringToList(rs.getString("TAGS"));
            String author = rs.getString("USER_ID");
            Timestamp updateDate = rs.getTimestamp("UPDATE_DATE");
            Integer version = rs.getInt("VERSION");
            return TestCaseMetadataImpl.builder()
                .withId(id)
                .withTitle(title)
                .withDescription(description)
                .withTags(tags)
                .withCreationDate(creationDate != null ? creationDate.toInstant() : Instant.now().truncatedTo(MILLIS))
                .withAuthor(author)
                .withUpdateDate(updateDate.toInstant())
                .withVersion(version)
                .build();
        }
    }

    private static class ScenarioRowMapper implements RowMapper<TestCaseData> {
        private final ObjectMapper mapper;

        private ScenarioRowMapper(ObjectMapper mapper) {
            this.mapper = mapper;
        }

        @Override
        public TestCaseData mapRow(ResultSet rs, int rowNum) throws SQLException {
            TestCaseData.TestCaseDataBuilder testCaseDataBuilder = TestCaseData.builder()
                .withContentVersion(rs.getString("CONTENT_VERSION"))
                .withId(rs.getString("ID"))
                .withTitle(rs.getString("TITLE"))
                .withDescription(rs.getString("DESCRIPTION"))
                .withTags(TagListMapper.tagsStringToList(rs.getString("TAGS")))
                .withRawScenario(rs.getString("CONTENT"))
                .withAuthor(rs.getString("USER_ID"))
                .withVersion(rs.getInt("VERSION"));

            Try.exec(() -> {
                TypeReference<Map<String, String>> typeRef = new TypeReference<>() {
                };
                String executionParameters = rs.getString("DATASET");
                return testCaseDataBuilder.withExecutionParameters(mapper.readValue(executionParameters != null ? executionParameters : "{}", typeRef));
            }).runtime();

            Timestamp creationDate = rs.getTimestamp("CREATION_DATE");
            testCaseDataBuilder.withCreationDate(creationDate != null ? creationDate.toInstant() : Instant.now().truncatedTo(MILLIS));

            Timestamp updateDate = rs.getTimestamp("UPDATE_DATE");
            testCaseDataBuilder.withUpdateDate(updateDate.toInstant());

            return testCaseDataBuilder.build();
        }
    }

    private ImmutableMap<String, Object> buildIdParameterMap(String scenarioId) {
        return ImmutableMap.<String, Object>builder().put("id", scenarioId).build();
    }
}
