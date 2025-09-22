package com.example.GoCafe.service;

import com.example.GoCafe.entity.Menu;
import com.example.GoCafe.repository.MenuRepository;
import com.example.GoCafe.support.EntityIdUtil;
import com.example.GoCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository repository;

    @Transactional(readOnly = true)
    public List<Menu> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Menu findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Menu not found: " + id));
    }

    @Transactional
    public Menu create(Menu entity) {
        EntityIdUtil.setId(entity, null);
        return repository.save(entity);
    }

    @Transactional
    public Menu update(Long id, Menu entity) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Menu not found: " + id);
        }
        EntityIdUtil.setId(entity, id);
        return repository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Menu not found: " + id);
        }
        repository.deleteById(id);
    }
    @Transactional(readOnly = true)
    public List<Menu> findByCafeId(Long cafeId) {
        return repository.findByCafe_Id(cafeId);
    }

    void add(Long cafeId,
             String menuName,
             Integer menuPrice,
             MultipartFile menuPhoto,   // 파일 업로드(선택)
             String photoUrl,           // URL 직접입력(선택)
             Boolean isNew,
             Boolean isRecommended,
             Long editorMemberId) {

    }

}
