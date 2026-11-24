package fr.avote.ingestion;

import java.time.LocalDate;

/** Représentation normalisée d'un parlementaire, quelle que soit la source. */
public record ImportedParliamentarian(
        String sourceId,
        String civility,
        String firstName,
        String lastName,
        String gender,
        LocalDate birthDate,
        String profession,
        String photoUrl,
        String email,
        String externalUrl,
        String hatvpUrl,
        String groupCode,
        String groupLabel,
        String groupColor,
        String constituency,
        LocalDate mandateStart) {
}
