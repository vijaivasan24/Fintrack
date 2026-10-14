package com.fintrack.exception;

public class BudgetNotFoundException extends RuntimeException {

    public BudgetNotFoundException(String message) {
        super(message);
    }
}
