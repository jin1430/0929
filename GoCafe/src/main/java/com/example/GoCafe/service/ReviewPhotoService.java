// src/main/java/com/example/GoCafe/service/ReviewPhotoService.java
package com.example.GoCafe.service;

import com.example.GoCafe.entity.Review;
import com.example.GoCafe.entity.ReviewPhoto;
import com.example.GoCafe.repository.ReviewPhotoRepository;
import com.example.GoCafe.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewPhotoService {

    private final ReviewRepository reviewRepository;
    private final ReviewPhotoRepository reviewPhotoRepository;
    private final FileStorageService fileStorageService;

    /** 리뷰의 사진 목록 (sortIndex ASC, id ASC 보조정렬) */
    public List<ReviewPhoto> getPhotos(Long reviewId) {
        return reviewPhotoRepository.findByReview_IdOrderBySortIndexAscIdAsc(reviewId);
    }

    /** 해당 리뷰에서 다음 정렬 시작값(없으면 0) */
    int nextSortStart(Long reviewId) {
        Integer max = reviewPhotoRepository.findMaxSortIndexByReviewId(reviewId);
        return (max == null ? -1 : max) + 1;
    }

    /** URL이 이미 정해진 사진 1장 추가 (정렬 자동) */
    @Transactional
    public ReviewPhoto addPhoto(Long reviewId, String photoUrl) {
        int sort = nextSortStart(reviewId);
        Review review = reviewRepository.findById(reviewId).orElse(null);
        ReviewPhoto reviewPhoto = new ReviewPhoto(null, review, photoUrl, sort);
        return reviewPhotoRepository.save(reviewPhoto);
    }

    /** URL이 이미 정해진 사진 1장 추가 (정렬 지정) */
    @Transactional
    public ReviewPhoto addPhoto(Long reviewId, String photoUrl, int sortOrder) {
        Review review = reviewRepository.findById(reviewId).orElse(null);
        ReviewPhoto reviewPhoto = new ReviewPhoto(null, review, photoUrl, sortOrder);
        return reviewPhotoRepository.save(reviewPhoto);
    }

    /** 멀티파트 파일들을 저장소에 저장하고, URL로 ReviewPhoto 다건 생성 */
    @Transactional
    public List<ReviewPhoto> addPhotos(Long reviewId, List<MultipartFile> files) {
        List<ReviewPhoto> result = new ArrayList<>();
        if (files == null || files.isEmpty()) return result;

        int start = nextSortStart(reviewId);
        int i = 0;
        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) continue;

            // 파일 저장 (ex: "reviews/{reviewId}" 하위)
            String url = fileStorageService.save(f, "reviews/" + reviewId);

            Review review = reviewRepository.findById(reviewId).orElse(null);
            ReviewPhoto rp = new ReviewPhoto(null, review, url, start + i);
            result.add(reviewPhotoRepository.save(rp));
            i++;
        }
        return result;
    }

    /** 사진 1장 삭제 */
    @Transactional
    public void deleteById(Long reviewPhotoId) {
        reviewPhotoRepository.deleteById(reviewPhotoId);
    }

    /** 특정 리뷰의 사진 전부 삭제 */
    @Transactional
    public void deleteAllByReviewId(Long reviewId) {
        reviewPhotoRepository.deleteByReview_Id(reviewId);
    }

    /** 해당 리뷰의 최대 sortIndex (없으면 -1) */
    public int findMaxSortOrderByReviewId(Long reviewId) {
        Integer v = reviewPhotoRepository.findMaxSortIndexByReviewId(reviewId);
        return v == null ? -1 : v;
    }

    @Transactional
    public void save(ReviewPhoto reviewPhoto) {
        reviewPhotoRepository.save(reviewPhoto);
    }

    /** 리뷰 사진 목록 (sortIndex ASC) */
    public List<ReviewPhoto> findByReviewIdOrderBySortIndexAsc(Long id) {
        return reviewPhotoRepository.findByReview_IdOrderBySortIndexAsc(id);
    }

    /* ===========================
       편의 메서드 (컨트롤러 연동용)
       =========================== */

    /** 파일을 저장소에 저장하고 접근 URL만 반환 */
    @Transactional
    public String storeAndGetUrl(Long reviewId, MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        return fileStorageService.save(file, "reviews/" + reviewId);
    }

    /** 이미 저장된 URL을 정렬 지정하여 ReviewPhoto로 붙임 */
    @Transactional
    public ReviewPhoto attachPhoto(Long reviewId, String photoUrl, int sortIndex) {
        if (photoUrl == null || photoUrl.isBlank()) return null;
        return addPhoto(reviewId, photoUrl, sortIndex);
    }

    /** 파일 1개를 저장 후 ReviewPhoto로 즉시 첨부(정렬 자동) */
    @Transactional
    public ReviewPhoto uploadAndAttach(Long reviewId, MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        String url = fileStorageService.save(file, "reviews/" + reviewId);
        return addPhoto(reviewId, url);
    }

    /** 파일 여러 개를 저장 후 ReviewPhoto로 즉시 첨부(정렬 자동) */
    @Transactional
    public List<ReviewPhoto> uploadAndAttachAll(Long reviewId, List<MultipartFile> files) {
        return addPhotos(reviewId, files);
    }
}
