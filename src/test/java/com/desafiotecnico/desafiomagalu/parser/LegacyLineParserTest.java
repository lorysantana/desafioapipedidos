package com.desafiotecnico.desafiomagalu.parser;

import com.desafiotecnico.desafiomagalu.exception.BadFileFormatException;
import com.desafiotecnico.desafiomagalu.parser.LegacyLineParser.ParsedLine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LegacyLineParserTest {

    private final LegacyLineParser parser = new LegacyLineParser();

    private static String buildLine(long userId,
                                    String userName,
                                    long orderId,
                                    long productId,
                                    String valueAsDecimal,
                                    String dateYmd) {
        String userIdRaw = String.format("%010d", userId);
        String userNameFixed = (userName == null ? "" : userName);
        if (userNameFixed.length() > 45) userNameFixed = userNameFixed.substring(0, 45);
        String userNameRaw = String.format("%-45s", userNameFixed);
        String orderIdRaw = String.format("%010d", orderId);
        String productIdRaw = String.format("%010d", productId);
        String valueRaw = String.format("%12s", valueAsDecimal).replace(' ', '0');
        return userIdRaw + userNameRaw + orderIdRaw + productIdRaw + valueRaw + dateYmd;
    }

    @Test
    @DisplayName("should parse a valid legacy line into ParsedLine")
    void shouldParseValidLine() {
        String line = buildLine(1L, "Zarelli", 123L, 111L, "000000512.24", "20211201");

        ParsedLine p = parser.parse(line);

        assertThat(p).isNotNull();
        assertThat(p.getUserId()).isEqualTo(1L);
        assertThat(p.getUserName()).isEqualTo("Zarelli");
        assertThat(p.getOrderId()).isEqualTo(123L);
        assertThat(p.getProductId()).isEqualTo(111L);
        assertThat(p.getValue()).isEqualByComparingTo(new BigDecimal("512.24"));
        assertThat(p.getDate()).isEqualTo(LocalDate.of(2021, 12, 1));
    }

    @Test
    @DisplayName("should throw BadFileFormatException when line is null")
    void shouldThrowWhenLineIsNull() {
        assertThrows(BadFileFormatException.class, () -> parser.parse(null));
    }

    @Test
    @DisplayName("should throw BadFileFormatException when line is shorter than expected")
    void shouldThrowWhenLineTooShort() {
        String shortLine = "too short";
        assertThrows(BadFileFormatException.class, () -> parser.parse(shortLine));
    }

    @Test
    @DisplayName("should parse monetary value formatted with leading zeros and decimal point")
    void shouldParseMonetaryValueWithLeadingZerosAndDecimal() {
        String line = buildLine(2L, "Medeiros", 12345L, 222L, "000000256.24", "20201201");

        ParsedLine p = parser.parse(line);

        assertThat(p.getUserId()).isEqualTo(2L);
        assertThat(p.getUserName()).isEqualTo("Medeiros");
        assertThat(p.getOrderId()).isEqualTo(12345L);
        assertThat(p.getProductId()).isEqualTo(222L);
        assertThat(p.getValue()).isEqualByComparingTo(new BigDecimal("256.24"));
        assertThat(p.getDate()).isEqualTo(LocalDate.of(2020, 12, 1));
    }

    @Test
    @DisplayName("should throw BadFileFormatException for invalid monetary field")
    void shouldThrowForInvalidMonetaryField() {
        String badValue = "00000ABCDEF";
        badValue = String.format("%12s", badValue).replace(' ', '0');
        String line = buildLine(1L, "Zarelli", 123L, 111L, badValue, "20211201");

        assertThrows(BadFileFormatException.class, () -> parser.parse(line));
    }

    @Test
    @DisplayName("should throw BadFileFormatException for invalid date field")
    void shouldThrowForInvalidDate() {
        String line = buildLine(1L, "Zarelli", 123L, 111L, "000000512.24", "2021ABCD"); // invalid date

        assertThrows(BadFileFormatException.class, () -> parser.parse(line));
    }
}