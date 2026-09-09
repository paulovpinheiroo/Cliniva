const TOKEN_KEY = 'cliniva-token'
const CLINICA_KEY = 'cliniva-clinica'

export const sessionStore = {
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY)
  },
  setToken(token: string | null) {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token)
    } else {
      localStorage.removeItem(TOKEN_KEY)
    }
  },
  getClinicaId(): string | null {
    return localStorage.getItem(CLINICA_KEY)
  },
  setClinicaId(id: string | null) {
    if (id) {
      localStorage.setItem(CLINICA_KEY, id)
    } else {
      localStorage.removeItem(CLINICA_KEY)
    }
  },
}