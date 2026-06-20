import { timingSafeEqual } from "crypto";
import { revalidatePath } from "next/cache";
import { NextRequest } from "next/server";

// backend RevalidateWebhookListener의 REVALIDATE_PATHS와 일치시킨다.
// 화이트리스트 밖 경로는 무시해 임의 경로 재검증을 막는다(시크릿이 노출되더라도 방어심화).
const ALLOWED_PATHS = new Set(["/", "/latest", "/rounds", "/frequency", "/stats", "/companion"]);

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

  let body: { paths?: unknown };
  try {
    body = await req.json();
  } catch {
    return new Response("Invalid JSON body", { status: 400 });
  }

  const requested = Array.isArray(body.paths) ? body.paths : [];
  const paths = requested.filter(
    (path): path is string => typeof path === "string" && ALLOWED_PATHS.has(path)
  );

  for (const path of paths) {
    revalidatePath(path);
  }

  return Response.json({ revalidated: true, paths });
}
