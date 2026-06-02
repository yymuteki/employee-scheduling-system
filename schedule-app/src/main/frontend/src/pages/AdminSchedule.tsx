import { useState, useEffect, useMemo } from 'react'
import client from '../api/client'
import type { User } from '../App'

interface SchedItem {
  id: number
  userId: number
  userName: string
  date: string
  shift: string
  yearMonth: string
}

interface DayRow {
  date: string
  dayOfWeek: string
  morning: SchedItem | null
  evening: SchedItem | null
}

const PAGE_SIZE = 16

export default function AdminSchedule({ user, onLogout }: { user: User; onLogout: () => void }) {
  const defaultMonth = new Date().getMonth() + 1 >= 12 ? `${new Date().getFullYear() + 1}-01` : `${new Date().getFullYear()}-${String(new Date().getMonth() + 2).padStart(2, '0')}`
  const [yearMonth, setYearMonth] = useState(defaultMonth)
  const [schedules, setSchedules] = useState<SchedItem[]>([])
  const [published, setPublished] = useState(false)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [editing, setEditing] = useState<DayRow | null>(null)
  const [editMorning, setEditMorning] = useState('')
  const [editEvening, setEditEvening] = useState('')
  const [saveError, setSaveError] = useState('')
  const [alertOpen, setAlertOpen] = useState(false)
  const [alertMsg, setAlertMsg] = useState('')

  const employees = useMemo(() => {
    const names = new Set<string>()
    schedules.forEach(s => names.add(s.userName))
    return Array.from(names)
  }, [schedules])

  const fetchData = () => {
    setLoading(true)
    client.get('/schedule/all', { params: { yearMonth } }).then(res => {
      setSchedules(res.data.data || [])
      setPublished(res.data.published)
    }).catch(() => {}).finally(() => setLoading(false))
  }

  useEffect(() => { fetchData() }, [yearMonth])

  const dayRows: DayRow[] = useMemo(() => {
    const map = new Map<string, DayRow>()
    schedules.forEach(s => {
      if (!map.has(s.date)) {
        map.set(s.date, { date: s.date, dayOfWeek: getDayOfWeek(s.date), morning: null, evening: null })
      }
      const row = map.get(s.date)!
      if (s.shift === 'MORNING') row.morning = s
      else if (s.shift === 'EVENING') row.evening = s
    })
    // Fill in missing days
    const [year, month] = yearMonth.split('-').map(Number)
    const daysInMonth = new Date(year, month, 0).getDate()
    for (let d = 1; d <= daysInMonth; d++) {
      const date = `${yearMonth}-${String(d).padStart(2, '0')}`
      if (!map.has(date)) {
        map.set(date, { date, dayOfWeek: getDayOfWeek(date), morning: null, evening: null })
      }
    }
    return Array.from(map.values()).sort((a, b) => a.date.localeCompare(b.date))
  }, [schedules, yearMonth])

  const totalPages = Math.ceil(dayRows.length / PAGE_SIZE)
  const pageRows = dayRows.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)

  const stats = useMemo(() => {
    let morningCount = 0, eveningCount = 0
    schedules.forEach(s => {
      if (s.shift === 'MORNING') morningCount++
      else if (s.shift === 'EVENING') eveningCount++
    })
    const uniqueUsers = new Set(schedules.map(s => s.userId)).size
    const avg = uniqueUsers > 0 ? Math.round((morningCount + eveningCount) / uniqueUsers * 10) / 10 : 0
    return { morningCount, eveningCount, avgWorkingDays: avg, totalEmployees: uniqueUsers || employees.length }
  }, [schedules, employees.length])

  const openEdit = (row: DayRow) => {
    setEditing(row)
    setEditMorning(row.morning?.userName || '（空）')
    setEditEvening(row.evening?.userName || '（空）')
    setSaveError('')
  }

  const handleSave = async () => {
    setSaveError('')
    if (!editing) return

    // Check same person on both shifts
    if (editMorning !== '（空）' && editMorning === editEvening) {
      const errMsg = `${editMorning}已被选为早班和晚班，同一天不能上两个班次`
      setAlertMsg(errMsg)
      setAlertOpen(true)
      return
    }

    // Update morning if changed
    if (editing.morning) {
      const newShiftName = editMorning === editing.morning.userName ? 'MORNING' :
        (editMorning === '（空）' ? 'OFF' : 'MORNING')
      if (newShiftName !== editing.morning.shift || (newShiftName === 'MORNING' && editMorning !== editing.morning.userName)) {
        try {
          await client.put(`/admin/schedule/${editing.morning.id}`, { shift: newShiftName })
        } catch (err: any) {
          const msg = err.response?.data?.error || '保存失败'
          setAlertMsg(msg)
          setAlertOpen(true)
          return
        }
      }
    }
    // Update evening if changed
    if (editing.evening) {
      const newShiftName = editEvening === editing.evening.userName ? 'EVENING' :
        (editEvening === '（空）' ? 'OFF' : 'EVENING')
      if (newShiftName !== editing.evening.shift || (newShiftName === 'EVENING' && editEvening !== editing.evening.userName)) {
        try {
          await client.put(`/admin/schedule/${editing.evening.id}`, { shift: newShiftName })
        } catch (err: any) {
          const msg = err.response?.data?.error || '保存失败'
          setAlertMsg(msg)
          setAlertOpen(true)
          return
        }
      }
    }
    setEditing(null)
    fetchData()
  }

  const handlePublish = async () => {
    if (published) {
      await client.post('/admin/unpublish', { yearMonth })
    } else {
      await client.post('/admin/publish', { yearMonth })
    }
    fetchData()
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
          <a href="/admin/generate" style={{ fontSize: 13, color: '#94a3b8', textDecoration: 'none' }}>生成排班</a>
          <span style={{ fontSize: 13, color: '#60a5fa', cursor: 'pointer', borderBottom: '2px solid #60a5fa', paddingBottom: 2 }}>排班管理</span>
        </div>
        <span style={{ fontSize: 13, color: '#cbd5e1' }}>
          管理员 (admin) &nbsp;
          <span style={{ color: '#94a3b8', cursor: 'pointer' }} onClick={handleLogout}>退出</span>
        </span>
      </div>

      <div style={{ maxWidth: 900, margin: '24px auto', padding: '0 16px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16, flexWrap: 'wrap', gap: 8 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <h2 style={{ margin: 0, fontSize: 18 }}>{yearMonth.replace('-', '年')}月 排班表</h2>
            {schedules.length > 0 && (
              <span style={{ background: published ? '#dcfce7' : '#fef3c7', color: published ? '#166534' : '#92400e', padding: '2px 10px', borderRadius: 10, fontSize: 12 }}>
                {published ? '已发布' : '草稿'}
              </span>
            )}
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <select value={yearMonth} onChange={e => { setYearMonth(e.target.value); setPage(1) }}
              style={{ padding: '8px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14 }}>
              {generateMonthOptions().map(m => <option key={m} value={m}>{m}</option>)}
            </select>
            {schedules.length > 0 && (
              <button onClick={handlePublish}
                style={{ padding: '8px 18px', background: published ? '#dc2626' : '#16a34a', color: '#fff', border: 'none', borderRadius: 6, fontSize: 13, cursor: 'pointer' }}>
                {published ? '取消发布' : '📢 发布排班'}
              </button>
            )}
          </div>
        </div>

        {schedules.length > 0 && (
          <div style={{ display: 'flex', gap: 16, marginBottom: 16 }}>
            <div style={{ background: '#f0fdf4', border: '1px solid #86efac', borderRadius: 6, padding: '8px 16px' }}>
              <span style={{ fontSize: 12, color: '#6b7280' }}>早班总人次</span><br /><span style={{ fontWeight: 700, color: '#166534' }}>{stats.morningCount}</span>
            </div>
            <div style={{ background: '#eff6ff', border: '1px solid #93c5fd', borderRadius: 6, padding: '8px 16px' }}>
              <span style={{ fontSize: 12, color: '#6b7280' }}>晚班总人次</span><br /><span style={{ fontWeight: 700, color: '#1e40af' }}>{stats.eveningCount}</span>
            </div>
            <div style={{ background: '#fefce8', border: '1px solid #fde68a', borderRadius: 6, padding: '8px 16px' }}>
              <span style={{ fontSize: 12, color: '#6b7280' }}>人均工作日</span><br /><span style={{ fontWeight: 700, color: '#a16207' }}>{stats.avgWorkingDays}</span>
            </div>
          </div>
        )}

        {loading ? (
          <div style={{ textAlign: 'center', padding: 40, color: '#9ca3af' }}>加载中...</div>
        ) : schedules.length === 0 ? (
          <div style={{ textAlign: 'center', padding: 60, color: '#9ca3af', background: '#fff', borderRadius: 8, border: '1px solid #e5e7eb' }}>
            暂无排班数据，请先生成排班
          </div>
        ) : (
          <>
            <div style={{ background: '#fff', border: '1px solid #e5e7eb', borderRadius: 8, overflow: 'hidden' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
                <thead>
                  <tr style={{ background: '#f9fafb', borderBottom: '2px solid #e5e7eb' }}>
                    <th style={{ padding: '8px 12px', fontWeight: 600, color: '#374151', textAlign: 'left' }}>日期</th>
                    <th style={{ padding: '8px 12px', fontWeight: 600, color: '#374151', textAlign: 'left' }}>星期</th>
                    <th style={{ padding: '8px 12px', fontWeight: 600, color: '#374151', textAlign: 'left' }}>早班</th>
                    <th style={{ padding: '8px 12px', fontWeight: 600, color: '#374151', textAlign: 'left' }}>晚班</th>
                    <th style={{ padding: '8px 12px', fontWeight: 600, color: '#374151', textAlign: 'center' }}>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {pageRows.map(row => (
                    <tr key={row.date} style={{ borderBottom: '1px solid #f3f4f6' }}>
                      <td style={{ padding: '8px 12px', color: '#374151' }}>{formatDate(row.date)}</td>
                      <td style={{ padding: '8px 12px', color: '#6b7280' }}>{row.dayOfWeek}</td>
                      <td style={{ padding: '8px 12px' }}>
                        {row.morning ? (
                          <span style={{ background: '#dcfce7', color: '#166534', padding: '2px 8px', borderRadius: 4 }}>{row.morning.userName}</span>
                        ) : <span style={{ color: '#9ca3af' }}>-</span>}
                      </td>
                      <td style={{ padding: '8px 12px' }}>
                        {row.evening ? (
                          <span style={{ background: '#dbeafe', color: '#1e40af', padding: '2px 8px', borderRadius: 4 }}>{row.evening.userName}</span>
                        ) : <span style={{ color: '#9ca3af' }}>-</span>}
                      </td>
                      <td style={{ padding: '8px 12px', textAlign: 'center' }}>
                        <span style={{ color: '#2563eb', cursor: 'pointer' }} onClick={() => openEdit(row)}>✏️ 编辑</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 10, fontSize: 12, color: '#6b7280' }}>
              <span>共 {dayRows.length} 天，第 {page}/{totalPages} 页</span>
              <div style={{ display: 'flex', gap: 4 }}>
                <button disabled={page <= 1} onClick={() => setPage(page - 1)}
                  style={{ padding: '4px 10px', border: '1px solid #d1d5db', borderRadius: 4, background: page <= 1 ? '#f3f4f6' : '#fff', color: page <= 1 ? '#9ca3af' : '#2563eb', cursor: page <= 1 ? 'default' : 'pointer', fontSize: 12 }}>
                  ◀ 上一页
                </button>
                {Array.from({ length: totalPages }, (_, i) => i + 1).map(p => (
                  <button key={p} onClick={() => setPage(p)}
                    style={{ padding: '4px 10px', border: `1px solid ${p === page ? '#2563eb' : '#d1d5db'}`, borderRadius: 4, background: p === page ? '#2563eb' : '#fff', color: p === page ? '#fff' : '#2563eb', cursor: 'pointer', fontSize: 12 }}>
                    {p}
                  </button>
                ))}
                <button disabled={page >= totalPages} onClick={() => setPage(page + 1)}
                  style={{ padding: '4px 10px', border: '1px solid #d1d5db', borderRadius: 4, background: page >= totalPages ? '#f3f4f6' : '#fff', color: page >= totalPages ? '#9ca3af' : '#2563eb', cursor: page >= totalPages ? 'default' : 'pointer', fontSize: 12 }}>
                  下一页 ▶
                </button>
              </div>
            </div>
          </>
        )}
      </div>

      {/* Edit Modal */}
      {editing && !alertOpen && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50 }} onClick={() => setEditing(null)}>
          <div style={{ background: '#fff', borderRadius: 12, padding: 24, width: 420, boxShadow: '0 8px 32px rgba(0,0,0,0.2)' }} onClick={e => e.stopPropagation()}>
            <div style={{ fontSize: 14, fontWeight: 600, color: '#374151', marginBottom: 16 }}>编辑排班 — {formatDate(editing.date)}（{editing.dayOfWeek}）</div>

            <div style={{ display: 'flex', gap: 24, marginBottom: 16 }}>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 12, color: '#6b7280', marginBottom: 6 }}>🌅 早班</div>
                <select value={editMorning} onChange={e => setEditMorning(e.target.value)}
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14 }}>
                  {employees.map(n => <option key={n} value={n}>{n}</option>)}
                  <option value="（空）">（空）</option>
                </select>
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 12, color: '#6b7280', marginBottom: 6 }}>🌙 晚班</div>
                <select value={editEvening} onChange={e => setEditEvening(e.target.value)}
                  style={{ width: '100%', padding: '8px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14 }}>
                  {employees.map(n => <option key={n} value={n}>{n}</option>)}
                  <option value="（空）">（空）</option>
                </select>
              </div>
            </div>

            {editMorning !== '（空）' && editMorning === editEvening && (
              <div style={{ background: '#fef3c7', border: '1px solid #fde68a', borderRadius: 6, padding: '8px 12px', marginBottom: 12, fontSize: 12, color: '#92400e' }}>
                ⚠️ {editMorning}已被选为早班和晚班，同一天不能上两个班次
              </div>
            )}

            <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
              <button onClick={() => setEditing(null)}
                style={{ padding: '8px 18px', background: '#fff', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 13, cursor: 'pointer' }}>取消</button>
              <button onClick={handleSave}
                style={{ padding: '8px 18px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: 6, fontSize: 13, cursor: 'pointer' }}>保存</button>
            </div>
          </div>
        </div>
      )}

      {/* Save Failed Alert */}
      {alertOpen && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 60 }} onClick={() => setAlertOpen(false)}>
          <div style={{ background: '#fff', borderRadius: 12, padding: '24px 32px', boxShadow: '0 4px 24px rgba(0,0,0,0.12)', minWidth: 380, maxWidth: 440 }} onClick={e => e.stopPropagation()}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
              <span style={{ fontSize: 20 }}>⚠️</span>
              <span style={{ fontSize: 16, fontWeight: 700, color: '#dc2626' }}>保存失败</span>
            </div>
            <div style={{ fontSize: 13, color: '#6b7280', marginBottom: 16 }}>
              原因为：{alertMsg}
            </div>
            <div style={{ textAlign: 'right' }}>
              <button onClick={() => setAlertOpen(false)}
                style={{ padding: '6px 20px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: 6, fontSize: 13, cursor: 'pointer' }}>
                知道了
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function formatDate(dateStr: string): string {
  const parts = dateStr.split('-')
  return `${parseInt(parts[1])}月${parseInt(parts[2])}日`
}

function getDayOfWeek(dateStr: string): string {
  const days = ['日', '一', '二', '三', '四', '五', '六']
  const d = new Date(dateStr)
  return days[d.getDay()]
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
