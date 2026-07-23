"use client";

import { useEffect, useRef, useState } from "react";
import {
  createComment,
  deleteComment,
  fetchCommunityComments,
  getCommunitySession,
  type CommunitySession,
} from "@/lib/community-client";
import type { CommunityComment } from "@/lib/community-api";

export function CommentSection({ postId }: { postId: number }) {
  const [comments, setComments] = useState<CommunityComment[]>([]);
  const [session, setSession] = useState<CommunitySession | null>(null);
  const [content, setContent] = useState("");
  const [replyTo, setReplyTo] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // 목록 재조회 race 방지 — 오래된 응답이 최신 상태를 덮어쓰지 않도록 요청 시퀀스를 비교한다.
  const fetchSeqRef = useRef(0);

  const loadComments = () => {
    const seq = ++fetchSeqRef.current;
    fetchCommunityComments(postId)
      .then((result) => {
        if (seq === fetchSeqRef.current) setComments(result.items);
      })
      .catch(() => {
        if (seq === fetchSeqRef.current) setError("댓글을 불러오지 못했습니다.");
      });
  };

  useEffect(() => {
    loadComments();
    getCommunitySession()
      .then(setSession)
      .catch(() => setSession({ loggedIn: false, userId: null, nickname: null }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [postId]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (submitting || !content.trim()) return;
    setSubmitting(true);
    setError(null);
    try {
      await createComment(postId, content.trim(), replyTo);
      setContent("");
      setReplyTo(null);
      loadComments();
    } catch {
      setError("댓글 작성에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (commentId: number) => {
    if (!window.confirm("댓글을 삭제할까요?")) return;
    try {
      await deleteComment(commentId);
      loadComments();
    } catch {
      setError("댓글 삭제에 실패했습니다.");
    }
  };

  return (
    <section className="community-comment-section" aria-label="댓글">
      <h2>댓글 {comments.length}개</h2>
      {error && <p role="alert">{error}</p>}

      <ul className="community-comment-list">
        {comments.map((comment) => (
          <li
            key={comment.id}
            className={`community-comment${comment.parentId ? " is-reply" : ""}`}
          >
            <span className="community-comment-author">{comment.authorNickname}</span>
            <p>{comment.content}</p>
            {!comment.deleted && session?.loggedIn && (
              <div className="community-comment-actions">
                {!comment.parentId && (
                  <button type="button" onClick={() => setReplyTo(comment.id)}>
                    답글
                  </button>
                )}
                {session.userId === comment.ownerId && (
                  <button type="button" onClick={() => handleDelete(comment.id)}>
                    삭제
                  </button>
                )}
              </div>
            )}
          </li>
        ))}
      </ul>

      {session?.loggedIn ? (
        <form onSubmit={handleSubmit} className="community-comment-form">
          {replyTo !== null && (
            <p className="community-comment-reply-target">
              답글 작성 중
              <button type="button" onClick={() => setReplyTo(null)}>
                취소
              </button>
            </p>
          )}
          <label htmlFor="comment-content">댓글 작성</label>
          <textarea
            id="comment-content"
            value={content}
            maxLength={1000}
            onChange={(event) => setContent(event.target.value)}
            required
          />
          <button type="submit" disabled={submitting || !content.trim()}>
            {submitting ? "등록 중…" : "등록"}
          </button>
        </form>
      ) : (
        <p>댓글을 작성하려면 로그인이 필요합니다.</p>
      )}
    </section>
  );
}
