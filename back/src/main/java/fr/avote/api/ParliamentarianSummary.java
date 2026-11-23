package fr.avote.api;

import fr.avote.domain.Chamber;

/** Données affichées dans la grille. */
public record ParliamentarianSummary(
        long id,
        Chamber chamber,
        String civility,
        String firstName,
        String lastName,
        String photoUrl,
        Long groupId,
        String groupCode,
        String groupLabel,
        String groupColor,
        String constituency) {
}
