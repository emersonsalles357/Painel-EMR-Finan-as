import { useState } from 'react';
import { Navbar } from '../components/Navbar';
import { CRUDTable } from '../components/CRUDTable';
import { Modal } from '../components/Modal';
import { useFinancas } from '../contexts/FinancasContext';
import { useToast } from '../contexts/ToastContext';
import { gastosService } from '../services/api';
import type { Gasto } from '../types';

const FILTERS = ['Pago', 'Pendente', 'Atrasado'];
const COLUMNS = [
  { key: 'descricao', label: 'Descrição' },
  { key: 'categoria', label: 'Categoria' },
  { key: 'valor', label: 'Valor', type: 'currency' as const },
  { key: 'status', label: 'Status', type: 'badge' as const },
  { key: 'data', label: 'Data', type: 'date' as const },
];

const CATEGORIAS = ['Tecnologia', 'Marketing', 'Operacional', 'Pessoas'];
const STATUSES = ['Pago', 'Pendente', 'Atrasado'];

export function GastosPage() {
  const { gastos, setGastos } = useFinancas();
  const { showToast } = useToast();

  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<Gasto | null>(null);
  const [deleteId, setDeleteId] = useState<string | null>(null);

  const initial = editingItem ?? { descricao: '', categoria: CATEGORIAS[0], valor: 0, status: STATUSES[0], data: new Date().toISOString().split('T')[0] };

  const handleSave = async (data: Record<string, unknown>) => {
    const payload = data as unknown as Partial<Gasto>;
    if (editingItem) {
      const normalized = { ...payload, id: editingItem.id };
      await gastosService.update(editingItem.id, normalized as Partial<Gasto>);
      showToast('Registro atualizado com sucesso.');
    } else {
      await gastosService.create(payload as Omit<Gasto, 'id'>);
      showToast('Registro criado com sucesso.');
    }
    const refreshed = await gastosService.list();
    setGastos(refreshed);
    setModalOpen(false);
    setEditingItem(null);
  };

  const handleDelete = async () => {
    if (deleteId) {
      await gastosService.remove(deleteId);
      showToast('Registro excluído com sucesso.');
      const refreshed = await gastosService.list();
      setGastos(refreshed);
      setDeleteId(null);
    }
  };

  return (
    <section className="page-enter">
      <Navbar title="Gastos" />
      <div className="main-content">
        <div className="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4">
          <div>
            <h2 className="section-title mb-1">Gastos</h2>
            <p className="text-muted-soft mb-0">Gerencie despesas, categorias, status e vencimentos.</p>
          </div>
        </div>
        <CRUDTable
          columns={COLUMNS}
          rows={gastos}
          entityName="Gastos"
          filters={FILTERS}
          onCreate={() => { setEditingItem(null); setModalOpen(true); }}
          onEdit={(row) => { setEditingItem(row); setModalOpen(true); }}
          onDelete={(id) => setDeleteId(id)}
        />
      </div>

      {modalOpen && (
        <Modal
          title={`${editingItem ? 'Editar' : 'Novo'} Gasto`}
          onClose={() => { setModalOpen(false); setEditingItem(null); }}
          onSave={handleSave}
          initialData={initial}
          fields={[
            { name: 'descricao', label: 'Descrição', placeholder: 'Ex.: Infraestrutura cloud', col: 6 },
            { name: 'categoria', label: 'Categoria', type: 'select', options: CATEGORIAS, col: 6 },
            { name: 'valor', label: 'Valor', type: 'text', kind: 'money', placeholder: 'Ex.: 350,00', col: 4 },
            { name: 'status', label: 'Status', type: 'select', options: STATUSES, col: 4 },
            { name: 'data', label: 'Data', type: 'date', col: 4 },
          ]}
        />
      )}

      {deleteId && (
        <Modal
          title="Confirmar exclusão"
          onClose={() => setDeleteId(null)}
          onSave={handleDelete}
          initialData={{}}
          fields={[]}
          isDelete
        />
      )}
    </section>
  );
}
