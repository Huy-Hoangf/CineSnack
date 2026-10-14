package com.cinesnack.support;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CodesTest {
    @Test
    void maDonVeThanhToanVaHoaDonVuaCotSql() {
        for (String prefix : new String[] {"DH", "VE", "TM", "HDV", "HDB"}) {
            String code = Codes.taoMa(prefix);
            assertTrue(code.startsWith(prefix));
            assertTrue(code.length() <= 30, code);
            assertNotEquals(code, Codes.taoMa(prefix));
        }
    }
}
