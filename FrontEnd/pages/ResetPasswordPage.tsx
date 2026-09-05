import { useMemo, useRef, useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import axios from 'axios';
import { authService } from '../services/api';
import { useToast } from '../contexts/ToastContext';

type FieldErrors = Partial<Record<'novaSenha' | 'confirmarNovaSenha' | 'form', string>>;

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';
  const navigate = useNavigate();
  const { showToast } = useToast();
  const submittingRef = useRef(false);

  const [novaSenha, setNovaSenha] = useState('');
  const [confirmarNovaSenha, setConfirmarNovaSenha] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<FieldErrors>({});

  const requirements = useMemo(() => [
    { label: '10 ou mais caracteres', valid: novaSenha.length >= 10 },
    { label: 'letra maiúscula', valid: /[A-ZÀ-ÖØ-Þ]/.test(novaSenha) },
    { label: 'letra minúscula', valid: /[a-zà-öø-ÿ]/.test(novaSenha) },
    { label: 'número', valid: /\d/.test(novaSenha) },
    { label: 'caractere especial', valid: /[^\p{L}\p{N}\s]/u.test(novaSenha) },
  ], [novaSenha]);

  const validate = () => {
    const next: FieldErrors = {};
    if (!requirements.every((r) => r.valid)) {
      next.novaSenha = 'A senha ainda não atende a todos os requisitos.';
    }
    if (novaSenha !== confirmarNovaSenha) {
      next.confirmarNovaSenha = 'As senhas não coincidem.';
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (submittingRef.current || !validate()) return;
    if (!token.trim()) {
      setErrors({ form: 'Token de recuperação inválido ou ausente.' });
      return;
    }

    submittingRef.current = true;
    setLoading(true);
    setErrors({});

    try {
      await authService.resetPassword(token.trim(), novaSenha);
      showToast('Senha redefinida com sucesso. Faça login com a nova senha.');
      navigate('/login', { replace: true });
    } catch (error) {
      const backendMessage = axios.isAxiosError(error) ? error.response?.data?.message : undefined;
      const message = backendMessage || 'Não foi possível redefinir a senha. O link pode ter expirado ou já ter sido utilizado.';
      setErrors({ form: message });
      showToast(message, 'danger');
    } finally {
      submittingRef.current = false;
      setLoading(false);
    }
  };

  if (!token.trim()) {
    return (
      <main className="auth-layout page-enter">
        <section className="auth-card text-center">
          <div className="emr-mark">EMR</div>
          <h1 className="section-title h4 mb-2">Link inválido</h1>
          <div className="alert alert-warning text-start mb-4" role="alert">
            <i className="bi bi-exclamation-triangle-fill me-2"></i>
            O link de recuperação de senha é inválido ou não contém um token válido.
          </div>
          <p className="text-muted-soft mb-4">
            Por favor, solicite um novo link de recuperação de senha.
          </p>
          <Link to="/esqueci-senha" className="btn btn-primary-gradient w-100 py-2">
            Solicitar nova recuperação
          </Link>
          <p className="auth-switch mb-0 mt-3">
            <Link to="/login">Voltar ao login</Link>
          </p>
        </section>
      </main>
    );
  }

  return (
    <main className="auth-layout page-enter">
      <section className="auth-card auth-card-register">
        <div className="text-center mb-4">
          <div className="emr-mark">EMR</div>
          <h1 className="section-title h4 mb-2">Redefinir sua senha</h1>
          <p className="text-muted-soft mb-0">Crie uma nova senha segura para sua conta.</p>
        </div>

        <form onSubmit={handleSubmit} noValidate>
          <label htmlFor="novaSenha" className="form-label fw-semibold">Nova senha</label>
          <div className="input-icon password-input mb-1">
            <i className="bi bi-lock"></i>
            <input
              id="novaSenha"
              type={showPassword ? 'text' : 'password'}
              autoComplete="new-password"
              value={novaSenha}
              onChange={(e) => setNovaSenha(e.target.value)}
              className={`form-control ${errors.novaSenha ? 'is-invalid' : ''}`}
              maxLength={72}
              disabled={loading}
            />
            <button
              type="button"
              className="password-toggle"
              onClick={() => setShowPassword((v) => !v)}
              aria-label={showPassword ? 'Ocultar nova senha' : 'Mostrar nova senha'}
            >
              <span className={`bi ${showPassword ? 'bi-eye-slash' : 'bi-eye'}`}></span>
            </button>
          </div>
          {errors.novaSenha && <span className="invalid-feedback-live">{errors.novaSenha}</span>}

          <ul className="password-requirements" aria-label="Requisitos da senha">
            {requirements.map((req) => (
              <li key={req.label} className={req.valid ? 'is-valid' : ''}>
                <i className={`bi ${req.valid ? 'bi-check-circle-fill' : 'bi-circle'}`}></i>
                {req.label}
              </li>
            ))}
          </ul>

          <label htmlFor="confirmarNovaSenha" className="form-label fw-semibold mt-2">Confirmar nova senha</label>
          <div className="input-icon password-input mb-1">
            <i className="bi bi-lock"></i>
            <input
              id="confirmarNovaSenha"
              type={showConfirmation ? 'text' : 'password'}
              autoComplete="new-password"
              value={confirmarNovaSenha}
              onChange={(e) => setConfirmarNovaSenha(e.target.value)}
              className={`form-control ${errors.confirmarNovaSenha ? 'is-invalid' : ''}`}
              maxLength={72}
              disabled={loading}
            />
            <button
              type="button"
              className="password-toggle"
              onClick={() => setShowConfirmation((v) => !v)}
              aria-label={showConfirmation ? 'Ocultar confirmação de senha' : 'Mostrar confirmação de senha'}
            >
              <span className={`bi ${showConfirmation ? 'bi-eye-slash' : 'bi-eye'}`}></span>
            </button>
          </div>
          {errors.confirmarNovaSenha && <span className="invalid-feedback-live">{errors.confirmarNovaSenha}</span>}

          {errors.form && (
            <div className="alert alert-danger py-2 mt-3 mb-0" role="alert">
              {errors.form}
            </div>
          )}

          <button type="submit" className="btn btn-primary-gradient w-100 mt-3 py-2" disabled={loading}>
            {loading ? (
              <>
                <span className="spinner-border spinner-border-sm me-2"></span>
                Redefinindo senha...
              </>
            ) : (
              <>
                Redefinir senha <i className="bi bi-check-lg ms-2"></i>
              </>
            )}
          </button>

          <p className="auth-switch mb-0 mt-3">
            Lembrou a senha antiga? <Link to="/login">Voltar ao login</Link>
          </p>
        </form>
      </section>
    </main>
  );
}
