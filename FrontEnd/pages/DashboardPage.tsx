import { Link } from "react-router-dom";
import { Doughnut } from "react-chartjs-2";
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from "chart.js";
import { Navbar } from "../components/Navbar";
import { DashboardChart } from "../components/DashboardChart";
import { useFinancas } from "../contexts/FinancasContext";
import { useAuth } from "../contexts/AuthContext";
import { formatCurrency, formatDate } from "../utils";
ChartJS.register(ArcElement, Tooltip, Legend);
export function DashboardPage() {
  const { gastos, recebimentos, investimentos, loading, error, loadAll } =
    useFinancas();
  const { user } = useAuth();
  const expenses = gastos.reduce((s, x) => s + Number(x.valor), 0),
    income = recebimentos.reduce((s, x) => s + Number(x.valor), 0),
    invested = investimentos.reduce((s, x) => s + Number(x.valor), 0);
  const months = [
    ...new Set([...gastos, ...recebimentos].map((x) => x.data.slice(0, 7))),
  ].sort();
  const totals = (items: { data: string; valor: number }[]) =>
    months.map((m) =>
      items
        .filter((x) => x.data.startsWith(m))
        .reduce((s, x) => s + Number(x.valor), 0),
    );
  const categories = Object.entries(
    gastos.reduce<Record<string, number>>((acc, x) => {
      acc[x.categoria || "Outros"] =
        (acc[x.categoria || "Outros"] || 0) + Number(x.valor);
      return acc;
    }, {}),
  );
  const activities = [
    ...gastos.map((x) => ({
      id: "g" + x.id,
      title: x.descricao,
      type: "Despesa",
      value: -x.valor,
      date: x.data,
    })),
    ...recebimentos.map((x) => ({
      id: "r" + x.id,
      title: String(x.descricao || x.cliente),
      type: "Receita",
      value: x.valor,
      date: x.data,
    })),
    ...investimentos.map((x) => ({
      id: "i" + x.id,
      title: x.ativo,
      type: "Investimento",
      value: x.valor,
      date: x.data,
    })),
  ]
    .sort((a, b) => b.date.localeCompare(a.date))
    .slice(0, 6);
  const cards = [
    {
      label: "Saldo consolidado",
      value: income - expenses + invested,
      icon: "wallet2",
      tone: "cyan",
      hint: "Receitas − despesas + investimentos",
    },
    {
      label: "Receitas",
      value: income,
      icon: "arrow-up",
      tone: "positive",
      hint: "Total cadastrado",
    },
    {
      label: "Despesas",
      value: expenses,
      icon: "arrow-down",
      tone: "negative",
      hint: "Total cadastrado",
    },
    {
      label: "Investimentos",
      value: invested,
      icon: "bar-chart",
      tone: "blue",
      hint: "Capital aplicado",
    },
  ];
  return (
    <section>
      <Navbar />
      <main className="main-content">
        <div className="page-heading">
          <span className="eyebrow">SEU PANORAMA FINANCEIRO</span>
          <h2>Olá{user?.name ? ", " + user.name : ""}</h2>
          <p>Visão geral das suas finanças</p>
        </div>
        {loading ? (
          <div className="chart-card" role="status">
            Carregando suas finanças…
          </div>
        ) : error ? (
          <div className="alert alert-danger" role="alert">
            {error}{" "}
            <button className="btn btn-light" onClick={() => void loadAll()}>
              Tentar novamente
            </button>
          </div>
        ) : (
          <>
            <div className="metrics-grid">
              {cards.map((c) => (
                <article key={c.label} className={"summary-card " + c.tone}>
                  <div className="summary-top">
                    <span className="summary-icon">
                      <i className={"bi bi-" + c.icon} />
                    </span>
                    <div>
                      <h3>{c.label}</h3>
                      <strong>{formatCurrency(c.value)}</strong>
                    </div>
                  </div>
                  <p>{c.hint}</p>
                </article>
              ))}
            </div>
            <div className="dashboard-grid">
              <div className="chart-card flow-panel">
                <h3 className="h5">Fluxo financeiro</h3>
                <p className="text-muted-soft small">
                  Totais por mês dos lançamentos cadastrados
                </p>
                {months.length ? (
                  <DashboardChart
                    labels={months.map((m) => m.slice(5) + "/" + m.slice(0, 4))}
                    receitas={totals(recebimentos)}
                    despesas={totals(gastos)}
                  />
                ) : (
                  <div className="empty-state">
                    <i className="bi bi-graph-up" />
                    <p>Seu histórico começa com o primeiro lançamento.</p>
                    <Link to="/recebimentos">Cadastrar receita →</Link>
                  </div>
                )}
              </div>
              <aside className="chart-card">
                <h3 className="h5 mb-3">Ações rápidas</h3>
                <div className="quick-actions">
                  <Link className="positive" to="/recebimentos?novo=1">
                    <i className="bi bi-arrow-up" />
                    Nova receita
                    <i className="bi bi-plus-lg" />
                  </Link>
                  <Link className="negative" to="/gastos?novo=1">
                    <i className="bi bi-arrow-down" />
                    Nova despesa
                    <i className="bi bi-plus-lg" />
                  </Link>
                  <Link className="blue" to="/investimentos?novo=1">
                    <i className="bi bi-bar-chart" />
                    Novo investimento
                    <i className="bi bi-plus-lg" />
                  </Link>
                </div>
                <p className="small text-muted-soft mt-4 mb-0">
                  <i className="bi bi-info-circle me-2" />O saldo consolidado
                  inclui os investimentos e não representa apenas dinheiro
                  disponível.
                </p>
              </aside>
              <section className="chart-card movements">
                <h3 className="h5 mb-3">Últimas movimentações</h3>
                {activities.length ? (
                  <div className="table-responsive">
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Data</th>
                          <th>Descrição</th>
                          <th>Tipo</th>
                          <th className="text-end">Valor</th>
                        </tr>
                      </thead>
                      <tbody>
                        {activities.map((a) => (
                          <tr key={a.id}>
                            <td>{formatDate(a.date)}</td>
                            <td>{a.title}</td>
                            <td>{a.type}</td>
                            <td
                              className={
                                "text-end " +
                                (a.type === "Despesa"
                                  ? "text-danger"
                                  : "text-success")
                              }
                            >
                              {formatCurrency(a.value)}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="empty-state">
                    <i className="bi bi-receipt" />
                    <p>Você ainda não possui movimentações cadastradas.</p>
                    <Link to="/gastos">Cadastrar despesa →</Link>
                  </div>
                )}
              </section>
              <section className="chart-card distribution">
                <h3 className="h5 mb-3">Distribuição dos gastos</h3>
                {categories.length ? (
                  <div className="donut-wrap">
                    <Doughnut
                      aria-label="Gastos por categoria"
                      data={{
                        labels: categories.map((x) => x[0]),
                        datasets: [
                          {
                            data: categories.map((x) => x[1]),
                            backgroundColor: [
                              "#00cfe8",
                              "#278eff",
                              "#5e6ce8",
                              "#c256da",
                              "#00b8a7",
                              "#eab564",
                            ],
                            borderColor: "#002031",
                            borderWidth: 2,
                          },
                        ],
                      }}
                      options={{
                        maintainAspectRatio: false,
                        plugins: {
                          legend: {
                            position: "bottom",
                            labels: { color: "#b6d2e2", usePointStyle: true },
                          },
                        },
                        cutout: "72%",
                      }}
                    />
                  </div>
                ) : (
                  <div className="empty-state">
                    <i className="bi bi-pie-chart" />
                    <p>Você ainda não possui despesas cadastradas.</p>
                    <Link to="/gastos">Adicionar despesa →</Link>
                  </div>
                )}
              </section>
            </div>
          </>
        )}
      </main>
    </section>
  );
}
