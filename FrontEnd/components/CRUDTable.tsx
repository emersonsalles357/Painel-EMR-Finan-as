import { useState } from 'react';

interface CRUDTableProps<T extends { id: string }> {
  columns: { key: keyof T | string; label: string; type?: 'currency' | 'date' | 'badge' | 'percent' }[];
  rows: T[];
  entityName: string;
  filters?: string[];
  onCreate: () => void;
  onEdit: (row: T) => void;
  onDelete: (id: string) => void;
}

function badgeClass(value: string) {
  const normalized = value.toLowerCase();
  if (['pago', 'recebido', 'ativo'].some((item) => normalized.includes(item))) return 'badge-soft-success';
  if (['pendente', 'em aberto'].some((item) => normalized.includes(item))) return 'badge-soft-warning';
  if (['cancelado', 'atrasado'].some((item) => normalized.includes(item))) return 'badge-soft-danger';
  return 'badge-soft-info';
}

function formatCell(value: unknown, type?: string) {
  if (type === 'currency') return <strong>{new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(Number(value))}</strong>;
  if (type === 'date') return value ? new Intl.DateTimeFormat('pt-BR', { timeZone: 'UTC' }).format(new Date(value as string)) : '-';
  if (type === 'badge') return <span className={`badge rounded-pill ${badgeClass(String(value))}`}>{String(value)}</span>;
  if (type === 'percent') return <span className="fw-semibold">{Number(value).toFixed(2)}%</span>;
  return String(value || '');
}

export function CRUDTable<T extends { id: string }>({
  columns, rows, entityName, filters = [], onCreate, onEdit, onDelete,
}: CRUDTableProps<T>) {
  const [page, setPage] = useState(1);
  const [query, setQuery] = useState('');
  const [activeFilter, setActiveFilter] = useState('Todos');
  const pageSize = 6;

  const filtered = rows.filter((row) => {
    const matchesQuery = Object.values(row).join(' ').toLowerCase().includes(query.toLowerCase());
    const matchesFilter = activeFilter === 'Todos' || Object.values(row).some((v) => String(v) === activeFilter);
    return matchesQuery && matchesFilter;
  });

  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const currentPage = Math.min(page, totalPages);
  const slice = filtered.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  const handleSearch = (e: React.ChangeEvent<HTMLInputElement>) => {
    setQuery(e.target.value);
    setPage(1);
  };

  const handleFilter = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setActiveFilter(e.target.value);
    setPage(1);
  };

  return (
    <div className="modern-table-card page-enter">
      <div className="table-toolbar">
        <div>
          <h2 className="h5 fw-bold mb-1">{entityName}</h2>
          <span className="text-muted-soft small">{filtered.length} registro(s) encontrados</span>
        </div>
        <div className="d-flex gap-2 flex-wrap">
          <input className="form-control" style={{ maxWidth: '260px' }} placeholder="Buscar em tempo real..." value={query} onChange={handleSearch} />
          {filters.length > 0 && (
            <select className="form-select" style={{ maxWidth: '190px' }} value={activeFilter} onChange={handleFilter}>
              <option>Todos</option>
              {filters.map((f) => <option key={f}>{f}</option>)}
            </select>
          )}
          <button className="btn btn-primary-gradient rounded-3" onClick={onCreate}><i className="bi bi-plus-lg me-1"></i>Novo</button>
        </div>
      </div>
      <div className="table-responsive">
        <table className="table align-middle">
          <thead>
            <tr>
              {columns.map((col) => <th key={String(col.key)}>{col.label}</th>)}
              <th className="text-end">Ações</th>
            </tr>
          </thead>
          <tbody>
            {slice.length === 0 ? (
              <tr><td colSpan={columns.length + 1} className="text-center text-muted-soft py-5">Nenhum registro encontrado.</td></tr>
            ) : slice.map((row) => (
              <tr key={row.id}>
                {columns.map((col) => <td key={String(col.key)}>{formatCell(row[col.key as keyof T], col.type)}</td>)}
                <td className="text-end">
                  <button className="action-btn me-1" onClick={() => onEdit(row)} title="Editar"><i className="bi bi-pencil"></i></button>
                  <button className="action-btn text-danger" onClick={() => onDelete(row.id)} title="Excluir"><i className="bi bi-trash"></i></button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 pt-2">
        <span className="small text-muted-soft">Página {currentPage} de {totalPages}</span>
        <div className="btn-group">
          <button className="btn btn-light" disabled={currentPage === 1} onClick={() => setPage((p) => p - 1)}>Anterior</button>
          <button className="btn btn-light" disabled={currentPage === totalPages} onClick={() => setPage((p) => p + 1)}>Próxima</button>
        </div>
      </div>
    </div>
  );
}
