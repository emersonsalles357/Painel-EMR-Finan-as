import { Navbar } from '../components/Navbar';

export function NotFoundPage() {
  return (
    <section className="page-enter">
      <Navbar title="404" />
      <div className="main-content">
        <div className="glass-panel p-5 text-center">
          <div className="emr-mark mb-4" style={{ margin: '0 auto 28px' }}>404</div>
          <h2 className="section-title mb-3">Página não encontrada</h2>
          <p className="text-muted-soft mb-0">
            A página que você está tentando acessar não existe ou foi movida.
          </p>
          <a href="/dashboard" className="btn btn-primary-gradient mt-4">
            <i className="bi bi-house me-2"></i>Voltar ao Dashboard
          </a>
        </div>
      </div>
    </section>
  );
}
