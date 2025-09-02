package com.example.GoCafe.controller;

import com.example.GoCafe.entity.Cafe;
import com.example.GoCafe.entity.CafeImage;
import com.example.GoCafe.repository.CafeImageRepository;
import com.example.GoCafe.repository.CafeRepository;
import com.example.GoCafe.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cafes")
public class CafeImageController {

    private final CafeRepository cafeRepository;
    private final CafeImageRepository cafeImageRepository;
    private final FileStorageService storage;

    @PostMapping("/{cafeId}/photos")
    @Transactional
    public ResponseEntity<?> upload(@PathVariable Long cafeId,
                                    @RequestParam("files") List<MultipartFile> files,
                                    @RequestParam(value = "setMain", required = false, defaultValue = "false") boolean setMain) throws Exception {

        Cafe cafe = cafeRepository.findById(cafeId).orElseThrow();

        for (MultipartFile f : files) {
            var sf = storage.store(f, "cafes/" + cafeId);
            CafeImage img = new CafeImage();
            img.setCafe(cafe);
            img.setStoredName(sf.storedName());
            img.setOriginalName(sf.originalName());
            img.setUrl(sf.url());
            img.setContentType(sf.contentType());
            img.setSizeBytes(sf.sizeBytes());
            img.setMain(false);
            cafeImageRepository.save(img);

            // 최초 업로드이거나 setMain=true이면 대표사진 반영
            if (cafe.getCafePhoto() == null || setMain) {
                cafe.setCafePhoto(sf.url());
                img.setMain(true);
            }
        }
        return ResponseEntity.ok(cafeImageRepository.findByCafe_CafeId(cafeId));
    }

    @PatchMapping("/photos/{photoId}/main")
    @Transactional
    public ResponseEntity<?> setMain(@PathVariable Long photoId) {
        CafeImage target = cafeImageRepository.findById(photoId).orElseThrow();
        Cafe cafe = target.getCafe();
        cafeImageRepository.findByCafe_CafeId(cafe.getCafeId())
                .forEach(i -> i.setMain(i.getId().equals(photoId)));
        cafe.setCafePhoto(target.getUrl());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{cafeId}/photos")
    public List<CafeImage> list(@PathVariable Long cafeId) {
        return cafeImageRepository.findByCafe_CafeId(cafeId);
    }

    @DeleteMapping("/photos/{photoId}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long photoId) {
        cafeImageRepository.deleteById(photoId);
        return ResponseEntity.noContent().build();
    }
}
