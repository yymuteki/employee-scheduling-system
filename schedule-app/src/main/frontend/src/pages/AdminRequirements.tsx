import { useState, useEffect } from 'react'
import client from '../api/client'
import type { User } from '../App'

interface ReqItem {
  id: number
  userId: number
  userName: string
  yearMonth: string
  naturalInput: string | null
  parsedUnavailable: string
  parsedPreferences: string
  parsedNotes: string | null
}

export default function AdminRequirements({ user, onLogout }: { user: User; onLogout: () => void }) {
  const defaultMonth = new Date().getMonth() + 1 >= 12 ? `${new Date().getFullYear() + 1}-01` : `${new Date().getFullYear()}-${String(new Date().getMonth() + 2).padStart(2, '0')}`
  const [yearMonth, setYearMonth] = useState(defaultMonth)
  const [reqs, setReqs] = useState<ReqItem[]>([])
  const [loading, setLoading] = useState(false)
  const [allEmployees, setAllEmployees] = useState<{ name: string }[]>([])

  useEffect(() => {
    setLoading(true)
    Promise.all([
      client.get('/admin/requirements', { params: { yearMonth } }),
      client.get('/auth/me')
    ]).then(([reqRes, _meRes]) => {
      setReqs(reqRes.data)
    }).catch(() => {}).finally(() => setLoading(false))
  }, [yearMonth])

  const handleLogout = async () => {
    await client.post('/auth/logout')
    onLogout()
  }

  const submittedCount = reqs.filter(r => r.naturalInput).length

  return (
    <div style={{ minHeight: '100vh', background: '#f3f4f6' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 24px', background: '#1e3a5f' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 24 }}>
          <span style={{ fontSize: 18, fontWeight: 700, color: '#fff' }}>排班系统</span>
          <span style={{ fontSize: 13, color: '#60a5fa', cursor: 'pointer', borderBottom: '2px solid #60a5fa', paddingBottom: 2 }}>员工需求</span>
          <a href="/admin/generate" style={{ fontSize: 13, color: '#94a3b8', textDecoration: 'none' }}>生成排班</a>
          <a href="/admin/schedule" style={{ fontSize: 13, color: '#94a3b8', textDecoration: 'none' }}>排班管理</a>
        </div>
        <span style={{ fontSize: 13, color: '#cbd5e1' }}>
          {user.name} &nbsp;
          <span style={{ color: '#94a3b8', cursor: 'pointer' }} onClick={handleLogout}>退出</span>
        </span>
      </div>

      <div style={{ maxWidth: 1000, margin: '24px auto', padding: '0 16px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <h2 style={{ margin: 0, fontSize: 18 }}>员工排班需求汇总</h2>
          <select value={yearMonth} onChange={e => setYearMonth(e.target.value)}
            style={{ padding: '8px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14 }}>
            {generateMonthOptions().map(m => <option key={m} value={m}>{m}</option>)}
          </select>
        </div>

        <div style={{ display: 'flex', gap: 16, marginBottom: 16 }}>
          <div style={{ background: '#eff6ff', border: '1px solid #93c5fd', borderRadius: 8, padding: '10px 18px', textAlign: 'center' }}>
            <div style={{ fontSize: 20, fontWeight: 700, color: '#1e40af' }}>{submittedCount}/{reqs.length || 15}</div>
            <div style={{ fontSize: 12, color: '#6b7280' }}>已提交 / 总人数</div>
          </div>
        </div>

        <div style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: 8, overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
            <thead>
              <tr style={{ background: '#f9fafb', borderBottom: '1px solid #e5e7eb' }}>
                <th style={{ textAlign: 'left', padding: '10px 14px', fontWeight: 600, color: '#374151' }}>员工姓名</th>
                <th style={{ textAlign: 'left', padding: '10px 14px', fontWeight: 600, color: '#374151' }}>不可上班日期</th>
                <th style={{ textAlign: 'left', padding: '10px 14px', fontWeight: 600, color: '#374151' }}>偏好班次</th>
                <th style={{ textAlign: 'left', padding: '10px 14px', fontWeight: 600, color: '#374151' }}>解析备注</th>
                <th style={{ textAlign: 'left', padding: '10px 14px', fontWeight: 600, color: '#374151' }}>原始输入</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={5} style={{ padding: 40, textAlign: 'center', color: '#9ca3af' }}>加载中...</td></tr>
              ) : reqs.length === 0 ? (
                <tr><td colSpan={5} style={{ padding: 40, textAlign: 'center', color: '#9ca3af' }}>暂无数据</td></tr>
              ) : (
                reqs.map(req => {
                  let unavailable = '无'
                  let preference = '无特别偏好'
                  try {
                    const ua = JSON.parse(req.parsedUnavailable || '[]')
                    if (ua.length > 0) unavailable = ua.join(', ')
                    const pp = JSON.parse(req.parsedPreferences || '{}')
                    if (pp.preference) preference = '偏好' + pp.preference
                  } catch {}
                  return (
                    <tr key={req.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                      <td style={{ padding: '9px 14px', color: '#374151' }}>{req.userName}</td>
                      <td style={{ padding: '9px 14px', color: unavailable === '无' ? '#9ca3af' : '#374151' }}>{unavailable}</td>
                      <td style={{ padding: '9px 14px', color: preference === '无特别偏好' ? '#9ca3af' : '#374151' }}>{preference}</td>
                      <td style={{ padding: '9px 14px', color: req.parsedNotes ? '#dc2626' : '#9ca3af', fontSize: 12, maxWidth: 150 }}>
                        {req.parsedNotes || '—'}
                      </td>
                      <td style={{ padding: '9px 14px', color: req.naturalInput ? '#6b7280' : '#9ca3af' }}>{req.naturalInput || '（空）'}</td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>
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
