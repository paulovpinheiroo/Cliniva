interface Cliente {
    id: number
    nome: string
}
interface ClienteListProps {
    clientes: Cliente[];
}
export default function ClienteList({ clientes }: ClienteListProps) {
    if (!clientes || clientes.length === 0) {
        return <p>Nenhum cliente encontrado.</p>;
    }
    return (
        <ul>
            {clientes.map((cliente) => (
                <li key={cliente.id}>{cliente.nome}</li>
            ))}
        </ul>
    );
}