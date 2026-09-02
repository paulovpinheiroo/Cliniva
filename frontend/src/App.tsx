function App() {

  const cliente = [
    { id: 1, nome: "Maria Silva" },
    { id: 2, nome: "João Souza" },
    { id: 3, nome: "Ana Costa" }
  ]

  return <>
    <h1>Cliniva</h1>
    <section>
      <ul>
        {cliente.map((c) => (
          <li key={c.id}>{c.nome}</li>
        ))}
      </ul>
    </section>

  </>


}

export default App
