package com.chutneytesting.scenario.infra.raw;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TagListMapperTest {

    @Test
    public void null_string_should_produce_empty_list() throws Exception {
        List<String> actual = TagListMapper.tagsStringToList(null);
        assertThat(actual).isEmpty();
    }

    @Test
    public void null_list_should_produce_empty_string() throws Exception {
        String actual = TagListMapper.tagsListToString(null);
        assertThat(actual).isEqualTo("");
    }

    @Test
    public void string_should_be_split_on_comma_separator() throws Exception {
        List<String> actual = TagListMapper.tagsStringToList("  T1  , T2   ,  T3  ");
        assertThat(actual).containsExactly("T1", "T2", "T3");
    }

    @Test
    public void tags_should_be_joined_with_comma_separator() throws Exception {
        String actual = TagListMapper.tagsListToString(Arrays.asList("  T1", " T2 "));
        assertThat(actual).isEqualTo("T1,T2");
    }
}
