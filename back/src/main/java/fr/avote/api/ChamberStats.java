package fr.avote.api;

import fr.avote.domain.Chamber;

public record ChamberStats(Chamber chamber, String label, long memberCount) {
}
