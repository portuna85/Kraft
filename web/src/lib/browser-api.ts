export class BrowserApiError extends Error {
  constructor(
    public readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = "BrowserApiError";
  }
}

export async function browserFetch<T>(
  url: string,
  init?: RequestInit,
): Promise<T> {
  const res = await fetch(url, {
    signal: AbortSignal.timeout(5000),
    ...init,
  });
  let body: unknown;
  try {
    body = await res.json();
  } catch {
    body = undefined;
  }
  if (!res.ok) {
    const { code, message } = (body ?? {}) as {
      code?: string;
      message?: string;
    };
    throw new BrowserApiError(code ?? "UNKNOWN", message ?? res.statusText ?? "");
  }
  return body as T;
}
