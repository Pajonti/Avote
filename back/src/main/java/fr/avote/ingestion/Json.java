package fr.avote.ingestion;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import tools.jackson.databind.JsonNode;

/** Accès tolérants aux documents JSON hétérogènes des sources : absent ou null donne null. */
public final class Json {

    private Json() {
    }

    public static String text(JsonNode node, String... path) {
        JsonNode current = node;
        for (String key : path) {
            current = current.path(key);
        }
        if (current.isMissingNode() || current.isNull() || !current.isValueNode()) {
            return null;
        }
        String value = current.asString().strip();
        return value.isEmpty() ? null : value;
    }

    /** Un nœud qui peut être un objet seul ou un tableau d'objets (cas fréquent des exports XML→JSON). */
    public static Iterable<JsonNode> items(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return java.util.List.of();
        }
        return node.isArray() ? node : java.util.List.of(node);
    }

    public static LocalDate date(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value.substring(0, Math.min(10, value.length())).replace('/', '-'));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
