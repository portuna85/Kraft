import { buildWebsiteJsonLd } from "@/lib/csp-inline-scripts";

type BreadcrumbItem = { name: string; item?: string };

type JsonLdBreadcrumbProps = {
  baseUrl: string;
  nonce?: string;
  items: BreadcrumbItem[];
};

export function JsonLdBreadcrumb({ baseUrl, nonce, items }: JsonLdBreadcrumbProps) {
  const schema = {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: [
      { "@type": "ListItem", position: 1, name: "홈", item: baseUrl },
      ...items.map((crumb, index) => ({
        "@type": "ListItem",
        position: index + 2,
        name: crumb.name,
        ...(crumb.item ? { item: crumb.item } : {}),
      })),
    ],
  };
  return (
    <script
      type="application/ld+json"
      nonce={nonce}
      suppressHydrationWarning
      dangerouslySetInnerHTML={{ __html: JSON.stringify(schema) }}
    />
  );
}

type JsonLdWebSiteProps = {
  baseUrl: string;
  nonce?: string;
};

export function JsonLdWebSite({ baseUrl, nonce }: JsonLdWebSiteProps) {
  return (
    <script
      type="application/ld+json"
      nonce={nonce}
      suppressHydrationWarning
      dangerouslySetInnerHTML={{ __html: JSON.stringify(buildWebsiteJsonLd(baseUrl)) }}
    />
  );
}
