import { revalidatePath } from "next/cache";
import { NextRequest } from "next/server";

export async function POST(req: NextRequest) {
  const secret = req.headers.get("X-Revalidate-Secret");
  if (!secret || secret !== process.env.KRAFT_REVALIDATE_SECRET) {
    return new Response("Unauthorized", { status: 401 });
  }

  const body = await req.json() as { paths?: string[] };
  const paths = body.paths ?? [];

  for (const path of paths) {
    revalidatePath(path);
  }

  return Response.json({ revalidated: true, paths });
}
