package com.ian.novelviewer.novel.application;

import com.ian.novelviewer.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
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

import static com.ian.novelviewer.common.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    private static final Duration PRESIGNED_URL_DURATION = Duration.ofMinutes(30);

    @Value("${spring.cloud.aws.bucket}")
    private String bucket;

    public String upload(MultipartFile file, String folderName) throws IOException {
        try {
            String key = folderName + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            log.info("S3 이미지 업로드 처리 - key: {}", key);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            // presigned url 생성
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(
                    builder -> builder.getObjectRequest(getObjectRequest)
                            .signatureDuration(PRESIGNED_URL_DURATION)
            );

            log.info("이미지 업로드 성공");
            return presignedRequest.url().toString();
        } catch (AwsServiceException | SdkClientException e) {
            log.error("S3 업로드 중 오류 발생: {}", e.getMessage());
            throw new CustomException(S3_UPLOAD_FAILED);
        }
    }

    public String update(
            MultipartFile file, String folderName, String oldKey
    ) throws IOException {
        try {
            String newKey = folderName + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            log.info("S3 이미지 수정 처리 - key: {}", newKey);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(newKey)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            try {
                delete(oldKey);
            } catch (Exception e) {
                log.error("이미지 삭제 실패");
            }

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(newKey)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(
                    builder -> builder.getObjectRequest(getObjectRequest)
                            .signatureDuration(PRESIGNED_URL_DURATION)
            );

            log.info("이미지 수정 성공");
            return presignedRequest.url().toString();
        } catch (AwsServiceException | SdkClientException e) {
            log.error("S3 수정 중 오류 발생: {}", e.getMessage());
            throw new CustomException(S3_UPDATE_FAILED);
        }
    }

    public void delete(String key) throws IOException {
        try {
            log.info("S3 이미지 삭제 처리 - key: {}", key);

            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());

            log.info("이미지 삭제 성공");
        } catch (AwsServiceException | SdkClientException e) {
            log.error("S3 삭제 중 오류 발생: {}", e.getMessage());
            throw new CustomException(S3_DELETE_FAILED);
        }
    }
}