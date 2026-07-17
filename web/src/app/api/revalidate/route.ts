import { timingSafeEqual } from "crypto";
import { revalidatePath, revalidateTag } from "next/cache";
import { NextRequest } from "next/server";

// backend RevalidateWebhookListener의 REVALIDATE_PATHS와 일치시킨다.
// 화이트리스트 밖 경로는 무시해 임의 경로 재검증을 막는다(시크릿이 노출되더라도 방어심화).
// FE-07 전환기: path 기반은 태그로 이전 중 — 1~2주 관찰 후 이 화이트리스트와 함께 제거 예정.
const ALLOWED_PATHS = new Set(["/", "/rounds", "/frequency", "/stats", "/companion"]);
const ALLOWED_PATH_PATTERNS = [/^\/rounds\/\d+$/];

function isAllowedPath(path: string): boolean {
  return ALLOWED_PATHS.has(path) || ALLOWED_PATH_PATTERNS.some((pattern) => pattern.test(path));
}

// backend RevalidateWebhookListener.tagsFor()의 태그 이름과 일치시킨다.
const ALLOWED_TAGS = new Set(["rounds:latest", "rounds:list", "stats:all"]);
const ALLOWED_TAG_PATTERNS = [/^rounds:detail:\d+$/];

function isAllowedTag(tag: string): boolean {
  return ALLOWED_TAGS.has(tag) || ALLOWED_TAG_PATTERNS.some((pattern) => pattern.test(tag));
}

export async function POST(req: NextRequest) {
  const secret = req.headers.get("X-Revalidate-Secret");
  const expected = process.env.KRAFT_REVALIDATE_SECRET;
  if (!secret || !expected) {
    return new Response("Unauthorized", { status: 401 });
  }
  const secretBuf = Buffer.from(secret);
  const expectedBuf = Buffer.from(expected);
  if (secretBuf.length !== expectedBuf.length || !timingSafeEqual(secretBuf, expectedBuf)) {
    return new Response("Unauthorized", { status: 401 });
  }

  let body: { paths?: unknown; tags?: unknown };
  try {
    body = await req.json();
  } catch {
    return new Response("Invalid JSON body", { status: 400 });
  }

  const requestedPaths = Array.isArray(body.paths) ? body.paths : [];
  const paths = requestedPaths.filter(
    (path): path is string => typeof path === "string" && isAllowedPath(path)
  );
  const requestedTags = Array.isArray(body.tags) ? body.tags : [];
  const tags = requestedTags.filter(
    (tag): tag is string => typeof tag === "string" && isAllowedTag(tag)
  );

  for (const tag of tags) {
    // profile 인자는 'use cache' 프로필 캐시에만 의미가 있다. 이 코드베이스는 classic
    // fetch(next:{revalidate,tags}) 패턴만 쓰므로 "무조건 즉시 무효화"를 뜻하는 'max'로 고정한다.
    revalidateTag(tag, "max");
  }
  for (const path of paths) {
    revalidatePath(path);
  }

  return Response.json({ revalidated: true, paths, tags });
}
