package com.example.GoCafe.service;

import com.example.GoCafe.client.HttpClient;
import com.example.GoCafe.entity.Menu;
import com.example.GoCafe.entity.MenuVector;
import com.example.GoCafe.repository.MenuVectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MenuVectorService {

    private final HttpClient httpClient;                 // /menu-vec 호출용 (getMenuVectorBytes)
    private final MenuVectorRepository vectorRepository; // menu_vector 저장소

    /**
     * 메뉴 엔티티를 받아 이름으로 벡터 계산 → menu_vector 테이블에 upsert 저장.
     * PK=FK(1:1)이므로 존재하면 덮어쓰고, 없으면 새로 생성.
     */
    @Transactional
    public MenuVector upsertForMenu(Menu menu) {
        if (menu == null || menu.getId() == null) {
            throw new IllegalArgumentException("menu or menu.id must not be null");
        }
        if (menu.getName() == null || menu.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("menu.name must not be blank");
        }

        // 1) FastAPI에서 벡터( float32 LE → byte[] ) 획득
        byte[] embedding = httpClient.getMenuVectorBytes(menu.getName());

        // 2) 기존 행 있으면 로드, 없으면 새로 생성
        MenuVector vec = vectorRepository.findById(menu.getId())
                .orElseGet(() -> {
                    MenuVector nv = new MenuVector();
                    nv.setMenuId(menu.getId()); // @MapsId: menu_id = menu.id
                    nv.setMenu(menu);
                    return nv;
                });

        // 3) 벡터 갱신 후 저장
        vec.setVector(embedding);
        return vectorRepository.save(vec);
    }

    /**
     * 메뉴 ID와 메뉴 이름만으로 저장/갱신해야 할 때 사용하는 경량 메서드.
     * (Menu 엔티티를 이미 가지고 있다면 upsertForMenu(menu) 사용 권장)
     */
    @Transactional
    public MenuVector upsertByIdAndName(Long menuId, Menu menuRef, String menuName) {
        if (menuId == null) throw new IllegalArgumentException("menuId must not be null");
        if (menuName == null || menuName.trim().isEmpty())
            throw new IllegalArgumentException("menuName must not be blank");

        byte[] embedding = httpClient.getMenuVectorBytes(menuName);

        MenuVector vec = vectorRepository.findById(menuId)
                .orElseGet(() -> {
                    MenuVector nv = new MenuVector();
                    nv.setMenuId(menuId);
                    // menuRef가 null이어도 매핑 자체는 가능하지만, @MapsId 1:1 매핑의 일관성을 위해 가능하면 세팅
                    if (menuRef != null) nv.setMenu(menuRef);
                    return nv;
                });

        vec.setVector(embedding);
        return vectorRepository.save(vec);
    }

    /** 필요 시 벡터 삭제 */
    @Transactional
    public void deleteByMenuId(Long menuId) {
        if (menuId == null) return;
        if (vectorRepository.existsById(menuId)) {
            vectorRepository.deleteById(menuId);
        }
    }

    /** 단건 조회 */
    @Transactional(readOnly = true)
    public MenuVector findByMenuId(Long menuId) {
        return vectorRepository.findById(menuId).orElse(null);
    }
}
