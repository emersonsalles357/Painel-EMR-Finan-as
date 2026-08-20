import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import styles from './Sidebar.module.css';

const links = [
  { path: '/dashboard', icon: 'bi-grid-1x2', label: 'Dashboard' },
  { path: '/recebimentos', icon: 'bi-cash-coin', label: 'Recebimentos' },
  { path: '/gastos', icon: 'bi-credit-card', label: 'Gastos' },
  { path: '/investimentos', icon: 'bi-graph-up-arrow', label: 'Investimentos' },
  { path: '/perfil', icon: 'bi-question-circle', label: 'Ajuda' },
];

export function Sidebar() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <>
      <aside className={`sidebar ${styles.sidebar}`} id="sidebar">
        <div className={styles.sidebarBrand}>
          EMR Finanças<small>Painel financeiro</small>
        </div>
        <nav className={styles.sidebarNav}>
          {links.map((link) => (
            <NavLink
              key={link.path}
              to={link.path}
              className={({ isActive }) => `${styles.sidebarLink} ${isActive ? styles.active : ''}`}
            >
              <i className={`bi ${link.icon}`}></i><span>{link.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className={styles.sidebarFooter}>
          <button className={`btn btn-sm btn-outline-danger w-100 ${styles.logoutBtn}`} onClick={handleLogout}>
            <i className="bi bi-box-arrow-right me-1"></i>Sair
          </button>
        </div>
      </aside>
      <div className={`sidebar-backdrop ${styles.backdrop}`} id="sidebar-backdrop"></div>
    </>
  );
}
