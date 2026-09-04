import { useMemo, useRef, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { authService } from '../services/api';
import { useToast } from '../contexts/ToastContext';

type FieldErrors = Partial<Record<'nome' | 'email' | 'senha' | 'confirmarSenha' | 'form', string>>;

export function RegisterPage() {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const submittingRef = useRef(false);
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [confirmarSenha, setConfirmarSenha] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<FieldErrors>({});

  const requirements = useMemo(() => [
    { label: '10 ou mais caracteres', valid: senha.length >= 10 },
    { label: 'letra maiúscula', valid: /[A-ZÀ-ÖØ-Þ]/.test(senha) },
    { label: 'letra minúscula', valid: /[a-zà-öø-ÿ]/.test(senha) },
    { label: 'número', valid: /\d/.test(senha) },
    { label: 'caractere especial', valid: /[^\p{L}\p{N}\s]/u.test(senha) },
  ], [senha]);

  const validate = () => {
    const next: FieldErrors = {};
    const trimmedName = nome.trim();
    const trimmedEmail = email.trim();
    if (trimmedName.length < 2) next.nome = 'Informe um nome com pelo menos 2 caracteres.';
    if (!trimmedEmail) next.email = 'Campo obrigatório.';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail)) next.email = 'Informe um e-mail válido.';
    if (!requirements.every((requirement) => requirement.valid)) next.senha = 'A senha ainda não atende a todos os requisitos.';
    if (senha !== confirmarSenha) next.confirmarSenha = 'As senhas não coincidem.';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (submittingRef.current || !validate()) return;
    submittingRef.current = true;
    setLoading(true);
    setErrors({});
    try {
      await authService.register({ nome: nome.trim(), email: email.trim(), senha });
      navigate('/login', { replace: true, state: { registrationSuccess: true } });
    } catch (error) {
      const status = axios.isAxiosError(error) ? error.response?.status : undefined;
      const backendMessage = axios.isAxiosError(error) ? error.response?.data?.message : undefined;
      const message = status === 409
        ? 'Este e-mail já está cadastrado.'
        : status === 400
          ? backendMessage || 'Revise os dados informados.'
          : 'Não foi possível criar a conta. Tente novamente.';
      setErrors({ form: message });
      showToast(message, 'danger');
    } finally {
      submittingRef.current = false;
      setLoading(false);
    }
  };

  return (
    <main className="auth-layout page-enter">
      <section className="auth-card auth-card-register">
        <div className="text-center mb-4">
          <div className="emr-mark">EMR</div>
          <h1 className="section-title h4 mb-2">Crie sua conta</h1>
          <p className="text-muted-soft mb-0">Comece a organizar sua vida financeira.</p>
        </div>

        <form onSubmit={handleSubmit} noValidate>
          <label htmlFor="nome" className="form-label fw-semibold">Nome</label>
          <div className="input-icon mb-1">
            <i className="bi bi-person"></i>
            <input id="nome" autoComplete="name" value={nome} onChange={(event) => setNome(event.target.value)}
              className={`form-control ${errors.nome ? 'is-invalid' : ''}`} maxLength={100} />
          </div>
          {errors.nome && <span className="invalid-feedback-live">{errors.nome}</span>}

          <label htmlFor="email" className="form-label fw-semibold mt-2">E-mail</label>
          <div className="input-icon mb-1">
            <i className="bi bi-envelope"></i>
            <input id="email" type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)}
              className={`form-control ${errors.email ? 'is-invalid' : ''}`} maxLength={120} />
          </div>
          {errors.email && <span className="invalid-feedback-live">{errors.email}</span>}

          <PasswordField id="senha" label="Senha" value={senha} visible={showPassword}
            onChange={setSenha} onToggle={() => setShowPassword((value) => !value)} error={errors.senha} />
          <ul className="password-requirements" aria-label="Requisitos da senha">
            {requirements.map((requirement) => (
              <li key={requirement.label} className={requirement.valid ? 'is-valid' : ''}>
                <i className={`bi ${requirement.valid ? 'bi-check-circle-fill' : 'bi-circle'}`}></i>
                {requirement.label}
              </li>
            ))}
          </ul>

          <PasswordField id="confirmarSenha" label="Confirmar senha" value={confirmarSenha}
            visible={showConfirmation} onChange={setConfirmarSenha}
            onToggle={() => setShowConfirmation((value) => !value)} error={errors.confirmarSenha} />

          {errors.form && <div className="alert alert-danger py-2 mt-3 mb-0" role="alert">{errors.form}</div>}
          <button type="submit" className="btn btn-primary-gradient w-100 mt-3 py-2" disabled={loading}>
            {loading ? <><span className="spinner-border spinner-border-sm me-2"></span>Criando conta...</> : <>Criar conta <i className="bi bi-arrow-right ms-2"></i></>}
          </button>
          <p className="auth-switch mb-0 mt-3">Já possui uma conta? <Link to="/login">Voltar ao login</Link></p>
        </form>
      </section>
    </main>
  );
}

function PasswordField({ id, label, value, visible, onChange, onToggle, error }: {
  id: string; label: string; value: string; visible: boolean;
  onChange: (value: string) => void; onToggle: () => void; error?: string;
}) {
  return (
    <>
      <label htmlFor={id} className="form-label fw-semibold mt-2">{label}</label>
      <div className="input-icon password-input mb-1">
        <i className="bi bi-lock"></i>
        <input id={id} type={visible ? 'text' : 'password'} autoComplete="new-password" value={value}
          onChange={(event) => onChange(event.target.value)} className={`form-control ${error ? 'is-invalid' : ''}`} maxLength={72} />
        <button type="button" className="password-toggle" onClick={onToggle}
          aria-label={visible ? `Ocultar ${label.toLowerCase()}` : `Mostrar ${label.toLowerCase()}`}>
          <span className={`bi ${visible ? 'bi-eye-slash' : 'bi-eye'}`}></span>
        </button>
      </div>
      {error && <span className="invalid-feedback-live">{error}</span>}
    </>
  );
}
