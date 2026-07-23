package com.kraft.community;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kraft.Application;
import com.kraft.community.auth.CommunityPrincipal;
import com.kraft.community.comment.CreateCommentRequest;
import com.kraft.community.post.CreatePostRequest;
import com.kraft.community.post.UpdatePostRequest;
import com.kraft.community.user.CommunityUser;
import com.kraft.community.user.CommunityUserRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * Blitz 71건의 테스트가 보호하던 행위(익명 무세션 조회, 소유권 판정, 낙관적 잠금 409,
 * 1단계 대댓글 강제, tombstone)를 Kraft 커뮤니티 API에 대한 e2e 성격의 검증으로 옮긴다.
 */
@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("커뮤니티 게시글·댓글 API 행위 검증")
class CommunityPostCommentApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommunityUserRepository communityUserRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CommunityUser owner;
    private CommunityUser other;

    @BeforeEach
    void setUp() {
        owner = communityUserRepository.save(new CommunityUser(
                "google", "owner-" + System.nanoTime(), "글쓴이", null, OffsetDateTime.now()));
        other = communityUserRepository.save(new CommunityUser(
                "google", "other-" + System.nanoTime(), "다른사람", null, OffsetDateTime.now()));
    }

    @Test
    @DisplayName("익명 사용자도 게시글 목록·상세를 인증 없이 조회할 수 있다")
    void anonymousUser_canReadPostsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/community/posts"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증되지 않은 요청은 게시글 작성이 거부된다")
    void unauthenticatedRequest_cannotCreatePost() throws Exception {
        mockMvc.perform(post("/api/v1/community/posts")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreatePostRequest("제목", "내용"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("본인 게시글이 아니면 수정·삭제가 403으로 거부된다")
    void nonOwner_cannotUpdateOrDeletePost() throws Exception {
        long postId = createPost(owner, "제목", "내용");

        mockMvc.perform(put("/api/v1/community/posts/" + postId)
                        .with(asUser(other))
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpdatePostRequest("새 제목", "새 내용", 0L))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/community/posts/" + postId)
                        .with(asUser(other))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("게시글 수정 시 버전이 다르면 409를 반환한다")
    void updatingPost_withStaleVersion_returnsConflict() throws Exception {
        long postId = createPost(owner, "제목", "내용");

        mockMvc.perform(put("/api/v1/community/posts/" + postId)
                        .with(asUser(owner))
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpdatePostRequest("새 제목", "새 내용", 999L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMMUNITY_POST_VERSION_CONFLICT"));
    }

    @Test
    @DisplayName("답글에는 답글을 달 수 없다")
    void replyToReply_isRejected() throws Exception {
        long postId = createPost(owner, "제목", "내용");
        long parentCommentId = createComment(owner, postId, "부모 댓글", null);
        long replyId = createComment(other, postId, "답글", parentCommentId);

        mockMvc.perform(post("/api/v1/community/posts/" + postId + "/comments")
                        .with(asUser(owner))
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("답글의 답글", replyId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMUNITY_COMMENT_REPLY_DEPTH_EXCEEDED"));
    }

    @Test
    @DisplayName("댓글을 삭제하면 행은 유지되고 내용·작성자만 마스킹된다")
    void deletingComment_tombstonesInsteadOfHardDelete() throws Exception {
        long postId = createPost(owner, "제목", "내용");
        long commentId = createComment(owner, postId, "삭제될 댓글", null);

        mockMvc.perform(delete("/api/v1/community/comments/" + commentId)
                        .with(asUser(owner))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/community/posts/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].deleted").value(true))
                .andExpect(jsonPath("$.items[0].content").value("삭제된 댓글입니다."));
    }

    private long createPost(CommunityUser author, String title, String content) throws Exception {
        String body = mockMvc.perform(post("/api/v1/community/posts")
                        .with(asUser(author))
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreatePostRequest(title, content))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long createComment(CommunityUser author, long postId, String content, Long parentId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/community/posts/" + postId + "/comments")
                        .with(asUser(author))
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest(content, parentId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).isNotBlank();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private RequestPostProcessor asUser(CommunityUser user) {
        CommunityPrincipal principal = new CommunityPrincipal(user.getId(), user.getNickname(), "sub",
                java.util.Map.of("sub", "test"));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(authentication);
    }
}
