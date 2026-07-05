export function formatMoney(amountInGrosz: number): string {
  const pln = amountInGrosz / 100;
  return new Intl.NumberFormat("pl-PL", {
    style: "currency",
    currency: "PLN",
  }).format(pln);
}

export function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat("pl-PL", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function formatDayOfWeek(dayOfWeek: number): string {
  const names = ["pon", "wt", "sr", "czw", "pt", "sob", "niedz"];
  return names[dayOfWeek - 1] ?? "?";
}
