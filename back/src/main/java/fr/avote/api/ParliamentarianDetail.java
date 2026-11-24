package fr.avote.api;

import fr.avote.domain.Chamber;
import java.time.LocalDate;

/** Fiche détail d'un parlementaire. */
public record ParliamentarianDetail(
        long id,
        Chamber chamber,
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
        Long groupId,
        String groupCode,
        String groupLabel,
        String groupColor,
        String constituency,
        LocalDate mandateStart) {
}
