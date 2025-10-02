package com.example.GoCafe.service;

import com.example.GoCafe.dto.MenuForm;
import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.Menu;
import com.example.GoCafe.repository.CafeRepository;
import com.example.GoCafe.repository.MenuRepository;
import com.example.GoCafe.support.EntityIdUtil;
import com.example.GoCafe.support.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;
    // ## 아래 2개 Repository와 Service를 추가로 주입받아야 합니다. ##
    private final CafeRepository cafeRepository;
    private final FileStorageService fileStorageService;

    // ## 'deleteMenu' 메서드 추가 ##
    /**
     * Controller에서 호출할 메뉴 삭제 메서드
     */
    @Transactional
    public void deleteMenu(Long menuId) {
        if (!menuRepository.existsById(menuId)) {
            throw new NotFoundException("Menu not found: " + menuId);
        }
        menuRepository.deleteById(menuId);
    }

    // ## 'addMenu' 메서드 추가 ##
    /**
     * Controller에서 호출할 메뉴 추가 메서드
     */
    @Transactional
    public void addMenu(Long cafeId, MenuForm dto, MultipartFile menuPhoto) throws IOException {
        Cafe cafe = cafeRepository.findById(cafeId)
                .orElseThrow(() -> new NotFoundException("Cafe not found: " + cafeId));

        String photoUrl = dto.getPhotoUrl();

        if (menuPhoto != null && !menuPhoto.isEmpty()) {
            FileStorageService.StoredFile storedFile = fileStorageService.store(menuPhoto, "menus");
            photoUrl = storedFile.url();
        }

        Menu menu = dto.toEntity(cafe, photoUrl);
        menuRepository.save(menu);
    }


    /* ===================================
     * ↓↓ 기존 코드 (유지) ↓↓
     * =================================== */

    @Transactional(readOnly = true)
    public List<Menu> findAll() {
        return menuRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Menu findById(Long id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Menu not found: " + id));
    }

    @Transactional
    public Menu create(Menu entity) {
        EntityIdUtil.setId(entity, null);
        return menuRepository.save(entity);
    }

    @Transactional
    public Menu update(Long id, Menu entity) {
        if (!menuRepository.existsById(id)) {
            throw new NotFoundException("Menu not found: " + id);
        }
        EntityIdUtil.setId(entity, id);
        return menuRepository.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!menuRepository.existsById(id)) {
            throw new NotFoundException("Menu not found: " + id);
        }
        menuRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Menu> findByCafeId(Long cafeId) {
        return menuRepository.findByCafe_Id(cafeId);
    }

    // 이 메서드는 구현되지 않았고 사용되지 않으므로 그대로 두거나 삭제해도 됩니다.
    void add(Long cafeId,
             String menuName,
             Integer menuPrice,
             MultipartFile menuPhoto,
             String photoUrl,
             Boolean isNew,
             Boolean isRecommended,
             Long editorMemberId) {
    }
}