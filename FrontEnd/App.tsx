import type { ReactNode } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { FinancasProvider } from './contexts/FinancasContext';
import { ToastProvider } from './contexts/ToastContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Sidebar } from './components/Sidebar';
import { ToastContainer } from './components/ToastContainer';

// Importações das páginas originais
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { GastosPage } from './pages/GastosPage';
import { RecebimentosPage } from './pages/RecebimentosPage';
import { InvestimentosPage } from './pages/InvestimentosPage';
import { PerfilPage } from './pages/PerfilPage';
import { NotFoundPage } from './pages/NotFoundPage';

// Novas importações adicionadas (certifique-se de que estes arquivos existem na pasta pages)
import { CadastroPage } from './pages/CadastroPage'; 
import { RecuperarSenhaPage } from './pages/RecuperarSenhaPage';

import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap-icons/font/bootstrap-icons.css';
import './styles/variables.css';
import './styles/global.css';
import './styles/sidebar.css';
import './styles/dashboard.css';
import './styles/forms.css';
import './styles/tables.css';
import './styles/responsive.css';

function AppLayout({ children }: { children: ReactNode }) {
  return (
    <div className="app-shell">
      <Sidebar />
      <div className="main-wrapper">
        {children}
      </div>
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter basename="/Painel-EMR-Finan-as">
      <AuthProvider>
        <FinancasProvider>
          <ToastProvider>
            <ToastContainer />
            <Routes>
              {/* --- ROTAS PÚBLICAS --- */}
              <Route path="/login" element={<LoginPage />} />
              
              {/* Novas rotas públicas adicionadas para resolver o erro 404 */}
              <Route path="/cadastro" element={<CadastroPage />} />
              <Route path="/recuperar-senha" element={<RecuperarSenhaPage />} />

              {/* --- ROTAS PROTEGIDAS --- */}
              <Route
                path="/dashboard"
                element={
                  <ProtectedRoute>
                    <AppLayout><DashboardPage /></AppLayout>
                  </ProtectedRoute>
                }
              />
              <Route
                path="/gastos"
                element={
                  <ProtectedRoute>
                    <AppLayout><GastosPage /></AppLayout>
                  </ProtectedRoute>
                }
              />
              <Route
                path="/recebimentos"
                element={
                  <ProtectedRoute>
                    <AppLayout><RecebimentosPage /></AppLayout>
                  </ProtectedRoute>
                }
              />
              <Route
                path="/investimentos"
                element={
                  <ProtectedRoute>
                    <AppLayout><InvestimentosPage /></AppLayout>
                  </ProtectedRoute>
                }
              />
              <Route
                path="/perfil"
                element={
                  <ProtectedRoute>
                    <AppLayout><PerfilPage /></AppLayout>
                  </ProtectedRoute>
                }
              />
              
              {/* Redirecionamento padrão */}
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              
              {/* Rota 404 (NotFound) - Removido o <ProtectedRoute> para que funcione publicamente */}
              <Route path="*" element={<AppLayout><NotFoundPage /></AppLayout>} />
            </Routes>
          </ToastProvider>
        </FinancasProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
