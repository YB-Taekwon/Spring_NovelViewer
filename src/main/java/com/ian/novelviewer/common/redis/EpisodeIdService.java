package com.ian.novelviewer.common.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EpisodeIdService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate redisTemplate;

    public Long getNextEpisodeId(Long novelId) {
        String key = RedisKeyUtil.episodeIdKey(novelId);
        return redisTemplate.opsForValue().increment(key);
    }
}
