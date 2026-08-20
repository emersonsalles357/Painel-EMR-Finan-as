export interface User {
  id: string;
  name: string;
  email: string;
  [key: string]: unknown;
}

export interface AuthState {
  token: string | null;
  user: User | null;
}

export interface Gasto {
  id: string;
  descricao: string;
  categoria: string;
  valor: number;
  status: 'Pago' | 'Pendente' | 'Atrasado';
  data: string;
  [key: string]: unknown;
}

export interface Recebimento {
  id: string;
  cliente: string;
  categoria: string;
  valor: number;
  status: 'Recebido' | 'Em aberto' | 'Atrasado';
  data: string;
  [key: string]: unknown;
}

export interface Investimento {
  id: string;
  ativo: string;
  tipo: 'Renda Fixa' | 'Renda Variável' | 'Cripto' | 'Fundo';
  valor: number;
  rentabilidade: number;
  status: 'Ativo' | 'Resgatado' | 'Pendente';
  data: string;
  [key: string]: unknown;
}

export interface FinancasState {
  gastos: Gasto[];
  recebimentos: Recebimento[];
  investimentos: Investimento[];
}

export type ToastType = 'success' | 'danger' | 'warning' | 'info';
