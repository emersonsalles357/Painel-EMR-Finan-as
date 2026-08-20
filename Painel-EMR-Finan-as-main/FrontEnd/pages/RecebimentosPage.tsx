import { useState } from 'react';
import { Navbar } from '../components/Navbar';
import { CRUDTable } from '../components/CRUDTable';
import { Modal } from '../components/Modal';
import { useFinancas } from '../contexts/FinancasContext';
import { useToast } from '../contexts/ToastContext';
import { recebimentosService } from '../services/api';
import type { Recebimento } from '../types';

const FILTERS = ['Recebido', 'Em aberto', 'Atrasado'];
const COLUMNS = [
  { key: 'descricao', label: 'Descrição' },
  { key: 'cliente', label: 'Cliente' },
  { key: 'valor', label: 'Valor', type: 'currency' as const },
  { key: 'status', label: 'Status', type: 'badge' as const },
  { key: 'data', label: 'Data', type: 'date' as const },
];

const CATEGORIAS = ['SaaS', 'Serviços', 'Licenciamento', 'Consultoria'];
const STATUSES = ['Recebido', 'Em aberto', 'Atrasado'];

export function RecebimentosPage() {
  const { recebimentos, setRecebimentos } = useFinancas();
  const { showToast } = useToast();

  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<Recebimento | null>(null);
  const [deleteId, setDeleteId] = useState<string | null>(null);

  const initial = editingItem ?? { descricao: '', cliente: '', categoria: CATEGORIAS[0], valor: 0, status: STATUSES[0], data: new Date().toISOString().split('T')[0] };

  const handleSave = async (data: Record<string, unknown>) => {
    const payload = data as unknown as Partial<Recebimento>;
    if (editingItem) {
      await recebimentosService.update(editingItem.id, payload);
      showToast('Registro atualizado com sucesso.');
    } else {
      await recebimentosService.create(payload as Omit<Recebimento, 'id'>);
      showToast('Registro criado com sucesso.');
    }
    const refreshed = await recebimentosService.list();
    setRecebimentos(refreshed);
    setModalOpen(false);
    setEditingItem(null);
  };

  const handleDelete = async () => {
    if (deleteId) {
      await recebimentosService.remove(deleteId);
      showToast('Registro excluído com sucesso.');
      const refreshed = await recebimentosService.list();
      setRecebimentos(refreshed);
      setDeleteId(null);
    }
  };

  return (
    <section className="page-enter">
      <Navbar title="Recebimentos" />
      <div className="main-content">
        <div className="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4">
          <div>
            <h2 className="section-title mb-1">Recebimentos</h2>
            <p className="text-muted-soft mb-0">Controle entradas financeiras e cobranças de clientes.</p>
          </div>
        </div>
        <CRUDTable
          columns={COLUMNS}
          rows={recebimentos}
          entityName="Recebimentos"
          filters={FILTERS}
          onCreate={() => { setEditingItem(null); setModalOpen(true); }}
          onEdit={(row) => { setEditingItem(row); setModalOpen(true); }}
          onDelete={(id) => setDeleteId(id)}
        />
      </div>

      {modalOpen && (
        <Modal
          title={`${editingItem ? 'Editar' : 'Novo'} Recebimento`}
          onClose={() => { setModalOpen(false); setEditingItem(null); }}
          onSave={handleSave}
          initialData={initial}
          fields={[
            { name: 'descricao', label: 'Descrição', placeholder: 'Ex.: Fatura mensal', col: 6 },
            { name: 'cliente', label: 'Cliente/Origem', placeholder: 'Ex.: Empresa ACME', col: 6 },
            { name: 'valor', label: 'Valor', type: 'text', kind: 'money', placeholder: 'Ex.: 1200,00', col: 4 },
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
