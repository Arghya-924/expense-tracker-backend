package com.project.expense_tracker_backend.util;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DateUtilTest {

    private static final Logger log = LoggerFactory.getLogger(DateUtilTest.class);

    @Test
    void testGetFirstAndLastDateOfMonth() {

        String mockYearMonth = "2024-02";

        LocalDate[] firstAndLastDateOfMonth = DateUtil.getFirstAndLastDateOfMonth(mockYearMonth);

        LocalDate firstDate = LocalDate.parse("2024-02-01");
        LocalDate lastDate = LocalDate.parse("2024-02-29");

        assertEquals(firstDate, firstAndLastDateOfMonth[0]);
        assertEquals(lastDate, firstAndLastDateOfMonth[1]);

    }

    @Test
    void testGetFirstAndLastDateOfMonth_Wrong_Format() {

        String mockYearMonth = "dhjfakjdfhr";

        assertThrows(DateTimeParseException.class, () -> DateUtil.getFirstAndLastDateOfMonth(mockYearMonth));
    }
}
