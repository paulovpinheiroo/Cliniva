interface Cliente {
    id: number;
    nome: string;
}
interface ClienteListProps {
    clientes: Cliente[];
    onRemover: (id: number) => void;
}

export default function ClienteList({ clientes, onRemover }: ClienteListProps) {
    return (
        <ul>
            {clientes.map((cliente) => (
                <li key={cliente.id} style={{ marginBottom: "8px" }}>
                    {cliente.nome} {" "}
                    {/* O onClick chama uma função anônima que executa o onRemover passando o ID */}
                    <button onClick={() => onRemover(cliente.id)}>
                        Remover
                    </button>
                </li>
            ))}
        </ul>
    );
}
