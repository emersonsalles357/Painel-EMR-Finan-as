import { useToast } from '../contexts/ToastContext';

export function ToastContainer() {
  const { toasts, removeToast } = useToast();
  return (
    <div className="toast-container position-fixed top-0 end-0 p-3" style={{ zIndex: 9999 }}>
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={`toast align-items-center text-bg-${toast.type} border-0 show`}
          role="alert"
          aria-live="assertive"
          aria-atomic="true"
          onClick={() => removeToast(toast.id)}
        >
          <div className="d-flex">
            <div className="toast-body">{toast.message}</div>
            <button type="button" className="btn-close btn-close-white me-2 m-auto" aria-label="Fechar"></button>
          </div>
        </div>
      ))}
    </div>
  );
}
