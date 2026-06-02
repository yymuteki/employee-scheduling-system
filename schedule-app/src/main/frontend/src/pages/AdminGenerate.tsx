import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import client from '../api/client'
import type { User } from '../App'

export default function AdminGenerate({ user, onLogout }: { user: User; onLogout: () => void }) {
  const defaultMonth = new Date().getMonth() + 1 >= 12 ? `${new Date().getFullYear() + 1}-01` : `${new Date().getFullYear()}-${String(new Date().getMonth() + 2).padStart(2, '0')}`
  const [yearMonth, setYearMonth] = useState(defaultMonth)
  const [generating, setGenerating] = useState(false)
  const [success, setSuccess] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const handleGenerate = async () => {
    setGenerating(true)
    setError('')
    try {
      await client.post('/admin/generate', { yearMonth })
      setSuccess(true)
    } catch (err: any) {
      setError(err.response?.data?.error || '生成失败，请重试')
    } finally {
      setGenerating(false)
    }
  }

  const handleLogout = async () => {
    await client.post('/auth/logout')
    onLogout()
  }

  return (
    <div style={{ minHeight: '100vh', background: '#f3f4f6' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 24px', background: '#1e3a5f' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 24 }}>
          <span style={{ fontSize: 18, fontWeight: 700, color: '#fff' }}>排班系统</span>
          <a href="/admin/requirements" style={{ fontSize: 13, color: '#94a3b8', textDecoration: 'none' }}>员工需求</a>
          <span style={{ fontSize: 13, color: '#60a5fa', cursor: 'pointer', borderBottom: '2px solid #60a5fa', paddingBottom: 2 }}>生成排班</span>
          <a href="/admin/schedule" style={{ fontSize: 13, color: '#94a3b8', textDecoration: 'none' }}>排班管理</a>
        </div>
        <span style={{ fontSize: 13, color: '#cbd5e1' }}>
          管理员 (admin) &nbsp;
          <span style={{ color: '#94a3b8', cursor: 'pointer' }} onClick={handleLogout}>退出</span>
        </span>
      </div>

      <div style={{ maxWidth: 600, margin: '32px auto', padding: '0 16px' }}>
        <h2 style={{ fontSize: 18, marginBottom: 8 }}>AI 生成排班</h2>
        <p style={{ fontSize: 13, color: '#6b7280', margin: '0 0 16px 0' }}>基于员工需求和5条规则，自动生成下月排班表</p>

        <div style={{ marginBottom: 16 }}>
          <label style={{ fontSize: 13, color: '#374151', marginRight: 8 }}>排班月份：</label>
          <select value={yearMonth} onChange={e => setYearMonth(e.target.value)}
            style={{ padding: '8px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14 }}>
            {generateMonthOptions().map(m => <option key={m} value={m}>{m}</option>)}
          </select>
        </div>

        <div style={{ background: '#f9fafb', border: '1px solid #e5e7eb', borderRadius: 8, padding: 16, marginBottom: 16 }}>
          <div style={{ fontWeight: 600, fontSize: 14, marginBottom: 10 }}>生成规则确认</div>
          <div style={{ fontSize: 13, color: '#374151', lineHeight: 2 }}>
            <div>✅ 同一员工不会在同一天上两个班次</div>
            <div>✅ 连续工作不超过5天</div>
            <div>✅ 每个班次至少1人</div>
            <div>✅ 法定节假日不排班</div>
            <div>✅ 每人每月工作不超过20天</div>
            <div style={{ color: '#6b7280' }}>📋 优先满足员工偏好（不可上班日期、偏好班次）</div>
          </div>
        </div>

        {generating ? (
          <div style={{ textAlign: 'center', padding: '40px 20px' }}>
            <div style={{ fontSize: 36, marginBottom: 16 }}>⏳</div>
            <div style={{ fontSize: 16, fontWeight: 500, color: '#374151' }}>AI 正在生成排班...</div>
            <div style={{ fontSize: 13, color: '#9ca3af', marginTop: 8 }}>正在调用 DeepSeek 分析需求和约束</div>
            <div style={{ fontSize: 13, color: '#9ca3af' }}>预计需要 10-30 秒</div>
          </div>
        ) : success ? (
          <div style={{ textAlign: 'center', padding: '30px 20px', background: '#fff', border: '1px solid #e5e7eb', borderRadius: 8 }}>
            <div style={{ fontSize: 36, marginBottom: 12 }}>✅</div>
            <div style={{ fontSize: 16, fontWeight: 500, color: '#166534' }}>排班生成成功！</div>
            <div style={{ fontSize: 13, color: '#6b7280', marginTop: 4 }}>可前往排班管理页手动调整后发布</div>
            <button onClick={() => navigate('/admin/schedule')}
              style={{ marginTop: 16, padding: '10px 24px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: 6, fontSize: 14, cursor: 'pointer' }}>
              前往排班管理 →
            </button>
          </div>
        ) : (
          <>
            {error && (
              <div style={{ marginBottom: 16, padding: '10px 16px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: 6, color: '#dc2626', fontSize: 13 }}>
                {error}
              </div>
            )}
            <button onClick={handleGenerate}
              style={{ padding: '12px 32px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: 6, fontSize: 15, cursor: 'pointer' }}>
              🤖 开始生成排班
            </button>
          </>
        )}
      </div>
    </div>
  )
}

function generateMonthOptions(): string[] {
  const now = new Date()
  const months: string[] = []
  for (let i = -1; i < 3; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() + i, 1)
    months.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`)
  }
  return months
}
