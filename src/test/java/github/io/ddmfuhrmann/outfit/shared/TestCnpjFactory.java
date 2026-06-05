package github.io.ddmfuhrmann.outfit.shared;

import java.util.concurrent.atomic.AtomicInteger;

public final class TestCnpjFactory {

    private static final AtomicInteger SEED = new AtomicInteger(100);

    private TestCnpjFactory() {}

    public static String generate() {
        int base = SEED.incrementAndGet();
        int[] d = new int[14];
        String baseStr = String.format("%012d", base);
        for (int i = 0; i < 12; i++) d[i] = baseStr.charAt(i) - '0';

        int[] w1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 12; i++) sum += d[i] * w1[i];
        int r = sum % 11;
        d[12] = r < 2 ? 0 : 11 - r;

        int[] w2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        sum = 0;
        for (int i = 0; i < 13; i++) sum += d[i] * w2[i];
        r = sum % 11;
        d[13] = r < 2 ? 0 : 11 - r;

        StringBuilder sb = new StringBuilder();
        for (int digit : d) sb.append(digit);
        return sb.toString();
    }
}
