package com.ian.novelviewer.common.redis;

public final class RedisKeyUtil {
    private static final String PREFIX_NOVEL = "novel";
    private static final String PREFIX_COMMENT = "comment";
    private static final String PREFIX_USER = "user";
    private static final String PREFIX_EMAIL = "email";

    private static final String SUFFIX_EPISODE = "episodeId";
    private static final String SUFFIX_LIKE = "likes";
    private static final String SUFFIX_BOOKMARK = "bookmark";
    private static final String SUFFIX_VERIFY = "verify";

    private static final String SEPARATOR = ":";


    public static String episodeIdKey(Long novelId) {
        return PREFIX_NOVEL + SEPARATOR + novelId + SEPARATOR + SUFFIX_EPISODE;
    }

    public static String commentLikeKey(Long commentId) {
        return PREFIX_COMMENT + SEPARATOR + commentId + SEPARATOR + SUFFIX_LIKE;
    }

    public static String userBookmarkKey(Long userId) {
        return PREFIX_USER + SEPARATOR + userId + SEPARATOR + SUFFIX_BOOKMARK;
    }

    public static String emailVerifyKey(String email) {
        return PREFIX_EMAIL + SEPARATOR + email + SEPARATOR + SUFFIX_VERIFY;
    }

    private RedisKeyUtil() {
    }
}