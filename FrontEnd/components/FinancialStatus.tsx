import { useFinancas } from "../contexts/FinancasContext";
export function FinancialStatus() {
  const { loading, error, loadAll } = useFinancas();
  return loading ? (
    <p role="status" className="text-muted-soft">
      Carregando registros…
    </p>
  ) : error ? (
    <div className="alert alert-danger" role="alert">
      {error}{" "}
      <button className="btn btn-light" onClick={() => void loadAll()}>
        Tentar novamente
      </button>
    </div>
  ) : null;
}
