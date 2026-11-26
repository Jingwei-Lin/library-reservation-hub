// Calendar dates for date inputs ("YYYY-MM-DD") in the user's own time zone. toISOString() gives the UTC date,
// which in Sydney before 11 am is still yesterday.

const pad = (n) => String(n).padStart(2, '0');

/** A Date as the local calendar date, "YYYY-MM-DD" */
export const localDate = (date = new Date()) =>
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;

/** "YYYY-MM-DD" as local midnight (new Date("YYYY-MM-DD") would be UTC midnight) */
export const parseLocalDate = (value) => {
    const [y, m, d] = value.split('-').map(Number);
    return new Date(y, m - 1, d);
};

/** The local calendar date `days` after a date or a "YYYY-MM-DD" string */
export const addDays = (from, days) => {
    const date = typeof from === 'string' ? parseLocalDate(from) : new Date(from);
    date.setDate(date.getDate() + days);
    return localDate(date);
};
