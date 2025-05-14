package com.ian.novelviewer.common.redis;

public final class RedisKeyUtil {
    private static final String PREFIX_NOVEL = "novel";
    private static final String PREFIX_COMMENT = "comment";

    private static final String SUFFIX_EPISODE = "episodeId";
    private static final String SUFFIX_LIKE = "likes";

    private static final String SEPARATOR = ":";


    public static String episodeIdKey(Long novelId) {
        return PREFIX_NOVEL + SEPARATOR + novelId + SEPARATOR + SUFFIX_EPISODE;
    }

    public static String commentLikeKey(Long commentId) {
        return PREFIX_COMMENT + SEPARATOR + commentId + SEPARATOR + SUFFIX_LIKE;
    }

    private RedisKeyUtil() {
    }
}