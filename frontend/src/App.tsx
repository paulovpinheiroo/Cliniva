import { Navigate, Route, Routes } from 'react-router-dom'
import { RequerAdmin } from '@/components/auth/RequerAdmin'
import { RequerAuth } from '@/components/auth/RequerAuth'
import { AppLayout } from '@/components/layout/AppLayout'
import { HomeRouter } from '@/components/layout/HomeRouter'
import { AdminPage } from '@/pages/AdminPage'
import { AdminLoginPage } from '@/pages/AdminLoginPage'
import { AgendaPage } from '@/pages/AgendaPage'
import { AtendimentosPage } from '@/pages/AtendimentosPage'
import { BookingPage } from '@/pages/BookingPage'
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
      <Route path="/" element={<HomeRouter />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/login-admin" element={<AdminLoginPage />} />
      <Route path="/cadastro" element={<CadastroPage />} />
      <Route path="/agendar/:slug" element={<BookingPage />} />
      <Route
        element={
          <RequerAuth>
            <AppLayout />
          </RequerAuth>
        }
      >
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/clientes" element={<ClientesPage />} />
        <Route path="/clientes/:id" element={<ClienteDetalhePage />} />
        <Route path="/servicos" element={<ServicosPage />} />
        <Route path="/agenda" element={<AgendaPage />} />
        <Route path="/estoque" element={<EstoquePage />} />
        <Route path="/atendimentos" element={<AtendimentosPage />} />
        <Route path="/admin" element={<RequerAdmin><AdminPage /></RequerAdmin>} />
        <Route path="/trocar-senha" element={<TrocarSenhaPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}

export default App