package com.cinesnack.common.support;

import java.util.UUID;

public final class Codes {

    private Codes() {}

    public static String taoMa(String tienTo) {
        // Chừa chỗ cho tiền tố trong cột VARCHAR(30).
        return (
            tienTo +
            UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, Math.min(28, 30 - tienTo.length()))
        );
    }
}
