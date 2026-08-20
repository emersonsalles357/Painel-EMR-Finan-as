import { createContext, useContext, useReducer, type ReactNode } from 'react';
import type { User } from '../types';
import { authService } from '../services/api';

interface AuthState {
  token: string | null;
  user: User | null;
  loading: boolean;
}

type AuthAction =
  | { type: 'LOGIN_START' }
  | { type: 'LOGIN_SUCCESS'; token: string; user: User }
  | { type: 'LOGOUT' };

interface AuthContextValue extends AuthState {
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const initialState: AuthState = {
  token: localStorage.getItem('emr_financas_token'),
  user: JSON.parse(localStorage.getItem('emr_financas_user') || 'null'),
  loading: false,
};

function authReducer(state: AuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case 'LOGIN_START': return { ...state, loading: true };
    case 'LOGIN_SUCCESS': return { ...state, loading: false, token: action.token, user: action.user };
    case 'LOGOUT': return { ...state, token: null, user: null, loading: false };
    default: return state;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(authReducer, initialState);

  const login = async (email: string, password: string) => {
    dispatch({ type: 'LOGIN_START' });
    const { token, user } = await authService.login(email, password);
    localStorage.setItem('emr_financas_token', token);
    localStorage.setItem('emr_financas_user', JSON.stringify(user));
    dispatch({ type: 'LOGIN_SUCCESS', token, user });
  };

  const logout = () => {
    authService.logout();
    dispatch({ type: 'LOGOUT' });
  };

  return (
    <AuthContext.Provider value={{ ...state, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};
