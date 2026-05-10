package github.io.ddmfuhrmann.outfit.party.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Cpf {

    @Column(name = "cpf", length = 11)
    private String value;

    protected Cpf() {}

    private Cpf(String value) {
        this.value = value;
    }

    public static Cpf of(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("cpf is required");
        String digits = raw.replaceAll("\\D", "");
        validate(digits);
        return new Cpf(digits);
    }

    public String value() {
        return value;
    }

    private static void validate(String digits) {
        if (digits.length() != 11) throw new IllegalArgumentException("cpf must have 11 digits");
        if (digits.chars().distinct().count() == 1) throw new IllegalArgumentException("cpf is invalid");

        int sum = 0;
        for (int i = 0; i < 9; i++) sum += (digits.charAt(i) - '0') * (10 - i);
        int r = sum % 11;
        int d1 = r < 2 ? 0 : 11 - r;
        if (d1 != digits.charAt(9) - '0') throw new IllegalArgumentException("cpf is invalid");

        sum = 0;
        for (int i = 0; i < 10; i++) sum += (digits.charAt(i) - '0') * (11 - i);
        r = sum % 11;
        int d2 = r < 2 ? 0 : 11 - r;
        if (d2 != digits.charAt(10) - '0') throw new IllegalArgumentException("cpf is invalid");
    }
}
