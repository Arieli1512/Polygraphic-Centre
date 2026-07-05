export const dateTimeFormatter = new Intl.DateTimeFormat('pl-PL', {
    year: 'numeric',
    month: 'long',  // "June" (use 'short' for "Jun", 'numeric' for "6")
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    //timeZoneName: 'short' // "CEST" / "CET"
});