import { useState } from 'react'
import client from '../api/client'
import type { User } from '../App'

export default function LoginPage({ onLogin }: { onLogin: (u: User) => void }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    try {
      const res = await client.post('/auth/login', { username, password })
      onLogin(res.data)
    } catch {
      setError('用户名或密码错误')
    }
  }

  return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: 'linear-gradient(135deg, #1e3a5f 0%, #2563eb 100%)'
    }}>
      <div style={{
        background: '#fff', borderRadius: 12, padding: '40px 36px',
        width: 380, boxShadow: '0 12px 40px rgba(0,0,0,0.2)'
      }}>
        <h1 style={{ textAlign: 'center', marginBottom: 8, fontSize: 24, color: '#1e3a5f' }}>排班系统</h1>
        <p style={{ textAlign: 'center', marginBottom: 28, fontSize: 13, color: '#9ca3af' }}>员工智能排班管理平台</p>

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 16 }}>
            <label style={{ display: 'block', marginBottom: 6, fontSize: 13, color: '#374151' }}>用户名</label>
            <input
              type="text" value={username} onChange={e => setUsername(e.target.value)}
              placeholder="请输入用户名"
              style={{ width: '100%', padding: '10px 14px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14, boxSizing: 'border-box' }}
              required
            />
          </div>
          <div style={{ marginBottom: 20 }}>
            <label style={{ display: 'block', marginBottom: 6, fontSize: 13, color: '#374151' }}>密码</label>
            <input
              type="password" value={password} onChange={e => setPassword(e.target.value)}
              placeholder="请输入密码"
              style={{ width: '100%', padding: '10px 14px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14, boxSizing: 'border-box' }}
              required
            />
          </div>
          {error && (
            <div style={{ background: '#fef2f2', border: '1px solid #fecaca', borderRadius: 6, padding: '8px 14px', marginBottom: 16, color: '#dc2626', fontSize: 13 }}>
              {error}
            </div>
          )}
          <button type="submit" style={{
            width: '100%', padding: '11px', background: '#2563eb', color: '#fff',
            border: 'none', borderRadius: 6, fontSize: 15, cursor: 'pointer', fontWeight: 500
          }}>
            登录
          </button>
        </form>

        <div style={{ marginTop: 24, padding: '12px 14px', background: '#f9fafb', borderRadius: 6, fontSize: 12, color: '#6b7280', lineHeight: 1.8 }}>
          <div><b>管理员</b>：admin / admin123</div>
          <div><b>员工</b>：emp1 / 123456（张三）等</div>
        </div>
      </div>
    </div>
  )
}
