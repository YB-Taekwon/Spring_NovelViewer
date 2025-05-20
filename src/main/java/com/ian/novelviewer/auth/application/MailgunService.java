package com.ian.novelviewer.auth.application;

import com.ian.novelviewer.common.exception.CustomException;
import com.ian.novelviewer.common.redis.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.ian.novelviewer.common.exception.ErrorCode.MAILGUN_NETWORK_ERROR;
import static com.ian.novelviewer.common.exception.ErrorCode.MAILGUN_SEND_FAILED;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailgunService {

    @Value("${spring.mailgun.api-key}")
    private String apiKey;

    @Value("${spring.mailgun.domain}")
    private String domain;

    @Value("${spring.mailgun.sender}")
    private String sender;


    public void sendEmail(String recipient, String code) {
        OkHttpClient client = new OkHttpClient();

        RequestBody body = new FormBody.Builder()
                .add("from", sender)
                .add("to", recipient)
                .add("subject", "이메일 인증 코드")
                .add("text", "인증 코드는 다음과 같습니다: " + code)
                .build();

        Request request = new Request.Builder()
                .url("https://api.mailgun.net/v3/" + domain + "/messages")
                .post(body)
                .addHeader(AUTHORIZATION, Credentials.basic("api", apiKey))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("메일 발송 실패: {}", response.body().string());
                throw new CustomException(MAILGUN_SEND_FAILED);
            } else {
                log.debug("메일 발송 성공: {}", recipient);
            }
        } catch (IOException e) {
            throw new CustomException(MAILGUN_NETWORK_ERROR);
        }
    }
}