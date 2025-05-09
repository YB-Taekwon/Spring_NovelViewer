package com.ian.novelviewer.common.redis;

public class RedisKeyUtil {
    private static final String PREFIX_NOVEL = "novel:";

    public static String episodeIdKey(Long novelId) {
        return PREFIX_NOVEL + "_" + novelId + "_episodeId";
    }

    private RedisKeyUtil() {
    }
}
