import type { Gasto, Recebimento, Investimento, User } from '../types';

export const isMock = () => import.meta.env.VITE_USE_MOCK_API === 'true';

const delay = (ms = 400) => new Promise((r) => setTimeout(r, ms));

const seedGastos: Gasto[] = [
  { id: 'g1', descricao: 'Assinatura cloud', categoria: 'Tecnologia', valor: 189.9, status: 'Pago', data: '2026-05-03' },
  { id: 'g2', descricao: 'Marketing Ads', categoria: 'Marketing', valor: 950, status: 'Pendente', data: '2026-05-08' },
  { id: 'g3', descricao: 'Coworking', categoria: 'Operacional', valor: 780, status: 'Pago', data: '2026-05-12' },
];

const seedRecebimentos: Recebimento[] = [
  { id: 'r1', cliente: 'Cliente Enterprise', categoria: 'SaaS', valor: 6400, status: 'Recebido', data: '2026-05-05' },
  { id: 'r2', cliente: 'Consultoria API', categoria: 'Serviços', valor: 2200, status: 'Em aberto', data: '2026-05-18' },
];

const seedInvestimentos: Investimento[] = [
  { id: 'i1', ativo: 'CDB Liquidez', tipo: 'Renda Fixa', valor: 12000, rentabilidade: 1.05, status: 'Ativo', data: '2026-04-01' },
  { id: 'i2', ativo: 'ETF Tecnologia', tipo: 'Renda Variável', valor: 8600, rentabilidade: 2.4, status: 'Ativo', data: '2026-03-15' },
];

let _gastos = [...seedGastos];
let _recebimentos = [...seedRecebimentos];
let _investimentos = [...seedInvestimentos];

export const mockApi = {
  auth: {
    async login(_email: string, _password: string) {
      await delay(500);
      return {
        token: `mock-jwt-${Date.now()}`,
        user: { id: '1', name: 'Gestor EMR', email: 'admin@emrfinancas.com' } as User,
      };
    },
  },

  gastos: {
    async list(): Promise<Gasto[]> { await delay(); return [..._gastos]; },
    async create(payload: Omit<Gasto, 'id'>): Promise<Gasto[]> {
      await delay();
      _gastos = [..._gastos, { ...payload, id: crypto.randomUUID() } as Gasto];
      return [..._gastos];
    },
    async update(id: string, payload: Partial<Gasto>): Promise<Gasto[]> {
      await delay();
      _gastos = _gastos.map((g) => g.id === id ? { ...g, ...payload } : g);
      return [..._gastos];
    },
    async remove(id: string): Promise<Gasto[]> {
      await delay();
      _gastos = _gastos.filter((g) => g.id !== id);
      return [..._gastos];
    },
  },

  recebimentos: {
    async list(): Promise<Recebimento[]> { await delay(); return [..._recebimentos]; },
    async create(payload: Omit<Recebimento, 'id'>): Promise<Recebimento[]> {
      await delay();
      _recebimentos = [..._recebimentos, { ...payload, id: crypto.randomUUID() } as Recebimento];
      return [..._recebimentos];
    },
    async update(id: string, payload: Partial<Recebimento>): Promise<Recebimento[]> {
      await delay();
      _recebimentos = _recebimentos.map((r) => r.id === id ? { ...r, ...payload } : r);
      return [..._recebimentos];
    },
    async remove(id: string): Promise<Recebimento[]> {
      await delay();
      _recebimentos = _recebimentos.filter((r) => r.id !== id);
      return [..._recebimentos];
    },
  },

  investimentos: {
    async list(): Promise<Investimento[]> { await delay(); return [..._investimentos]; },
    async create(payload: Omit<Investimento, 'id'>): Promise<Investimento[]> {
      await delay();
      _investimentos = [..._investimentos, { ...payload, id: crypto.randomUUID() } as Investimento];
      return [..._investimentos];
    },
    async update(id: string, payload: Partial<Investimento>): Promise<Investimento[]> {
      await delay();
      _investimentos = _investimentos.map((i) => i.id === id ? { ...i, ...payload } : i);
      return [..._investimentos];
    },
    async remove(id: string): Promise<Investimento[]> {
      await delay();
      _investimentos = _investimentos.filter((i) => i.id !== id);
      return [..._investimentos];
    },
  },
};

export function resetMockData() {
  _gastos = [...seedGastos];
  _recebimentos = [...seedRecebimentos];
  _investimentos = [...seedInvestimentos];
}
