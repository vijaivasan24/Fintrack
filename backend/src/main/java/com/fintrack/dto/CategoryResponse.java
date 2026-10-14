package com.fintrack.dto;

import com.fintrack.entity.CategoryType;

public class CategoryResponse {

    private Long id;
    private String name;
    private CategoryType type;

    public CategoryResponse() {
    }

    public CategoryResponse(Long id, String name, CategoryType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CategoryType getType() {
        return type;
    }

    public void setType(CategoryType type) {
        this.type = type;
    }
}
