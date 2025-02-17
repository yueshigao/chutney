package com.chutneytesting.component.scenario.api.dto;

import com.chutneytesting.server.core.domain.tools.ui.KeyValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableComposableStepDto.class)
@JsonDeserialize(as = ImmutableComposableStepDto.class)
@Value.Style(jdkOnly = true)
public interface ComposableStepDto {

    Comparator<ComposableStepDto> stepDtoComparator = Comparator
        .comparing(ComposableStepDto::name, String.CASE_INSENSITIVE_ORDER);

    Optional<String> id();

    String name();

    @Value.Default()
    default StrategyDto strategy() { return ImmutableStrategyDto.builder().build(); }

    @Value.Default()
    default StepUsage usage() { return StepUsage.STEP; }

    Optional<String> task();

    @Value.Default()
    default List<ComposableStepDto> steps() { return Collections.emptyList(); }

    @Value.Default()
    @JsonProperty("parameters")
    // default parameters defined when editing the component alone
    default List<KeyValue> defaultParameters() { return Collections.emptyList(); }

    @Value.Default()
    @JsonProperty("computedParameters")
    // override default parameters values when the component is used inside another component
    default List<KeyValue> executionParameters() { return Collections.emptyList(); }

    enum StepUsage { STEP, GIVEN, WHEN, THEN }

    @Value.Default()
    default List<String> tags() { return Collections.emptyList(); }
}
