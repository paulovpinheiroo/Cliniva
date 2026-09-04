import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from '@/components/layout/AppLayout'
import { AtendimentosPage } from '@/pages/AtendimentosPage'
import { ClientesPage } from '@/pages/ClientesPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { EstoquePage } from '@/pages/EstoquePage'
import { ServicosPage } from '@/pages/ServicosPage'

function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/clientes" element={<ClientesPage />} />
        <Route path="/servicos" element={<ServicosPage />} />
        <Route path="/estoque" element={<EstoquePage />} />
        <Route path="/atendimentos" element={<AtendimentosPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}

export default App