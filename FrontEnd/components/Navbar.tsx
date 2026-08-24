import { useAuth } from '../contexts/AuthContext';
import { useFinancas } from '../contexts/FinancasContext';
import { formatCurrency } from '../utils';

function useSaldo() {
  const { gastos, recebimentos, investimentos } = useFinancas();
  const totalGastos = gastos.reduce((sum, item) => sum + Number(item.valor), 0);
  const totalReceitas = recebimentos.reduce((sum, item) => sum + Number(item.valor), 0);
  const totalInvestimentos = investimentos.reduce((sum, item) => sum + Number(item.valor), 0);
  return totalReceitas - totalGastos + totalInvestimentos;
}

interface NavbarProps {
  title?: string;
}

export function Navbar({ title = 'Dashboard' }: NavbarProps) {
  const saldo = useSaldo();
  const { user } = useAuth();
  return (
    <header className="topbar">
      <div className="d-flex align-items-center gap-3">
        <button
          type="button"
          className="btn btn-light d-lg-none rounded-3"
          aria-label="Abrir menu"
          aria-controls="sidebar"
          onClick={() => window.dispatchEvent(new Event('sidebar-toggle'))}
        >
          <i className="bi bi-list fs-5"></i>
        </button>
        <div>
          <div className="small text-muted-soft">Bem-vindo de volta</div>
          <h1 className="h5 mb-0 section-title">{title === 'Dashboard' ? 'Painel EMR Finanças' : title}</h1>
        </div>
      </div>
      <div className="d-flex align-items-center gap-3">
        <div className="text-end d-none d-sm-block">
          <div className="small text-muted-soft">Saldo consolidado</div>
          <div className="consolidated-balance">{formatCurrency(saldo)}</div>
        </div>
        <button className="btn btn-primary-gradient rounded-3">
          <i className="bi bi-cloud-arrow-down me-1"></i>Exportar
        </button>
        <div className="d-none d-md-grid brand-icon">{user?.name?.[0] || 'E'}</div>
      </div>
    </header>
  );
}
