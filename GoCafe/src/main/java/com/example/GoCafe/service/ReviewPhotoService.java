package com.example.GoCafe.service;

import com.example.GoCafe.entity.ReviewPhoto;
import com.example.GoCafe.repository.ReviewPhotoRepository;
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

    private final ReviewPhotoRepository reviewPhotoRepository;
    private final FileStorageService fileStorageService; // 구현체 주입

    /**
     * 리뷰의 사진 목록 (오름차순)
     */
    public List<ReviewPhoto> getPhotos(Long reviewId) {
        return reviewPhotoRepository
                .findByReviewIdOrderByReviewPhotoSortOrderAscReviewPhotoIdAsc(reviewId);
    }
    /** 다음에 들어갈 정렬 시작값 계산 (없으면 0부터) */
    int nextSortStart(Long reviewId) {
        Integer max = reviewPhotoRepository.findMaxSortOrderByReviewId(reviewId);
        return (max == null ? -1 : max) + 1;
    }

    /** URL이 이미 정해진 사진 1장 추가 (정렬값 자동 할당) */
    @Transactional
    public ReviewPhoto addPhoto(Long reviewId, String photoUrl) {
        int sort = nextSortStart(reviewId);
        ReviewPhoto rp = new ReviewPhoto(null, reviewId, photoUrl, sort);
        return reviewPhotoRepository.save(rp);
    }

    /** URL이 이미 정해진 사진 1장 추가 (정렬값 지정) */
    @Transactional
    public ReviewPhoto addPhoto(Long reviewId, String photoUrl, int sortOrder) {
        ReviewPhoto rp = new ReviewPhoto(null, reviewId, photoUrl, sortOrder);
        return reviewPhotoRepository.save(rp);
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

            ReviewPhoto rp = new ReviewPhoto(null, reviewId, url, start + i);
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

    /** 특정 리뷰의 사진 전부 삭제 (부모 삭제 시 등) */
    @Transactional
    public void deleteAllByReviewId(Long reviewId) {
        reviewPhotoRepository.deleteByReviewId(reviewId);
    }

    @Transactional(readOnly = true)
    public int findMaxSortOrderByReviewId(Long reviewId) {
        Integer v = reviewPhotoRepository.findMaxSortOrderByReviewId(reviewId);
        return v == null ? -1 : v;
    }

    @Transactional
    public void save(ReviewPhoto reviewPhoto) {
        reviewPhotoRepository.save(reviewPhoto);
    }


}
