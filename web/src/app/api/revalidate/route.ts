import { timingSafeEqual } from "crypto";
import { revalidatePath } from "next/cache";
import { NextRequest } from "next/server";

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

  const body = await req.json() as { paths?: string[] };
  const paths = body.paths ?? [];

  for (const path of paths) {
    revalidatePath(path);
  }

  return Response.json({ revalidated: true, paths });
}
