package com.ian.novelviewer.novel.application;

import com.ian.novelviewer.novel.dto.NovelDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    private static final Duration PRESIGNED_URL_DURATION = Duration.ofMinutes(30);

    @Value("${spring.cloud.aws.bucket}")
    private String bucket;

    public NovelDto.ThumbnailResponse upload(MultipartFile file, String folderName) throws IOException {
        log.info("이미지 업로드 요청 처리 - 파일명: {}, 폴더명: {}", file.getOriginalFilename(), folderName);

        String fileName = folderName + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        log.info("file name: {}", fileName);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(
                builder -> builder.getObjectRequest(getObjectRequest)
                        .signatureDuration(PRESIGNED_URL_DURATION)
        );

        NovelDto.ThumbnailResponse thumbnailKey = NovelDto.ThumbnailResponse.builder()
                .thumbnailKey(presignedRequest.url().toString())
                .build();

        log.info("S3 업로드 성공: {}", thumbnailKey);
        return thumbnailKey;
    }

    public void delete(String key) throws IOException {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }
}