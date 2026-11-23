package fr.avote.ingestion.europarl;

import java.util.Map;

/** Libellés français et couleurs indicatives des groupes politiques du Parlement européen. */
final class EpGroups {

    record Info(String label, String color) {
    }

    private static final Map<String, Info> BY_CODE = Map.of(
            "PPE", new Info("Parti populaire européen (Démocrates-chrétiens)", "#3399FF"),
            "S&D", new Info("Alliance progressiste des socialistes et démocrates", "#E2001A"),
            "PfE", new Info("Patriotes pour l'Europe", "#5B3A8C"),
            "ECR", new Info("Conservateurs et réformistes européens", "#0A5DA6"),
            "Renew", new Info("Renew Europe", "#F2B705"),
            "Verts/ALE", new Info("Les Verts / Alliance libre européenne", "#4CAF50"),
            "The Left", new Info("La Gauche au Parlement européen (GUE/NGL)", "#A31621"),
            "ESN", new Info("Europe des nations souveraines", "#7A5C3E"),
            "NI", new Info("Non-inscrits", "#8A8A8A"));

    private EpGroups() {
    }

    static Info of(String code) {
        return BY_CODE.getOrDefault(code, new Info(code, null));
    }
}
