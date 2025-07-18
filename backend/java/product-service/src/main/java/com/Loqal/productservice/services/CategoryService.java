package com.Loqal.productservice.services;

import com.Loqal.productservice.entity.Category;
import com.Loqal.productservice.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public Category create(Category category) {
        return categoryRepository.save(category);
    }

    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    public Optional<Category> getById(UUID id) {
        return categoryRepository.findById(id);
    }

    public Category update(UUID id, Category updated) {
        return categoryRepository.findById(id).map(existing -> {
            existing.setCategory_name(updated.getCategory_name());
            existing.setCategory_description(updated.getCategory_description());
            return categoryRepository.save(existing);
        }).orElse(null);
    }

    public void delete(UUID id) {
        categoryRepository.deleteById(id);
    }
}

