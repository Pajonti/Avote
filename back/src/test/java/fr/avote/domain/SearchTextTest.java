package fr.avote.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SearchTextTest {

    @Test
    void stripsAccentsCaseAndExtraSpaces() {
        assertThat(SearchText.normalize("  Hélène   DE-la Rüe ")).isEqualTo("helene de-la rue");
    }

    @Test
    void handlesNull() {
        assertThat(SearchText.normalize(null)).isEmpty();
    }

    @Test
    void normalizesTypographicApostrophe() {
        assertThat(SearchText.normalize("D’Ornano")).isEqualTo("d'ornano");
    }
}
