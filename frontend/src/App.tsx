import { useState } from "react";
import ClienteList from "./ClienteList"
function App() {

  const [cliente, setCliente] = useState([
    { id: 1, nome: "Maria Silva" },
    { id: 2, nome: "João Souza" },
    { id: 3, nome: "Ana Costa" }
  ]);
  const proximoId = cliente.length > 0 ? Math.max(...cliente.map(c => c.id)) + 1 : 1;
  const adicionarCliente = () => {
    const novoCliente = { id: proximoId, nome: "Novo Cliente" };
    setCliente([...cliente, novoCliente]);
  };
  const handleRemoverCliente = (idParaRemover: number) => {
    const listaAtualizada = cliente.filter(cliente => cliente.id !== idParaRemover);
    setCliente(listaAtualizada);
  };


  return <div style={{ padding: "20px" }}>
    <h1>Cliniva</h1>
    <section>
      <ClienteList clientes={cliente} onRemover={handleRemoverCliente} />
    </section>
    <button onClick={adicionarCliente}>Adicionar Cliente</button>
  </div>
}
export default App