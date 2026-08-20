import { api } from './_apiClient';
import type { Gasto, Recebimento, Investimento } from '../types';

function toUser(backend: { codigo: number; nome: string; email: string }): { id: string; name: string; email: string } {
  return { id: String(backend.codigo), name: backend.nome, email: backend.email };
}

function toFrontendRecebimento(b: BackendRecebimento): Recebimento {
  return {
    id: String(b.codigo),
    descricao: b.descricao,
    cliente: b.origem,
    categoria: '',
    valor: b.valor,
    data: b.data,
    status: b.status as Recebimento['status'],
  };
}

function fromFrontendRecebimento(r: Partial<Recebimento>) {
  return {
    descricao: r.descricao ?? '',
    origem: r.cliente ?? '',
    valor: r.valor ?? 0,
    data: r.data ?? '',
    status: r.status ?? '',
  };
}

function toFrontendInvestimento(b: BackendInvestimento): Investimento {
  return {
    id: String(b.codigo),
    ativo: b.nome,
    tipo: b.tipo as Investimento['tipo'],
    valor: b.valorAplicado,
    rentabilidade: b.rentabilidadeMensal ?? 0,
    status: 'Ativo',
    data: b.dataAplicacao,
  };
}

function fromFrontendInvestimento(i: Partial<Investimento>) {
  return {
    nome: i.ativo ?? '',
    tipo: i.tipo ?? '',
    valorAplicado: i.valor ?? 0,
    rentabilidadeMensal: i.rentabilidade ?? 0,
    dataAplicacao: i.data ?? '',
  };
}

function toFrontendGasto(b: BackendGasto): Gasto {
  return {
    id: String(b.codigo),
    descricao: b.descricao,
    categoria: b.categoria,
    valor: b.valor,
    data: b.data,
    status: 'Pago',
  };
}

function fromFrontendGasto(g: Partial<Gasto>) {
  return {
    descricao: g.descricao ?? '',
    categoria: g.categoria ?? '',
    valor: g.valor ?? 0,
    data: g.data ?? '',
  };
}

export const realApi = {
  auth: {
    async login(email: string, senha: string) {
      const { data } = await api.post<{ codigo: number; nome: string; email: string; token: string }>(
        '/auth/login', { email, senha }
      );
      return { token: data.token, user: toUser(data) };
    },
  },

  gastos: {
    async list() {
      const { data } = await api.get<BackendGasto[]>('/gastos');
      return data.map(toFrontendGasto);
    },
    async create(payload: Omit<Gasto, 'id'>) {
      await api.post('/gastos', fromFrontendGasto(payload));
      return this.list();
    },
    async update(id: string, payload: Partial<Gasto>) {
      await api.put(`/gastos/${id}`, fromFrontendGasto(payload));
      return this.list();
    },
    async remove(id: string) {
      await api.delete(`/gastos/${id}`);
      return this.list();
    },
  },

  recebimentos: {
    async list() {
      const { data } = await api.get<BackendRecebimento[]>('/recebimentos');
      return data.map(toFrontendRecebimento);
    },
    async create(payload: Omit<Recebimento, 'id'>) {
      await api.post('/recebimentos', fromFrontendRecebimento(payload));
      return this.list();
    },
    async update(id: string, payload: Partial<Recebimento>) {
      await api.put(`/recebimentos/${id}`, fromFrontendRecebimento(payload));
      return this.list();
    },
    async remove(id: string) {
      await api.delete(`/recebimentos/${id}`);
      return this.list();
    },
  },

  investimentos: {
    async list() {
      const { data } = await api.get<BackendInvestimento[]>('/investimentos');
      return data.map(toFrontendInvestimento);
    },
    async create(payload: Omit<Investimento, 'id'>) {
      await api.post('/investimentos', fromFrontendInvestimento(payload));
      return this.list();
    },
    async update(id: string, payload: Partial<Investimento>) {
      await api.put(`/investimentos/${id}`, fromFrontendInvestimento(payload));
      return this.list();
    },
    async remove(id: string) {
      await api.delete(`/investimentos/${id}`);
      return this.list();
    },
  },
};

type BackendGasto = {
  codigo: number; descricao: string; categoria: string; valor: number;
  data: string; formaPagamento?: string; observacao?: string;
};

type BackendRecebimento = {
  codigo: number; descricao: string; origem: string; valor: number;
  data: string; status: string;
};

type BackendInvestimento = {
  codigo: number; nome: string; tipo: string; instituicao?: string;
  valorAplicado: number; rentabilidadeMensal?: number; dataAplicacao: string;
};
