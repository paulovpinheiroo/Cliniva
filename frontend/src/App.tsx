import ClienteList from "./ClienteList"
function App() {
  const cliente = [
    { id: 1, nome: "Maria Silva" },
    { id: 2, nome: "João Souza" },
    { id: 3, nome: "Ana Costa" }
  ]
  return <div style={{ padding: "20px" }}>
    <h1>Cliniva</h1>
    <section>
      <ClienteList clientes={cliente} />
    </section>
  </div>
}
export default App