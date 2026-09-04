export const formatCurrency = (value: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(Number(value || 0));

export const formatDate = (value: string) =>
  value ? new Intl.DateTimeFormat('pt-BR', { timeZone: 'UTC' }).format(new Date(value)) : '-';

export const toNumber = (value: string) =>
  Number(String(value).replace(/[^0-9,-]/g, '').replace(',', '.')) || 0;

export const debounce = <T extends (...args: unknown[]) => unknown>(fn: T, delay = 300) => {
  let timer: ReturnType<typeof setTimeout>;
  return (...args: Parameters<T>) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delay);
  };
};

const sanitizeMap: Record<string, string> = { '&': '&', '<': '<', '>': '>', "'": '&#039;', '"': '"' };

export const sanitize = (value: string = '') =>
  String(value).replace(/[&<>'"]/g, (char) => sanitizeMap[char] ?? char);
