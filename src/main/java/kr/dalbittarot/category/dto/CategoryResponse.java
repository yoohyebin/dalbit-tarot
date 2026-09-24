package kr.dalbittarot.category.dto;

import kr.dalbittarot.category.Category;

public record CategoryResponse(String code, String label, String description) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getCode(), category.getLabel(), category.getDescription());
    }
}
