const dateFormatter = new Intl.DateTimeFormat("ko-KR", {
  timeZone: "Asia/Seoul",
  year: "numeric",
  month: "long",
  day: "numeric",
  weekday: "short"
});

const dateTimeFormatter = new Intl.DateTimeFormat("ko-KR", {
  timeZone: "Asia/Seoul",
  year: "numeric",
  month: "long",
  day: "numeric",
  weekday: "short",
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
  hour12: false
});

const numberFormatter = new Intl.NumberFormat("ko-KR");

export function formatDrawDate(value: string): string {
  return dateFormatter.format(new Date(`${value}T00:00:00+09:00`));
}

export function formatDateTime(value: string): string {
  return dateTimeFormatter.format(new Date(value));
}

export function formatCurrency(value: number): string {
  return `${numberFormatter.format(value)}원`;
}

export function formatPlainNumber(value: number): string {
  return numberFormatter.format(value);
}
