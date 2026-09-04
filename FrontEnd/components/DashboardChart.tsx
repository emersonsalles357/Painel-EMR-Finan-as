import { Line } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, Filler);

interface DashboardChartProps {
  labels: string[];
  receitas: number[];
  despesas: number[];
}

export function DashboardChart({ labels, receitas, despesas }: DashboardChartProps) {
  const data = {
    labels,
    datasets: [
      {
        label: 'Receitas',
        data: receitas,
        tension: 0.42,
        fill: true,
        borderColor: '#3ed8cd',
        backgroundColor: 'rgba(62, 216, 205, .14)',
        pointBackgroundColor: '#3ed8cd',
        pointBorderColor: '#06121b',
      },
      {
        label: 'Despesas',
        data: despesas,
        tension: 0.42,
        fill: true,
        borderColor: '#d9a441',
        backgroundColor: 'rgba(217, 164, 65, .12)',
        pointBackgroundColor: '#d9a441',
        pointBorderColor: '#06121b',
      },
    ],
  };

  const options = {
    responsive: true,
    plugins: {
      legend: { position: 'bottom' as const, labels: { color: '#91a7b0', usePointStyle: true, padding: 18 } },
    },
    scales: {
      x: { grid: { color: 'rgba(255,255,255,.06)' }, ticks: { color: '#91a7b0' } },
      y: { beginAtZero: true, grid: { color: 'rgba(255,255,255,.06)' }, ticks: { color: '#91a7b0' } },
    },
  };

  return (
    <div className="chart-card h-100">
      <div className="d-flex justify-content-between align-items-start mb-3">
        <div><h2 className="h5 fw-bold mb-1">Performance financeira</h2><p className="text-muted-soft small mb-0">Receitas x despesas nos últimos meses</p></div>
        <span className="badge rounded-pill badge-soft-info">Tempo real</span>
      </div>
      <Line data={data} options={options} />
    </div>
  );
}
