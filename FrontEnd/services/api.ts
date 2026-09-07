
import { realApi } from './realApi';
import type { Gasto, Recebimento, Investimento } from '../types';

export const isMockMode = false;

export const authService = {
  async register(request: { nome: string; email: string; senha: string }) {
    return realApi.auth.register(request);
  },
  async login(email: string, password: string) {
    return realApi.auth.login(email, password);
  },
  async forgotPassword(email: string) {
    return realApi.auth.forgotPassword(email);
  },
  async resetPassword(token: string, novaSenha: string) {
    return realApi.auth.resetPassword(token, novaSenha);
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
    return realApi.gastos.list();
  },
  async create(payload: Omit<Gasto, 'id'>) {
    return realApi.gastos.create(payload);
  },
  async update(id: string, payload: Partial<Gasto>) {
    return realApi.gastos.update(id, payload);
  },
  async remove(id: string) {
    return realApi.gastos.remove(id);
  },
};

export const recebimentosService = {
  async list() {
    return realApi.recebimentos.list();
  },
  async create(payload: Omit<Recebimento, 'id'>) {
    return realApi.recebimentos.create(payload);
  },
  async update(id: string, payload: Partial<Recebimento>) {
    return realApi.recebimentos.update(id, payload);
  },
  async remove(id: string) {
    return realApi.recebimentos.remove(id);
  },
};

export const investimentosService = {
  async list() {
    return realApi.investimentos.list();
  },
  async create(payload: Omit<Investimento, 'id'>) {
    return realApi.investimentos.create(payload);
  },
  async update(id: string, payload: Partial<Investimento>) {
    return realApi.investimentos.update(id, payload);
  },
  async remove(id: string) {
    return realApi.investimentos.remove(id);
  },
};

export { resetMockData } from './mockApi';
