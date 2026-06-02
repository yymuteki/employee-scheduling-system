import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useState, useEffect } from 'react'
import client from './api/client'
import LoginPage from './pages/LoginPage'
import EmployeeRequirements from './pages/EmployeeRequirements'
import MySchedule from './pages/MySchedule'
import AdminRequirements from './pages/AdminRequirements'
import AdminGenerate from './pages/AdminGenerate'
import AdminSchedule from './pages/AdminSchedule'

export interface User {
  id: number
  username: string
  name: string
  role: 'EMPLOYEE' | 'ADMIN'
}

function App() {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    client.get('/auth/me')
      .then(res => setUser(res.data))
      .catch(() => setUser(null))
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>加载中...</div>
  }

  if (!user) {
    return <LoginPage onLogin={setUser} />
  }

  return (
    <BrowserRouter>
      <Routes>
        {user.role === 'ADMIN' ? (
          <>
            <Route path="/admin/requirements" element={<AdminRequirements user={user} onLogout={() => setUser(null)} />} />
            <Route path="/admin/generate" element={<AdminGenerate user={user} onLogout={() => setUser(null)} />} />
            <Route path="/admin/schedule" element={<AdminSchedule user={user} onLogout={() => setUser(null)} />} />
            <Route path="*" element={<Navigate to="/admin/requirements" />} />
          </>
        ) : (
          <>
            <Route path="/requirements" element={<EmployeeRequirements user={user} onLogout={() => setUser(null)} />} />
            <Route path="/schedule" element={<MySchedule user={user} onLogout={() => setUser(null)} />} />
            <Route path="*" element={<Navigate to="/requirements" />} />
          </>
        )}
      </Routes>
    </BrowserRouter>
  )
}

export default App
