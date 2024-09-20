package com.project.expense_tracker_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ExpenseResponseDto {

    private long id;
    private String description;
    private double amount;
    private LocalDate date;
    private String category;
}
