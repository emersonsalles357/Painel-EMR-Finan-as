import { Navbar } from '../components/Navbar';
import { MetricCard } from '../components/MetricCard';
import { DashboardChart } from '../components/DashboardChart';
import { useFinancas } from '../contexts/FinancasContext';
import { formatCurrency, formatDate } from '../utils';

export function DashboardPage() {
  const { gastos, recebimentos, investimentos } = useFinancas();

  const totalGastos = gastos.reduce((sum, item) => sum + Number(item.valor), 0);
  const totalReceitas = recebimentos.reduce((sum, item) => sum + Number(item.valor), 0);
  const totalInvestimentos = investimentos.reduce((sum, item) => sum + Number(item.valor), 0);
  const saldo = totalReceitas - totalGastos + totalInvestimentos;
  const percentual = totalReceitas ? (((saldo - totalGastos) / totalReceitas) * 100).toFixed(1) : '0';

  const atividades = [
    ...gastos.map((item) => ({ title: item.descricao, type: 'Despesa' as const, value: -item.valor, date: item.data })),
    ...recebimentos.map((item) => ({ title: item.cliente, type: 'Receita' as const, value: item.valor, date: item.data })),
  ].sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()).slice(0, 5);

  return (
    <section className="page-enter">
      <Navbar title="Dashboard" />
      <div className="main-content">
        <div className="dashboard-hero">
          <div className="access-panel">
            <div className="emr-mark">EMR</div>
            <div className="access-title mb-3">Acesse sua conta</div>
            <a href="/perfil" className="btn btn-primary-gradient w-100 py-2">Entrar <i className="bi bi-person-circle ms-2"></i></a>
            <div className="d-flex justify-content-between mt-3 small position-relative" style={{ zIndex: 2 }}>
              <a href="/perfil" className="text-muted-soft">cadastre-se</a>
              <a href="/perfil" className="text-muted-soft">recuperar senha</a>
            </div>
          </div>
          <div className="assistant-panel">
            <div className="assistant-title mb-4">Seja bem-vindo</div>
            <div className="assistant-question">O QUE VOCÊ DESEJA ACESSAR?</div>
            <div className="d-flex justify-content-between align-items-center gap-12 mt-18px">
              <strong>Olá! Como posso te ajudar?</strong>
              <span className="avatar">S</span>
            </div>
            <div className="small text-muted-soft mt-3">Sugestões rápidas</div>
            <div className="quick-grid">
              <a href="/recebimentos"><i className="bi bi-cash-coin me-1"></i>Recebimentos</a>
              <a href="/investimentos"><i className="bi bi-graph-up-arrow me-1"></i>Investimentos</a>
              <a href="/gastos"><i className="bi bi-credit-card me-1"></i>Gastos</a>
              <a href="/investimentos">Como inserir novo investimento?</a>
            </div>
          </div>
        </div>

        <h2 className="section-title h5 mb-3">Menu principal</h2>
        <div className="row g-3 mb-4">
          <div className="col-md-4"><MetricCard label="Recebimentos" value={totalReceitas} icon="bi-arrow-down-circle" hint="Entrada consolidada" /></div>
          <div className="col-md-4"><MetricCard label="Gastos" value={totalGastos} icon="bi-arrow-up-circle" hint="Saída operacional" /></div>
          <div className="col-md-4"><MetricCard label="Investimentos" value={totalInvestimentos} icon="bi-pie-chart" hint={`${percentual}% eficiência`} /></div>
        </div>
        <div className="row g-3 mb-4">
          <div className="col-sm-6 col-xl-3"><MetricCard label="Saldo total" value={saldo} icon="bi-wallet2" hint="Visão consolidada" /></div>
          <div className="col-sm-6 col-xl-3"><MetricCard label="Receitas" value={totalReceitas} icon="bi-arrow-down-circle" hint="+12,4% no mês" /></div>
          <div className="col-sm-6 col-xl-3"><MetricCard label="Despesas" value={totalGastos} icon="bi-arrow-up-circle" hint="Controle operacional" /></div>
          <div className="col-sm-6 col-xl-3"><MetricCard label="Investimentos" value={totalInvestimentos} icon="bi-pie-chart" hint="Carteira ativa" /></div>
        </div>
        <div className="row g-3">
          <div className="col-xl-8">
            <DashboardChart labels={['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun']} receitas={[4200, 6100, 5400, 7200, 8600, 9300]} despesas={[2300, 2800, 2600, 3100, 3700, 4200]} />
          </div>
          <div className="col-xl-4">
            <div className="activity-card h-100">
              <h2 className="h5 fw-bold mb-3">Atividades recentes</h2>
              {atividades.map((item, i) => (
                <div key={i} className="activity-item">
                  <div><strong>{item.title}</strong><br /><span className="small text-muted-soft">{item.type} • {formatDate(item.date)}</span></div>
                  <span className={`fw-bold ${item.value < 0 ? 'text-danger' : 'text-success'}`}>{formatCurrency(item.value)}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
