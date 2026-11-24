package fr.avote.domain;

import java.text.Normalizer;
import java.util.Locale;

/** Normalisation pour la recherche : minuscules, sans accents, espaces compactés. */
public final class SearchText {

    private SearchText() {
    }

    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('’', '\'')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
