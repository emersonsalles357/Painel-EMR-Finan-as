import { useState } from 'react';
import { Navbar } from '../components/Navbar';
import { CRUDTable } from '../components/CRUDTable';
import { Modal } from '../components/Modal';
import { useFinancas } from '../contexts/FinancasContext';
import { useToast } from '../contexts/ToastContext';
import { investimentosService } from '../services/api';
import type { Investimento } from '../types';

const FILTERS = ['Ativo', 'Resgatado', 'Pendente'];
const COLUMNS = [
  { key: 'ativo', label: 'Ativo' },
  { key: 'tipo', label: 'Tipo' },
  { key: 'valor', label: 'Valor', type: 'currency' as const },
  { key: 'rentabilidade', label: 'Rentabilidade', type: 'percent' as const },
  { key: 'status', label: 'Status', type: 'badge' as const },
];

const TIPOS = ['Renda Fixa', 'Renda Variável', 'Cripto', 'Fundo'];
const STATUSES = ['Ativo', 'Resgatado', 'Pendente'];

export function InvestimentosPage() {
  const { investimentos, setInvestimentos } = useFinancas();
  const { showToast } = useToast();

  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<Investimento | null>(null);
  const [deleteId, setDeleteId] = useState<string | null>(null);

  const initial = editingItem ?? { ativo: '', tipo: TIPOS[0], valor: 0, rentabilidade: 0, status: STATUSES[0], data: new Date().toISOString().split('T')[0] };

  const handleSave = async (data: Record<string, unknown>) => {
    const payload = data as unknown as Partial<Investimento>;
    if (editingItem) {
      await investimentosService.update(editingItem.id, payload);
      showToast('Registro atualizado com sucesso.');
    } else {
      await investimentosService.create(payload as Omit<Investimento, 'id'>);
      showToast('Registro criado com sucesso.');
    }
    const refreshed = await investimentosService.list();
    setInvestimentos(refreshed);
    setModalOpen(false);
    setEditingItem(null);
  };

  const handleDelete = async () => {
    if (deleteId) {
      await investimentosService.remove(deleteId);
      showToast('Registro excluído com sucesso.');
      const refreshed = await investimentosService.list();
      setInvestimentos(refreshed);
      setDeleteId(null);
    }
  };

  return (
    <section className="page-enter">
      <Navbar title="Investimentos" />
      <div className="main-content">
        <div className="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4">
          <div>
            <h2 className="section-title mb-1">Investimentos</h2>
            <p className="text-muted-soft mb-0">Acompanhe ativos, alocação e rentabilidade.</p>
          </div>
        </div>
        <CRUDTable
          columns={COLUMNS}
          rows={investimentos}
          entityName="Investimentos"
          filters={FILTERS}
          onCreate={() => { setEditingItem(null); setModalOpen(true); }}
          onEdit={(row) => { setEditingItem(row); setModalOpen(true); }}
          onDelete={(id) => setDeleteId(id)}
        />
      </div>

      {modalOpen && (
        <Modal
          title={`${editingItem ? 'Editar' : 'Novo'} Investimento`}
          onClose={() => { setModalOpen(false); setEditingItem(null); }}
          onSave={handleSave}
          initialData={initial}
          fields={[
            { name: 'ativo', label: 'Ativo', placeholder: 'Ex.: Tesouro Selic', col: 6 },
            { name: 'tipo', label: 'Tipo', type: 'select', options: TIPOS, col: 6 },
            { name: 'valor', label: 'Valor', type: 'text', kind: 'money', placeholder: 'Ex.: 5000,00'},
            { name: 'rentabilidade', label: 'Rentabilidade (%)', type: 'text', kind: 'number', placeholder: 'Ex.: 1,25', col: 4 },
            { name: 'status', label: 'Status', type: 'select', options: STATUSES, col: 4 },
            { name: 'data', label: 'Data', type: 'date', col: 12 },
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
