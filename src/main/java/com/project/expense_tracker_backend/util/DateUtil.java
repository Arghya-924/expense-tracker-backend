package com.project.expense_tracker_backend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;

public class DateUtil {


    private static final Logger log = LoggerFactory.getLogger(DateUtil.class);

    public static LocalDate[] getFirstAndLastDateOfMonth(String yearMonth) {

        YearMonth currentYearMonth = (yearMonth != null) ? YearMonth.parse(yearMonth)
                : YearMonth.of(LocalDate.now().getYear(), LocalDate.now().getMonth());

        LocalDate firstDateOfMonth = currentYearMonth.atDay(1);
        LocalDate lastDateOfMonth = currentYearMonth.atEndOfMonth();

        return new LocalDate[]{firstDateOfMonth, lastDateOfMonth};
    }

    public static Month getMonth(String yearMonth) {
        YearMonth currentYearMonth = (yearMonth != null) ? YearMonth.parse(yearMonth)
                : YearMonth.of(LocalDate.now().getYear(), LocalDate.now().getMonth());

        return currentYearMonth.getMonth();
    }

    public static YearMonth getYearMonth(LocalDate localDate) {
        return YearMonth.of(localDate.getYear(), localDate.getMonth());
    }

    public static YearMonth getYearMonth(String yearMonth) {

        return (yearMonth != null) ? YearMonth.parse(yearMonth)
                : YearMonth.of(LocalDate.now().getYear(), LocalDate.now().getMonth());
    }

}

