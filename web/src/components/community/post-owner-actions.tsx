"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { getCommunitySession, deletePost } from "@/lib/community-client";

// 소유권 판정은 서버 응답에 canEdit 같은 파생 필드로 섞지 않고, 클라이언트가
// 세션 엔드포인트의 로그인 사용자 ID와 게시글 ownerId를 직접 대조한다(§4.4).
export function PostOwnerActions({
  postId,
  ownerId,
  version,
}: {
  postId: number;
  ownerId: number;
  version: number;
}) {
  const [isOwner, setIsOwner] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();

  useEffect(() => {
    let cancelled = false;
    getCommunitySession()
      .then((session) => {
        if (!cancelled) setIsOwner(session.loggedIn && session.userId === ownerId);
      })
      .catch(() => {
        if (!cancelled) setIsOwner(false);
      });
    return () => {
      cancelled = true;
    };
  }, [ownerId]);

  if (!isOwner) {
    return null;
  }

  return (
    <div className="community-post-owner-actions">
      {error && <p role="alert">{error}</p>}
      <Link href={`/community/posts/${postId}/edit`}>수정</Link>
      <button
        type="button"
        disabled={deleting}
        onClick={async () => {
          if (!window.confirm("게시글을 삭제할까요?")) return;
          setDeleting(true);
          setError(null);
          try {
            await deletePost(postId, version);
            router.push("/community");
          } catch {
            setError("다른 곳에서 먼저 수정·삭제되었습니다. 새로고침 후 다시 시도하세요.");
          } finally {
            setDeleting(false);
          }
        }}
      >
        {deleting ? "삭제 중…" : "삭제"}
      </button>
    </div>
  );
}
