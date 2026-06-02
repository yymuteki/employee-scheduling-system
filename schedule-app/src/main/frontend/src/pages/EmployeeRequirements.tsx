import { useState, useEffect } from 'react'
import client from '../api/client'
import type { User } from '../App'

export default function EmployeeRequirements({ user, onLogout }: { user: User; onLogout: () => void }) {
  const defaultMonth = new Date().getMonth() + 1 >= 12 ? `${new Date().getFullYear() + 1}-01` : `${new Date().getFullYear()}-${String(new Date().getMonth() + 2).padStart(2, '0')}`
  const [yearMonth, setYearMonth] = useState(defaultMonth)
  const [input, setInput] = useState('')
  const [saved, setSaved] = useState(false)
  const [savedText, setSavedText] = useState('')
  const [editing, setEditing] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState('')

  useEffect(() => {
    client.get('/requirements', { params: { yearMonth } }).then(res => {
      if (res.data.found) {
        setSavedText(res.data.data.naturalInput || '')
        setSaved(!!res.data.data.naturalInput)
      } else {
        setSavedText('')
        setSaved(false)
      }
    }).catch(() => {})
  }, [yearMonth])

  const handleSubmit = async () => {
    setSubmitting(true)
    setMessage('')
    try {
      await client.post('/requirements', { yearMonth, naturalInput: input })
      setSavedText(input)
      setSaved(true)
      setEditing(false)
    } catch {
      setMessage('提交失败，请重试')
    } finally {
      setSubmitting(false)
    }
  }

  const handleEdit = () => {
    setInput(savedText)
    setEditing(true)
    setSaved(false)
  }

  const handleLogout = async () => {
    await client.post('/auth/logout')
    onLogout()
  }

  return (
    <div style={{ minHeight: '100vh', background: '#f3f4f6' }}>
      {/* Nav */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 24px', background: '#1e3a5f' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 24 }}>
          <span style={{ fontSize: 18, fontWeight: 700, color: '#fff' }}>排班系统</span>
          <span style={{ fontSize: 13, color: '#60a5fa', cursor: 'pointer', borderBottom: '2px solid #60a5fa', paddingBottom: 2 }}>我的需求</span>
          <a href="/schedule" style={{ fontSize: 13, color: '#94a3b8', textDecoration: 'none' }}>排班表</a>
        </div>
        <span style={{ fontSize: 13, color: '#cbd5e1' }}>
          {user.name} (员工) &nbsp;
          <span style={{ color: '#94a3b8', cursor: 'pointer' }} onClick={handleLogout}>退出</span>
        </span>
      </div>

      <div style={{ maxWidth: 600, margin: '32px auto', padding: '0 16px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <h2 style={{ margin: 0, fontSize: 18 }}>我的排班需求</h2>
          <select value={yearMonth} onChange={e => { setYearMonth(e.target.value); setInput(''); setEditing(false) }}
            style={{ padding: '8px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14 }}>
            {generateMonthOptions().map(m => <option key={m} value={m}>{m}</option>)}
          </select>
        </div>

        {!editing && saved && (
          <>
            <div style={{ background: '#f0fdf4', border: '1px solid #86efac', borderRadius: 8, padding: '10px 16px', marginBottom: 16, display: 'flex', alignItems: 'center', gap: 8 }}>
              <span>✅</span>
              <span style={{ fontSize: 13, color: '#166534' }}>需求已提交</span>
            </div>
            <div style={{ background: '#f9fafb', border: '1px solid #e5e7eb', borderRadius: 8, padding: 16 }}>
              <div style={{ fontSize: 12, color: '#9ca3af', marginBottom: 8 }}>你提交的内容：</div>
              <div style={{ fontSize: 14, color: '#374151', lineHeight: 1.6 }}>{savedText}</div>
            </div>
            <button onClick={handleEdit}
              style={{ marginTop: 12, padding: '10px 24px', background: '#fff', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14, cursor: 'pointer' }}>
              ✏️ 编辑
            </button>
          </>
        )}

        {(!saved || editing) && (
          <>
            <textarea
              value={input}
              onChange={e => setInput(e.target.value)}
              placeholder="描述你的下月排班需求，比如：我2月3号有事情不能上班，更希望上白班"
              rows={4}
              style={{ width: '100%', padding: 12, border: '1px solid #d1d5db', borderRadius: 8, fontSize: 14, resize: 'vertical', boxSizing: 'border-box', fontFamily: 'inherit' }}
            />
            {message && (
              <div style={{ marginTop: 8, padding: '8px 14px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: 6, color: '#dc2626', fontSize: 13 }}>
                {message}
              </div>
            )}
            <button onClick={handleSubmit} disabled={submitting || !input.trim()}
              style={{ marginTop: 12, padding: '10px 28px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: 6, fontSize: 14, cursor: 'pointer', opacity: submitting || !input.trim() ? 0.6 : 1 }}>
              {submitting ? '提交中...' : '提交'}
            </button>
            {editing && (
              <button onClick={() => { setEditing(false); setSaved(true) }}
                style={{ marginLeft: 8, padding: '10px 20px', background: '#fff', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14, cursor: 'pointer' }}>
                取消
              </button>
            )}
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
