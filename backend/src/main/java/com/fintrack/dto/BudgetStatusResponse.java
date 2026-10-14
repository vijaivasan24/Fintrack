package com.fintrack.dto;

import java.math.BigDecimal;

public class BudgetStatusResponse {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private BigDecimal limitAmount;
    private BigDecimal amountSpent;
    private BigDecimal remainingAmount;
    private boolean exceeded;

    public BudgetStatusResponse() {
    }

    public BudgetStatusResponse(Long id, Long categoryId, String categoryName, BigDecimal limitAmount,
                                BigDecimal amountSpent, BigDecimal remainingAmount, boolean exceeded) {
        this.id = id;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.limitAmount = limitAmount;
        this.amountSpent = amountSpent;
        this.remainingAmount = remainingAmount;
        this.exceeded = exceeded;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(BigDecimal limitAmount) {
        this.limitAmount = limitAmount;
    }

    public BigDecimal getAmountSpent() {
        return amountSpent;
    }

    public void setAmountSpent(BigDecimal amountSpent) {
        this.amountSpent = amountSpent;
    }

    public BigDecimal getSpent() {
        return amountSpent;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public BigDecimal getRemaining() {
        return remainingAmount;
    }

    public boolean isExceeded() {
        return exceeded;
    }

    public void setExceeded(boolean exceeded) {
        this.exceeded = exceeded;
    }
}
