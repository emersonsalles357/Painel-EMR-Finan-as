import { useState, useRef, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { authService } from '../services/api';
import { useToast } from '../contexts/ToastContext';

export function ForgotPasswordPage() {
  const { showToast } = useToast();
  const submittingRef = useRef(false);
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const genericMessage = 'Se existir uma conta associada a este e-mail, enviaremos as instruções de recuperação.';

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (submittingRef.current) return;

    const trimmed = email.trim();
    if (!trimmed || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) {
      setError('Informe um e-mail válido.');
      return;
    }

    submittingRef.current = true;
    setLoading(true);
    setError(null);

    try {
      await authService.forgotPassword(trimmed);
      setSubmitted(true);
      showToast(genericMessage, 'info');
    } catch {
      // Anti-enumeração e resiliência: mesmo se der erro ou rate limit, exibe resposta segura
      setSubmitted(true);
      showToast(genericMessage, 'info');
    } finally {
      submittingRef.current = false;
      setLoading(false);
    }
  };

  return (
    <main className="auth-layout page-enter">
      <section className="auth-card">
        <div className="text-center mb-4">
          <div className="emr-mark">EMR</div>
          <h1 className="section-title h4 mb-2">Recuperar senha</h1>
          <p className="text-muted-soft mb-0">Informe seu e-mail para receber o link de redefinição.</p>
        </div>

        {submitted ? (
          <div className="text-center py-2">
            <div className="alert alert-info text-start mb-4" role="alert">
              <i className="bi bi-info-circle-fill me-2"></i>
              {genericMessage}
            </div>
            <p className="text-muted-soft small mb-4">
              Verifique sua caixa de entrada e spam. O link é válido por 15 minutos e de uso único.
            </p>
            <Link to="/login" className="btn btn-primary-gradient w-100 py-2">
              Voltar ao login
            </Link>
          </div>
        ) : (
          <form onSubmit={handleSubmit} noValidate>
            <label htmlFor="email" className="form-label fw-semibold">E-mail cadastrado</label>
            <div className="input-icon mb-1">
              <i className="bi bi-envelope"></i>
              <input
                id="email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className={`form-control ${error ? 'is-invalid' : ''}`}
                placeholder="exemplo@email.com"
                maxLength={120}
                disabled={loading}
              />
            </div>
            {error && <span className="invalid-feedback-live">{error}</span>}

            <button type="submit" className="btn btn-primary-gradient w-100 mt-3 py-2" disabled={loading}>
              {loading ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2"></span>
                  Enviando instruções...
                </>
              ) : (
                <>
                  Enviar instruções <i className="bi bi-send ms-2"></i>
                </>
              )}
            </button>

            <p className="auth-switch mb-0 mt-3">
              Lembrou a senha? <Link to="/login">Voltar ao login</Link>
            </p>
          </form>
        )}
      </section>
    </main>
  );
}
