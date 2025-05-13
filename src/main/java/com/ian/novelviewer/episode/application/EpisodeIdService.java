package com.ian.novelviewer.episode.application;

import com.ian.novelviewer.common.redis.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EpisodeIdService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    public Long getNextEpisodeId(Long novelId) {
        String key = RedisKeyUtil.episodeIdKey(novelId);
        return redisTemplate.opsForValue().increment(key);
    }
}
