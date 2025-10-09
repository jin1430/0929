// src/main/java/com/example/GoCafe/service/FileStorageService.java
package com.example.GoCafe.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

public abstract class FileStorageService {

    @Value("${file.upload-dir}")
    private String root;

    private static final String URL_PREFIX = "/uploads/"; // 우리 서비스가 노출하는 고정 프리픽스

    public StoredFile store(MultipartFile file, String subDir) throws IOException {
        String ext = "";
        String original = file.getOriginalFilename();
        int dot = original != null ? original.lastIndexOf('.') : -1;
        if (dot > -1) ext = original.substring(dot);
        String stored = UUID.randomUUID().toString().replace("-", "") + ext;

        Path dir = Paths.get(root, subDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        Path target = dir.resolve(stored);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String url = URL_PREFIX + subDir.replace('\\', '/').replaceAll("^/+", "") + "/" + stored;
        return new StoredFile(stored, original, url, file.getContentType(), file.getSize());
    }

    public abstract String save(MultipartFile file, String subDir);

    public abstract void delete(String url);

    /** 관리자 다운로드 등에서 사용 */
    public byte[] loadAsBytes(String url) {
        try {
            Path p = resolvePathFromUrl(url);
            return Files.readAllBytes(p);
        } catch (IOException e) {
            throw new RuntimeException("파일 로드 실패: " + url, e);
        }
    }

    /** “/uploads/..” URL을 실제 저장소 경로로 변환 */
    protected Path resolvePathFromUrl(String url) {
        if (url == null || url.isBlank()) throw new IllegalArgumentException("url is blank");
        if (!url.startsWith(URL_PREFIX)) {
            throw new IllegalArgumentException("로컬 스토리지 URL 형식이 아닙니다: " + url);
        }
        String relative = url.substring(URL_PREFIX.length());
        Path base = Paths.get(root).toAbsolutePath().normalize();
        Path path = base.resolve(relative).normalize();
        if (!path.startsWith(base)) {
            throw new SecurityException("비정상 경로 접근 시도: " + url);
        }
        return path;
    }

    protected String getRoot() { return root; }

    public record StoredFile(String storedName, String originalName, String url,
                             String contentType, long sizeBytes) {}
}
