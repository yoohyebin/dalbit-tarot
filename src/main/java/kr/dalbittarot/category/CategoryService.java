package kr.dalbittarot.category;

import kr.dalbittarot.category.dto.CategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
