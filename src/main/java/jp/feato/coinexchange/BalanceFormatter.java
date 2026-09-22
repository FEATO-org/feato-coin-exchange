package jp.feato.coinexchange;

import java.math.BigDecimal;

final class BalanceFormatter {
    private BalanceFormatter() {
    }

    static String format(double value) {
        if (!Double.isFinite(value)) {
            return Double.toString(value);
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
