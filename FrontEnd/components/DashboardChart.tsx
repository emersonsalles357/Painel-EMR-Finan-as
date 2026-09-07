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
        borderColor: '#ff606c',
        backgroundColor: 'rgba(255, 96, 108, .12)',
        pointBackgroundColor: '#ff606c',
        pointBorderColor: '#06121b',
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: 'bottom' as const, labels: { color: '#91a7b0', usePointStyle: true, padding: 18 } },
    },
    scales: {
      x: { grid: { color: 'rgba(255,255,255,.06)' }, ticks: { color: '#91a7b0' } },
      y: { beginAtZero: true, grid: { color: 'rgba(255,255,255,.06)' }, ticks: { color: '#91a7b0' } },
    },
  };

  return (
    <div className="line-wrap">
      <Line aria-label="Receitas e despesas mensais em reais" data={data} options={options} />
    </div>
  );
}
