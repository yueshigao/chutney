package com.chutneytesting.glacio.domain.parser.executable;

import static java.util.Optional.ofNullable;

import com.chutneytesting.environment.api.EnvironmentApi;
import com.chutneytesting.glacio.domain.parser.ExecutableGlacioStepParser;
import com.chutneytesting.glacio.domain.parser.executable.common.EntryStepParser;
import com.chutneytesting.glacio.domain.parser.executable.common.FilteredByKeywordsSubStepMapStepParser;
import com.chutneytesting.glacio.domain.parser.executable.common.TargetStepParser;
import com.chutneytesting.task.domain.TaskTemplateRegistry;
import com.github.fridujo.glacio.model.Step;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultGlacioParser extends ExecutableGlacioStepParser {

    private final static Pattern STEP_TEXT_PATTERN = Pattern.compile("^(?<task>[a-zA-Z\\-_0-9]*)?\\s*(?<text>.*)$");

    private final TaskTemplateRegistry taskTemplateRegistry;

    public DefaultGlacioParser(TaskTemplateRegistry taskTemplateRegistry, EnvironmentApi environmentApplication) {
        super(new TargetStepParser(environmentApplication, "On"),
            new FilteredByKeywordsSubStepMapStepParser(new EntryStepParser(), "With"),
            new FilteredByKeywordsSubStepMapStepParser(new EntryStepParser(), "Take", "Keep"),
            new FilteredByKeywordsSubStepMapStepParser(new EntryStepParser(), "Validate"));
        this.taskTemplateRegistry = taskTemplateRegistry;
    }

    @Override
    public String parseTaskType(Step step) {
        Matcher matcher = STEP_TEXT_PATTERN.matcher(step.getText());
        if (matcher.matches()) {
            return ofNullable(matcher.group("task"))
                .filter(taskId -> this.taskTemplateRegistry.getByIdentifier(taskId).isPresent())
                .orElseGet(() ->
                    ofNullable(matcher.group("text"))
                        .filter(taskId -> this.taskTemplateRegistry.getByIdentifier(taskId).isPresent())
                        .orElseThrow(() -> new IllegalArgumentException("Cannot identify task from step text : " + step.getText())));
        }
        throw new IllegalArgumentException("Cannot extract task type from step text : " + step.getText());
    }

    @Override
    public Map<Locale, Set<String>> keywords() {
        throw new UnsupportedOperationException();
    }
}
