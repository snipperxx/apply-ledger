package io.github.snipperxx.applyledger.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApplicationSearchTest {

    @Test
    void likeWildcardsAreEscaped() {
        assertThat(ApplicationSearch.escapeLike("100%_c\\d")).isEqualTo("100\\%\\_c\\\\d");
    }

    @Test
    void plainTextIsUnchanged() {
        assertThat(ApplicationSearch.escapeLike("backend engineer")).isEqualTo("backend engineer");
    }
}
