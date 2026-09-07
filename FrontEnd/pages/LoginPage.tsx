import { Brand } from '../components/Brand';
import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useToast } from '../contexts/ToastContext';

export function LoginPage() {
  const { login, loading } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    if (location.state?.registrationSuccess) {
      showToast('Conta criada com sucesso. Faça seu login.');
      navigate(location.pathname, { replace: true, state: null });
    }
  }, [location.pathname, location.state, navigate, showToast]);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({});

  const validate = () => {
    const e: typeof errors = {};
    if (!email.trim()) e.email = 'Campo obrigatório.';
    if (!password.trim()) e.password = 'Campo obrigatório.';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!validate()) return;
    try {
      await login(email, password);
      showToast('Login realizado com sucesso.');
      navigate('/dashboard');
    } catch {
      showToast('Falha ao realizar login.', 'danger');
    }
  };

  return (
    <main className="auth-layout page-enter">
      <section className="auth-card">
        <div className="text-center mb-4">
          <Brand />
          <h1 className="section-title h4 mb-2">Acesse sua conta</h1>
          <p className="text-muted-soft mb-0">Painel financeiro premium do EMR Finanças.</p>
        </div>
        <form onSubmit={handleSubmit} noValidate>
          <label htmlFor="login-email" className="form-label fw-semibold">E-mail</label>
          <div className="input-icon mb-1">
            <i className="bi bi-envelope"></i>
            <input id="login-email" autoComplete="email" name="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} className={`form-control ${errors.email ? 'is-invalid' : ''}`} data-required />
          </div>
          {errors.email && <span className="invalid-feedback-live">{errors.email}</span>}

          <div className="d-flex justify-content-between align-items-center mt-2">
            <label htmlFor="login-password" className="form-label fw-semibold mb-0">Senha</label>
            <Link to="/esqueci-senha" className="small text-decoration-none text-muted-soft">
              Esqueci minha senha
            </Link>
          </div>
          <div className="input-icon mb-1">
            <i className="bi bi-lock"></i>
            <input id="login-password" autoComplete="current-password" name="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} className={`form-control ${errors.password ? 'is-invalid' : ''}`} data-required />
          </div>
          {errors.password && <span className="invalid-feedback-live">{errors.password}</span>}

          <button type="submit" className="btn btn-primary-gradient w-100 mt-3 py-2" disabled={loading}>
            {loading ? <><span className="spinner-border spinner-border-sm me-2"></span>Entrando...</> : <>Entrar <i className="bi bi-person-circle ms-2"></i></>}
          </button>
          <p className="auth-switch mb-0 mt-3">
            Ainda não tem uma conta? <Link to="/cadastro">Criar conta</Link>
          </p>
        </form>
      </section>
    </main>
  );
}
