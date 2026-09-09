import { Navigate, Route, Routes } from 'react-router-dom'
import { RequerAuth } from '@/components/auth/RequerAuth'
import { AppLayout } from '@/components/layout/AppLayout'
import { AtendimentosPage } from '@/pages/AtendimentosPage'
import { CadastroPage } from '@/pages/CadastroPage'
import { ClienteDetalhePage } from '@/pages/ClienteDetalhePage'
import { ClientesPage } from '@/pages/ClientesPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { EstoquePage } from '@/pages/EstoquePage'
import { LoginPage } from '@/pages/LoginPage'
import { ServicosPage } from '@/pages/ServicosPage'
import { TrocarSenhaPage } from '@/pages/TrocarSenhaPage'

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/cadastro" element={<CadastroPage />} />
      <Route
        element={
          <RequerAuth>
            <AppLayout />
          </RequerAuth>
        }
      >
        <Route path="/" element={<DashboardPage />} />
        <Route path="/clientes" element={<ClientesPage />} />
        <Route path="/clientes/:id" element={<ClienteDetalhePage />} />
        <Route path="/servicos" element={<ServicosPage />} />
        <Route path="/estoque" element={<EstoquePage />} />
        <Route path="/atendimentos" element={<AtendimentosPage />} />
        <Route path="/trocar-senha" element={<TrocarSenhaPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}

export default App