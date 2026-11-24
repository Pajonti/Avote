package fr.avote.api;

import fr.avote.domain.Chamber;

public record GroupDto(long id, Chamber chamber, String code, String label, String color, long memberCount) {
}
