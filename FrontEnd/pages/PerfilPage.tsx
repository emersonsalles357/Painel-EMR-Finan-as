import { useEffect, useState, type FormEvent } from "react";
import axios from "axios";
import { Navbar } from "../components/Navbar";
import { useAuth } from "../contexts/AuthContext";
import { realApi } from "../services/realApi";
import { passwordRequirements } from "../utils/password";
export function PerfilPage() {
  const { user, updateUser } = useAuth();
  const [nome, setNome] = useState("");
  const [atual, setAtual] = useState("");
  const [nova, setNova] = useState("");
  const [confirmar, setConfirmar] = useState("");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  useEffect(() => {
    if (user) setNome(user.name);
  }, [user]);
  const requirements = passwordRequirements(nova);
  const submit = async (e: FormEvent, password: boolean) => {
    e.preventDefault();
    if (busy) return;
    setError("");
    setMessage("");
    if (
      password &&
      (!requirements.every((r) => r.valid) || nova !== confirmar)
    ) {
      setError("Confira os requisitos e a confirmação da nova senha.");
      return;
    }
    setBusy(true);
    try {
      if (password) {
        await realApi.auth.changePassword(atual, nova);
        setAtual("");
        setNova("");
        setConfirmar("");
        setMessage(
          "Senha alterada com sucesso. Use a nova senha no próximo login.",
        );
      } else {
        updateUser(await realApi.auth.updateProfile(nome.trim()));
        setMessage("Perfil atualizado com sucesso.");
      }
    } catch (e) {
      setError(
        axios.isAxiosError(e)
          ? e.response?.data?.message ||
              "Não foi possível salvar. Tente novamente."
          : "Não foi possível salvar.",
      );
    } finally {
      setBusy(false);
    }
  };
  return (
    <section>
      <Navbar title="Minha conta" />
      <main className="main-content">
        <div className="page-heading">
          <h2>Minha conta</h2>
          <p>Seus dados e a segurança do seu acesso, em um só lugar.</p>
        </div>
        {message && (
          <div className="alert alert-success" role="status">
            {message}
          </div>
        )}
        {error && (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        )}
        {!user ? (
          <p role="status">Carregando perfil…</p>
        ) : (
          <div className="profile-grid">
            <section className="chart-card">
              <div className="d-flex gap-3 align-items-center mb-4">
                <span className="profile-avatar large">
                  {user.name[0]?.toUpperCase()}
                </span>
                <div>
                  <h3 className="h5 mb-1">Informações pessoais</h3>
                  <span className="text-muted-soft">
                    {user.role === "ROLE_ADMIN"
                      ? "Conta administradora"
                      : "Conta pessoal"}
                  </span>
                </div>
              </div>
              <form onSubmit={(e) => submit(e, false)}>
                <label htmlFor="profile-name" className="form-label">
                  Nome
                </label>
                <input
                  id="profile-name"
                  className="form-control mb-3"
                  required
                  minLength={2}
                  maxLength={100}
                  autoComplete="name"
                  value={nome}
                  onChange={(e) => setNome(e.target.value)}
                />
                <label htmlFor="profile-email" className="form-label">
                  E-mail
                </label>
                <input
                  id="profile-email"
                  className="form-control"
                  value={user.email}
                  readOnly
                />
                <p className="small text-muted-soft mt-2">
                  E-mail utilizado para acessar e recuperar sua conta.
                </p>
                <button
                  className="btn btn-primary-gradient mt-2"
                  disabled={busy}
                >
                  Salvar alterações
                </button>
              </form>
            </section>
            <section className="chart-card">
              <h3 className="h5">
                <i className="bi bi-shield-lock me-2" />
                Alterar senha
              </h3>
              <p className="text-muted-soft">
                Confirme sua senha atual para criar uma nova.
              </p>
              <form onSubmit={(e) => submit(e, true)}>
                <label htmlFor="current-password" className="form-label">
                  Senha atual
                </label>
                <input
                  id="current-password"
                  className="form-control mb-3"
                  type="password"
                  autoComplete="current-password"
                  required
                  value={atual}
                  onChange={(e) => setAtual(e.target.value)}
                />
                <label htmlFor="new-password" className="form-label">
                  Nova senha
                </label>
                <input
                  id="new-password"
                  className="form-control"
                  type="password"
                  autoComplete="new-password"
                  required
                  maxLength={72}
                  value={nova}
                  onChange={(e) => setNova(e.target.value)}
                  aria-describedby="password-policy"
                />
                <ul id="password-policy" className="password-requirements mb-3">
                  {requirements.map((r) => (
                    <li key={r.label} className={r.valid ? "is-valid" : ""}>
                      <i
                        className={
                          "bi " +
                          (r.valid ? "bi-check-circle-fill" : "bi-circle")
                        }
                      />
                      {r.label}
                    </li>
                  ))}
                </ul>
                <label htmlFor="confirm-password" className="form-label">
                  Confirmar nova senha
                </label>
                <input
                  id="confirm-password"
                  className="form-control mb-3"
                  type="password"
                  autoComplete="new-password"
                  required
                  maxLength={72}
                  value={confirmar}
                  onChange={(e) => setConfirmar(e.target.value)}
                />
                <button className="btn btn-primary-gradient" disabled={busy}>
                  {busy ? "Salvando…" : "Alterar senha"}
                </button>
              </form>
            </section>
          </div>
        )}
      </main>
    </section>
  );
}
