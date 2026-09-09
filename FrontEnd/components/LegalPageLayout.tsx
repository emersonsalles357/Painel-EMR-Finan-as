import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Brand } from './Brand';
import { PublicLegalFooter } from './PublicLegal';

type LegalPageLayoutProps = {
  title: string;
  alternatePath: string;
  alternateLabel: string;
  children: ReactNode;
};

export function LegalPageLayout({ title, alternatePath, alternateLabel, children }: LegalPageLayoutProps) {
  return (
    <main className="legal-page page-enter">
      <div className="legal-shell">
        <header className="legal-header">
          <Link to="/login" className="legal-brand-link" aria-label="Ir para o login do EMR Finanças">
            <Brand />
          </Link>
          <Link to="/login" className="legal-back-link">
            <i className="bi bi-arrow-left" aria-hidden="true"></i> Voltar ao login
          </Link>
        </header>

        <article className="legal-document">
          <div className="legal-title-block">
            <span className="legal-badge">Projeto demonstrativo</span>
            <h1>{title}</h1>
            <p className="legal-version">Versão 1.0 — Setembro de 2026</p>
          </div>

          <div className="legal-content">{children}</div>

          <p className="legal-cross-link">
            Consulte também: <Link to={alternatePath}>{alternateLabel}</Link>.
          </p>
        </article>

        <PublicLegalFooter />
      </div>
    </main>
  );
}
