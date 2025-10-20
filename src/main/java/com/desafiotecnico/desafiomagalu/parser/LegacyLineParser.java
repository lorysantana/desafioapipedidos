package com.desafiotecnico.desafiomagalu.parser;

import com.desafiotecnico.desafiomagalu.exception.BadFileFormatException;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
@Slf4j
public class LegacyLineParser {

    private static final int MIN_LINE_LENGTH = 95;
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyyMMdd");

    public ParsedLine parse(String line) {
        if (line == null) {
            throw new BadFileFormatException("Linha nula recebida");
        }

        if (line.length() < MIN_LINE_LENGTH) {
            throw new BadFileFormatException("Linha inválida (esperado pelo menos " + MIN_LINE_LENGTH + " caracteres)");
        }

        try {
            String userIdRaw   = substringSafe(line, 0, 10);
            String userNameRaw = substringSafe(line, 10, 55);
            String orderIdRaw  = substringSafe(line, 55, 65);
            String prodIdRaw   = substringSafe(line, 65, 75);
            String valueRaw    = substringSafe(line, 75, 87);
            String dateRaw     = substringSafe(line, 87, 95);

            Long userId    = parseLongAllowLeadingZeros(userIdRaw, "userId");
            String userName = userNameRaw.trim();
            Long orderId   = parseLongAllowLeadingZeros(orderIdRaw, "orderId");
            Long productId = parseLongAllowLeadingZeros(prodIdRaw, "productId");
            BigDecimal value = parseMonetaryValue(valueRaw, "value");
            LocalDate date = parseDate(dateRaw, "date");

            return new ParsedLine(userId, userName, orderId, productId, value, date);
        } catch (BadFileFormatException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao parsear linha", e);
            throw new BadFileFormatException("Erro ao parsear linha: " + e.getMessage(), e);
        }
    }

    private static String substringSafe(String s, int start, int end) {
        if (s == null) return "";
        if (s.length() <= start) return "";
        int e = Math.min(end, s.length());
        return s.substring(start, e);
    }

    private static Long parseLongAllowLeadingZeros(String raw, String fieldName) {
        if (raw == null) return 0L;
        String cleaned = raw.trim().replaceFirst("^0+", "");
        if (cleaned.isEmpty()) return 0L;
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException ex) {
            throw new BadFileFormatException("Campo inválido para " + fieldName + ": '" + raw + "'");
        }
    }

    private static BigDecimal parseMonetaryValue(String raw, String fieldName) {
        if (raw == null) return BigDecimal.ZERO;
        String t = raw.trim();
        if (t.isEmpty()) return BigDecimal.ZERO;

        try {
            return new BigDecimal(t);
        } catch (NumberFormatException ignored) { }

        String cleaned = t.replaceFirst("^0+", "");
        if (cleaned.isEmpty()) return BigDecimal.ZERO;

        try {
            long cents = Long.parseLong(cleaned);
            return BigDecimal.valueOf(cents).movePointLeft(2);
        } catch (NumberFormatException ex) {
            throw new BadFileFormatException("Campo inválido para " + fieldName + ": '" + raw + "'");
        }
    }

    private static LocalDate parseDate(String raw, String fieldName) {
        if (raw == null) throw new BadFileFormatException("Campo de data ausente: " + fieldName);
        String t = raw.trim();
        if (t.isEmpty()) throw new BadFileFormatException("Campo de data vazio: " + fieldName);
        try {
            return LocalDate.parse(t, DF);
        } catch (DateTimeParseException ex) {
            throw new BadFileFormatException("Data inválida no campo " + fieldName + ": '" + raw + "'");
        }
    }

    @Value
    public static class ParsedLine {
        Long userId;
        String userName;
        Long orderId;
        Long productId;
        BigDecimal value;
        LocalDate date;
    }
}