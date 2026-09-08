import { Brand } from './Brand';
import { useEffect, useRef, useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import styles from './Sidebar.module.css';

const links = [
  { path: '/dashboard', icon: 'bi-grid-1x2', label: 'Dashboard' },
  { path: '/recebimentos', icon: 'bi-cash-coin', label: 'Recebimentos' },
  { path: '/gastos', icon: 'bi-credit-card', label: 'Gastos' },
  { path: '/investimentos', icon: 'bi-graph-up-arrow', label: 'Investimentos' },
  { path: '/perfil', icon: 'bi-person-circle', label: 'Minha conta' },
];

export function Sidebar() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const isOpenRef = useRef(false);
  const wasOpenRef = useRef(false);

  useEffect(() => {
    const toggleSidebar = () => setIsOpen((open) => !open);
    const closeSidebar = () => setIsOpen(false);
    const escape = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && isOpenRef.current) closeSidebar();
    };
    window.addEventListener('keydown', escape);

    window.addEventListener('sidebar-toggle', toggleSidebar);
    window.addEventListener('sidebar-close', closeSidebar);

    return () => {
      window.removeEventListener('keydown', escape);
      window.removeEventListener('sidebar-toggle', toggleSidebar);
      window.removeEventListener('sidebar-close', closeSidebar);
    };
  }, []);

  useEffect(() => {
    isOpenRef.current = isOpen;
    const media = window.matchMedia('(max-width: 991px)');
    const sidebar = document.getElementById('sidebar');
    const main = document.querySelector<HTMLElement>('.main-wrapper');
    const apply = () => {
      if(sidebar) sidebar.inert = media.matches && !isOpen;
      if(main) main.inert = media.matches && isOpen;
      document.querySelector('[aria-controls="sidebar"]')?.setAttribute('aria-expanded', String(isOpen));
      if(media.matches && isOpen) sidebar?.querySelector<HTMLElement>('a')?.focus();
      if(media.matches && !isOpen && wasOpenRef.current) {
        document.querySelector<HTMLButtonElement>('[aria-controls="sidebar"]')?.focus();
      }
      wasOpenRef.current = isOpen;
    };
    apply();media.addEventListener('change',apply);
    return () => { media.removeEventListener('change',apply); if(main) main.inert=false; };
  }, [isOpen]);

  const handleLogout = () => {
    logout();
    setIsOpen(false);
    navigate('/login');
  };

  const handleNavClick = () => setIsOpen(false);

  return (
    <>
      <aside
        className={`sidebar ${styles.sidebar} ${isOpen ? 'show' : ''}`}
        id="sidebar"
      >
        <div className={styles.sidebarBrand}>
          <Brand />
        </div>
        <nav aria-label="Navegação principal" className={styles.sidebarNav}>
          {links.map((link) => (
            <NavLink
              key={link.path}
              to={link.path}
              onClick={handleNavClick}
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
      <div
        className={`sidebar-backdrop ${styles.backdrop} ${isOpen ? 'show' : ''}`}
        id="sidebar-backdrop"
        onClick={() => setIsOpen(false)}
        aria-hidden="true"
      ></div>
    </>
  );
}
