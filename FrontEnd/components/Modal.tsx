import { useState, type FormEvent } from 'react';
import { toNumber } from '../utils';

interface Field {
  name: string;
  label: string;
  type?: string;
  placeholder?: string;
  options?: string[];
  col?: number;
  kind?: string;
}

interface ModalProps {
  title: string;
  onClose: () => void;
  onSave: (data: Record<string, unknown>) => Promise<void>;
  initialData: Record<string, unknown>;
  fields: Field[];
  isDelete?: boolean;
}

export function Modal({ title, onClose, onSave, initialData, fields, isDelete }: ModalProps) {
  const [data, setData] = useState<Record<string, unknown>>(
    Object.fromEntries(Object.entries(initialData).filter(([, v]) => v !== undefined))
  );
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const handleChange = (name: string, value: string) => {
    setData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) setErrors((prev) => ({ ...prev, [name]: '' }));
  };

  const validate = () => {
    const e: Record<string, string> = {};
    fields.filter((f) => f.type !== 'date').forEach((f) => {
      if (!String(data[f.name] || '').trim()) e[f.name] = 'Campo obrigatório.';
    });
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!isDelete && !validate()) return;
    setSaving(true);
    try {
      const payload: Record<string, unknown> = {};
      fields.forEach((f) => {
        if (f.kind === 'money' || f.kind === 'number') {
          payload[f.name] = toNumber(String(data[f.name] || '0'));
        } else {
          payload[f.name] = data[f.name];
        }
      });
      await onSave(payload);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }} onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal-dialog modal-dialog-centered modal-lg">
        <div className="modal-content rounded-4 shadow">
          <div className="modal-header pb-0">
            <h5 className="modal-title fw-bold">{title}</h5>
            <button type="button" className="btn-close" onClick={onClose}></button>
          </div>
          {isDelete ? (
            <form onSubmit={handleSubmit}>
              <div className="modal-body">
                <p className="mb-0 text-muted-soft">Esta ação removerá o registro selecionado. Deseja continuar?</p>
              </div>
              <div className="modal-footer pt-0">
                <button type="button" className="btn btn-light" onClick={onClose}>Cancelar</button>
                <button type="submit" className="btn btn-danger" disabled={saving}>{saving ? 'Excluindo...' : 'Excluir'}</button>
              </div>
            </form>
          ) : (
            <form onSubmit={handleSubmit}>
              <div className="modal-body">
                <div className="row g-3">
                  {fields.map((field) => (
                    <div key={field.name} className={`col-md-${field.col || 6}`}>
                      <label className="form-label fw-semibold">{field.label}</label>
                      {field.type === 'select' ? (
                        <select
                          className={`form-select ${errors[field.name] ? 'is-invalid' : ''}`}
                          value={String(data[field.name] || '')}
                          onChange={(e) => handleChange(field.name, e.target.value)}
                        >
                          <option value="">Selecione</option>
                          {field.options?.map((opt) => <option key={opt}>{opt}</option>)}
                        </select>
                      ) : (
                        <input
                          type={field.type || 'text'}
                          className={`form-control ${errors[field.name] ? 'is-invalid' : ''}`}
                          value={String(data[field.name] || '')}
                          placeholder={field.placeholder || ''}
                          onChange={(e) => handleChange(field.name, e.target.value)}
                        />
                      )}
                      {errors[field.name] && <span className="invalid-feedback-live">{errors[field.name]}</span>}
                    </div>
                  ))}
                </div>
              </div>
              <div className="modal-footer pt-0">
                <button type="button" className="btn btn-light" onClick={onClose}>Cancelar</button>
                <button type="submit" className="btn btn-primary-gradient rounded-3" disabled={saving}>
                  {saving ? <><span className="spinner-border spinner-border-sm me-2"></span>Salvando...</> : 'Salvar'}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
