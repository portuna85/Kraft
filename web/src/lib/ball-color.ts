export function ballColorClass(n: number): string {
  if (n <= 10) return "";
  if (n <= 20) return "ball-blue";
  if (n <= 30) return "ball-red";
  if (n <= 40) return "ball-gray";
  return "ball-green";
}
