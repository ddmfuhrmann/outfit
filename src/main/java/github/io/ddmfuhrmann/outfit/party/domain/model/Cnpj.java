package github.io.ddmfuhrmann.outfit.party.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Cnpj {

    @Column(name = "cnpj", length = 14)
    private String value;

    protected Cnpj() {}

    private Cnpj(String value) {
        this.value = value;
    }

    public static Cnpj of(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("cnpj is required");
        String digits = raw.replaceAll("\\D", "");
        validate(digits);
        return new Cnpj(digits);
    }

    public String value() {
        return value;
    }

    private static void validate(String digits) {
        if (digits.length() != 14) throw new IllegalArgumentException("cnpj must have 14 digits");
        if (digits.chars().distinct().count() == 1) throw new IllegalArgumentException("cnpj is invalid");

        int[] w1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 12; i++) sum += (digits.charAt(i) - '0') * w1[i];
        int r = sum % 11;
        int d1 = r < 2 ? 0 : 11 - r;
        if (d1 != digits.charAt(12) - '0') throw new IllegalArgumentException("cnpj is invalid");

        int[] w2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        sum = 0;
        for (int i = 0; i < 13; i++) sum += (digits.charAt(i) - '0') * w2[i];
        r = sum % 11;
        int d2 = r < 2 ? 0 : 11 - r;
        if (d2 != digits.charAt(13) - '0') throw new IllegalArgumentException("cnpj is invalid");
    }
}
