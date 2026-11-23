package fr.avote.ingestion;

import fr.avote.domain.Chamber;
import java.util.List;

/** Récupère et normalise les parlementaires en exercice d'une chambre. */
public interface ChamberImporter {

    Chamber chamber();

    List<ImportedParliamentarian> fetch();
}
