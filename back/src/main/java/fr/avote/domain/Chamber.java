package fr.avote.domain;

/** Assemblée représentative dont on suit les membres. */
public enum Chamber {
    AN("Assemblée nationale"),
    SENAT("Sénat"),
    EP("Parlement européen");

    private final String label;

    Chamber(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
