import {
  createContext,
  useContext,
  useEffect,
  useState,
  useRef,
  type ReactNode,
} from "react";
import type { FinancasState, Gasto, Recebimento, Investimento } from "../types";
import {
  gastosService,
  recebimentosService,
  investimentosService,
} from "../services/api";
import { useAuth } from "./AuthContext";
interface Value extends FinancasState {
  loading: boolean;
  error: string;
  setGastos: (v: Gasto[]) => void;
  setRecebimentos: (v: Recebimento[]) => void;
  setInvestimentos: (v: Investimento[]) => void;
  loadAll: () => Promise<void>;
}
const Context = createContext<Value | null>(null);
const empty: FinancasState = {
  gastos: [],
  recebimentos: [],
  investimentos: [],
};
export function FinancasProvider({ children }: { children: ReactNode }) {
  const { token } = useAuth();
  const currentToken = useRef(token);
  currentToken.current = token;
  const [state, setState] = useState(empty);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const loadAll = async () => {
    const requestedToken = token;
    if (!token) return;
    setLoading(true);
    setError("");
    try {
      const [gastos, recebimentos, investimentos] = await Promise.all([
        gastosService.list(),
        recebimentosService.list(),
        investimentosService.list(),
      ]);
      if (currentToken.current === requestedToken)
        setState({ gastos, recebimentos, investimentos });
    } catch {
      if (currentToken.current === requestedToken)
        setError("Não foi possível carregar suas finanças. Tente novamente.");
    } finally {
      if (currentToken.current === requestedToken) setLoading(false);
    }
  };
  useEffect(() => {
    setState(empty);
    if (token) void loadAll();
    else setLoading(false);
  }, [token]);
  return (
    <Context.Provider
      value={{
        ...state,
        loading,
        error,
        loadAll,
        setGastos: (gastos) => setState((s) => ({ ...s, gastos })),
        setRecebimentos: (recebimentos) =>
          setState((s) => ({ ...s, recebimentos })),
        setInvestimentos: (investimentos) =>
          setState((s) => ({ ...s, investimentos })),
      }}
    >
      {children}
    </Context.Provider>
  );
}
export const useFinancas = () => {
  const value = useContext(Context);
  if (!value) throw new Error("FinancasProvider obrigatório");
  return value;
};
