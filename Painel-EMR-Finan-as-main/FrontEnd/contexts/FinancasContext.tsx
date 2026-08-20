import { createContext, useContext, useReducer, type ReactNode } from 'react';
import type { FinancasState, Gasto, Recebimento, Investimento } from '../types';
import {
  gastosService,
  recebimentosService,
  investimentosService,
  isMockMode,
} from '../services/api';
import { resetMockData } from '../services/mockApi';

type FinancasAction =
  | { type: 'SET_GASTOS'; gastos: Gasto[] }
  | { type: 'SET_RECEBIMENTOS'; recebimentos: Recebimento[] }
  | { type: 'SET_INVESTIMENTOS'; investimentos: Investimento[] };

interface FinancasContextValue extends FinancasState {
  setGastos: (gastos: Gasto[]) => void;
  setRecebimentos: (recebimentos: Recebimento[]) => void;
  setInvestimentos: (investimentos: Investimento[]) => void;
  loadAll: () => Promise<void>;
}

const FinancasContext = createContext<FinancasContextValue | null>(null);

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

const initialState: FinancasState = isMockMode
  ? { gastos: seedGastos, recebimentos: seedRecebimentos, investimentos: seedInvestimentos }
  : { gastos: [], recebimentos: [], investimentos: [] };

function financaReducer(state: FinancasState, action: FinancasAction): FinancasState {
  switch (action.type) {
    case 'SET_GASTOS': return { ...state, gastos: action.gastos };
    case 'SET_RECEBIMENTOS': return { ...state, recebimentos: action.recebimentos };
    case 'SET_INVESTIMENTOS': return { ...state, investimentos: action.investimentos };
    default: return state;
  }
}

export function FinancasProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(financaReducer, initialState);

  const loadAll = async () => {
    if (isMockMode) {
      resetMockData();
      const [gastos, recebimentos, investimentos] = await Promise.all([
        gastosService.list(),
        recebimentosService.list(),
        investimentosService.list(),
      ]);
      dispatch({ type: 'SET_GASTOS', gastos });
      dispatch({ type: 'SET_RECEBIMENTOS', recebimentos });
      dispatch({ type: 'SET_INVESTIMENTOS', investimentos });
    } else {
      const [gastos, recebimentos, investimentos] = await Promise.all([
        gastosService.list(),
        recebimentosService.list(),
        investimentosService.list(),
      ]);
      dispatch({ type: 'SET_GASTOS', gastos });
      dispatch({ type: 'SET_RECEBIMENTOS', recebimentos });
      dispatch({ type: 'SET_INVESTIMENTOS', investimentos });
    }
  };

  return (
    <FinancasContext.Provider value={{
      ...state,
      setGastos: (gastos) => dispatch({ type: 'SET_GASTOS', gastos }),
      setRecebimentos: (recebimentos) => dispatch({ type: 'SET_RECEBIMENTOS', recebimentos }),
      setInvestimentos: (investimentos) => dispatch({ type: 'SET_INVESTIMENTOS', investimentos }),
      loadAll,
    }}>
      {children}
    </FinancasContext.Provider>
  );
}

export const useFinancas = () => {
  const ctx = useContext(FinancasContext);
  if (!ctx) throw new Error('useFinancas must be used within FinancasProvider');
  return ctx;
};
