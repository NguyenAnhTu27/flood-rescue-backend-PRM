package com.floodrescue.shared.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class CodeGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static String generateRescueRequestCode() {
        String datePart = LocalDateTime.now().format(DATE_FORMAT);
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 9999);
        return String.format("%s%s%04d", "RR", datePart, randomPart);
    }

    public static String generateTeamCode() {
        String datePart = LocalDateTime.now().format(DATE_FORMAT);
        int randomPart = ThreadLocalRandom.current().nextInt(100, 999);
        return String.format("TEAM%s%03d", datePart, randomPart);
    }

    public static String generateInventoryReceiptCode() {
        String datePart = LocalDateTime.now().format(DATE_FORMAT);
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 9999);
        return String.format("IR%s%04d", datePart, randomPart);
    }

    public static String generateInventoryIssueCode() {
        String datePart = LocalDateTime.now().format(DATE_FORMAT);
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 9999);
        return String.format("IS%s%04d", datePart, randomPart);
    }
}