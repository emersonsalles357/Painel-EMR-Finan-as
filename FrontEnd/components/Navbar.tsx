import { Link } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
export function Navbar({ title = "Dashboard" }: { title?: string }) {
  const { user } = useAuth();
  return (
    <header className="topbar">
      <div className="d-flex align-items-center gap-3">
        <button
          type="button"
          className="btn btn-light d-lg-none"
          aria-label="Abrir menu"
          aria-controls="sidebar"
          onClick={() => window.dispatchEvent(new Event("sidebar-toggle"))}
        >
          <i className="bi bi-list fs-4" />
        </button>
        <div>
          <span className="eyebrow">EMR FINANÇAS</span>
          <h1 className="h6 mb-0">{title}</h1>
        </div>
      </div>
      <Link to="/perfil" className="profile-link">
        <span className="profile-avatar">{user?.name?.[0]?.toUpperCase()}</span>
        <span>
          <strong>{user?.name}</strong>
          <small>
            Minha conta <i className="bi bi-chevron-down" />
          </small>
        </span>
      </Link>
    </header>
  );
}
