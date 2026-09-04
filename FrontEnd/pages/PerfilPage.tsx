import { Navbar } from '../components/Navbar';
import { MetricCard } from '../components/MetricCard';
import { useAuth } from '../contexts/AuthContext';

export function PerfilPage() {
  const { user } = useAuth() ?? { user: { name: 'Gestor EMR', email: 'admin@emrfinancas.com' } };

  return (
    <section className="page-enter">
      <Navbar title="Perfil" />
      <div className="main-content">
        <div className="glass-panel p-4 p-lg-5">
          <div className="d-flex flex-wrap align-items-center gap-4">
            <div className="rounded-circle bg-dark text-white d-grid fs-2" style={{ width: '88px', height: '88px', placeItems: 'center' }}>
              {user?.name?.[0] || 'U'}
            </div>
            <div>
              <h2 className="section-title mb-1">{user?.name}</h2>
              <p className="text-muted-soft mb-0">{user?.email}</p>
            </div>
          </div>
          <hr className="my-4" />
          <div className="row g-3">
            <div className="col-md-4"><MetricCard label="JWT" value={0} icon="bi-shield-check" hint="Autenticação pronta para backend" /></div>
            <div className="col-md-4"><MetricCard label="100%" value={0} icon="bi-phone" hint="Layout responsivo" /></div>
            <div className="col-md-4"><MetricCard label="SPA" value={0} icon="bi-code-slash" hint="React Router sem reload" /></div>
          </div>
        </div>
      </div>
    </section>
  );
}
