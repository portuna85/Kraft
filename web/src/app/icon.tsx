import { ImageResponse } from "next/og";

export const contentType = "image/png";

const SIZES: Record<string, number> = { "32": 32, "192": 192, "512": 512 };

export function generateImageMetadata() {
  return Object.keys(SIZES).map((id) => ({
    id,
    contentType: "image/png" as const,
    size: { width: SIZES[id], height: SIZES[id] },
  }));
}

export default function Icon({ params }: { params: { id: string } }) {
  const sz = SIZES[params.id] ?? 32;
  const fontSize = Math.round(sz * 0.53);
  return new ImageResponse(
    (
      <div
        style={{
          width: sz,
          height: sz,
          borderRadius: "50%",
          background: "#ffc857",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontSize,
          fontWeight: 800,
          color: "#1d1a17",
        }}
      >
        K
      </div>
    ),
    { width: sz, height: sz },
  );
}
