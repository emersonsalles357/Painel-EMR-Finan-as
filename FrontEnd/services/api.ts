import { mockApi, isMock } from './mockApi';
import { realApi } from './realApi';
import type { Gasto, Recebimento, Investimento } from '../types';

export const isMockMode = isMock();

export const authService = {
  async login(email: string, password: string) {
    if (isMock()) {
      return mockApi.auth.login(email, password);
    }
    return realApi.auth.login(email, password);
  },
  logout() {
    localStorage.removeItem('emr_financas_token');
    localStorage.removeItem('emr_financas_user');
  },
  isAuthenticated() {
    return Boolean(localStorage.getItem('emr_financas_token'));
  },
};

export const gastosService = {
  async list() {
    if (isMock()) return mockApi.gastos.list();
    return realApi.gastos.list();
  },
  async create(payload: Omit<Gasto, 'id'>) {
    if (isMock()) return mockApi.gastos.create(payload);
    return realApi.gastos.create(payload);
  },
  async update(id: string, payload: Partial<Gasto>) {
    if (isMock()) return mockApi.gastos.update(id, payload);
    return realApi.gastos.update(id, payload);
  },
  async remove(id: string) {
    if (isMock()) return mockApi.gastos.remove(id);
    return realApi.gastos.remove(id);
  },
};

export const recebimentosService = {
  async list() {
    if (isMock()) return mockApi.recebimentos.list();
    return realApi.recebimentos.list();
  },
  async create(payload: Omit<Recebimento, 'id'>) {
    if (isMock()) return mockApi.recebimentos.create(payload);
    return realApi.recebimentos.create(payload);
  },
  async update(id: string, payload: Partial<Recebimento>) {
    if (isMock()) return mockApi.recebimentos.update(id, payload);
    return realApi.recebimentos.update(id, payload);
  },
  async remove(id: string) {
    if (isMock()) return mockApi.recebimentos.remove(id);
    return realApi.recebimentos.remove(id);
  },
};

export const investimentosService = {
  async list() {
    if (isMock()) return mockApi.investimentos.list();
    return realApi.investimentos.list();
  },
  async create(payload: Omit<Investimento, 'id'>) {
    if (isMock()) return mockApi.investimentos.create(payload);
    return realApi.investimentos.create(payload);
  },
  async update(id: string, payload: Partial<Investimento>) {
    if (isMock()) return mockApi.investimentos.update(id, payload);
    return realApi.investimentos.update(id, payload);
  },
  async remove(id: string) {
    if (isMock()) return mockApi.investimentos.remove(id);
    return realApi.investimentos.remove(id);
  },
};

export { resetMockData } from './mockApi';
