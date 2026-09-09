import { Link } from 'react-router-dom';

export function DemoEnvironmentNotice() {
  return (
    <aside className="demo-notice" aria-label="Aviso sobre o ambiente demonstrativo">
      <i className="bi bi-info-circle" aria-hidden="true"></i>
      <span>
        <strong>Ambiente demonstrativo</strong> — utilize preferencialmente dados fictícios. Não informe dados
        bancários, cartões, senhas ou outras informações financeiras confidenciais.
      </span>
    </aside>
  );
}

export function PublicLegalFooter() {
  return (
    <footer className="public-legal-footer">
      <nav aria-label="Documentos jurídicos">
        <Link to="/termos-de-uso">Termos de Uso</Link>
        <span aria-hidden="true">•</span>
        <Link to="/politica-de-privacidade">Política de Privacidade</Link>
      </nav>
      <small>EMR Finanças — Projeto demonstrativo.</small>
    </footer>
  );
}
