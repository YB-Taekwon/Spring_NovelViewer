package com.ian.novelviewer.user.application;

import com.ian.novelviewer.comment.domain.Comment;
import com.ian.novelviewer.comment.domain.CommentRepository;
import com.ian.novelviewer.common.exception.CustomException;
import com.ian.novelviewer.common.redis.RedisKeyUtil;
import com.ian.novelviewer.novel.domain.Novel;
import com.ian.novelviewer.novel.domain.NovelRepository;
import com.ian.novelviewer.user.domain.User;
import com.ian.novelviewer.user.domain.UserRepository;
import com.ian.novelviewer.user.dto.UserDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static com.ian.novelviewer.common.exception.ErrorCode.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final NovelRepository novelRepository;
    private final CommentRepository commentRepository;
    private final RedisTemplate<String, String> redisTemplate;


    /**
     * 주어진 로그인 ID를 기준으로 사용자의 프로필 정보를 조회합니다.
     *
     * @param loginId 사용자 로그인 ID
     * @return 사용자 프로필 정보 응답 DTO
     */
    public UserDto.UserInfoResponse getProfile(String loginId) {
        log.debug("프로필 조회 요청 수신 - loginId: {}", loginId);

        User user = findUserOrThrow(loginId);

        log.debug("프로필 조회 성공 - userId: {}, loginId: {}", user.getId(), user.getLoginId());
        return UserDto.UserInfoResponse.from(user);
    }


    /**
     * 주어진 사용자 ID를 기준으로 북마크한 소설 목록을 조회합니다.
     * Redis에서 북마크 정보를 가져오고, 관련 소설을 조회하여 응답합니다.
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보
     * @return 북마크한 소설 목록 응답 DTO의 Page 객체
     */
    public Page<UserDto.BookmarksResponse> getBookmarks(Long userId, Pageable pageable) {
        log.debug("북마크 조회 요청 - userId: {}", userId);

        String key = RedisKeyUtil.userBookmarkKey(userId);
        Set<String> novelIdSet = redisTemplate.opsForSet().members(key);

        if (novelIdSet == null || novelIdSet.isEmpty()) {
            log.debug("북마크 데이터 없음 - userId: {}", userId);
            return Page.empty(pageable);
        }

        List<Long> novelIds = novelIdSet.stream().map(Long::valueOf).toList();
        log.debug("북마크된 소설 ID 목록 - userId: {}, novelIds: {}", userId, novelIds);

        Page<Novel> novels = novelRepository.findAllByNovelIdIn(novelIds, pageable);

        log.debug("북마크된 소설 조회 성공 - userId: {}, 조회된 개수: {}", userId, novels.getTotalElements());
        return novels.map(UserDto.BookmarksResponse::from);
    }


    /**
     * 주어진 로그인 ID를 가진 사용자가 작성한 댓글 목록을 조회합니다.
     * 작성일을 최신순으로 페이징하여 조회합니다.
     *
     * @param loginId  사용자 로그인 ID
     * @param pageable 페이징 정보
     * @return 댓글 목록 응답 DTO의 Page 객체
     */
    public Page<UserDto.CommentsResponse> getComments(String loginId, Pageable pageable) {
        log.debug("댓글 목록 조회 요청 - loginId: {}, page: {}, size: {}",
                loginId, pageable.getPageNumber(), pageable.getPageSize());

        Page<Comment> comments = commentRepository.findByUser_LoginIdOrderByCreatedAtDesc(loginId, pageable);

        log.debug("댓글 조회 성공 - loginId: {}, 총 댓글 수: {}", loginId, comments.getTotalElements());
        return comments.map(UserDto.CommentsResponse::from);
    }


    /**
     * 사용자가 작가 권한을 요청합니다.
     *
     * @param request 작가 권한 요청 DTO
     * @param loginId 요청자 로그인 ID
     * @return 작가 권한 요청 결과 응답 DTO
     */
    @Transactional
    public UserDto.RoleRequestResponse requestRole(UserDto.RoleRequestRequest request, String loginId) {
        log.debug("작가 권한 요청 수신 - 요청자 로그인 ID: {}, 요청 필명: {}", loginId, request.getAuthorName());

        User user = findUserOrThrow(loginId);

        log.debug("사용자 조회 성공 - 사용자 ID: {}, 현재 권한 목록: {}", user.getLoginId(), user.getRoles());

        user.requestAuthorRole(request.getAuthorName());

        log.debug("작가 권한 요청 완료 - 필명 요청: {}, 요청 대기 상태: {}",
                user.getRequestAuthorName(), user.isRoleRequestPending());

        return UserDto.RoleRequestResponse.from(user);
    }


    /**
     * 주어진 로그인 ID로 사용자를 조회합니다.
     * 없으면 예외를 발생합니다.
     *
     * @param loginId 사용자 로그인 ID
     * @return 조회된 사용자 엔티티
     * @throws CustomException USER_NOT_FOUND 예외
     */
    private User findUserOrThrow(String loginId) {
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 사용자: {}", loginId);
                    return new CustomException(USER_NOT_FOUND);
                });
    }
}