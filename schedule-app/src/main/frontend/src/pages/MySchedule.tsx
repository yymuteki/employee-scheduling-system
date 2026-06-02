import { useState, useEffect } from 'react'
import FullCalendar from '@fullcalendar/react'
import dayGridPlugin from '@fullcalendar/daygrid'
import client from '../api/client'
import type { User } from '../App'

interface ScheduleItem {
  id: number
  userId: number
  userName: string
  date: string
  shift: string
  yearMonth: string
}

export default function MySchedule({ user, onLogout }: { user: User; onLogout: () => void }) {
  const defaultMonth = new Date().getMonth() + 1 >= 12 ? `${new Date().getFullYear() + 1}-01` : `${new Date().getFullYear()}-${String(new Date().getMonth() + 2).padStart(2, '0')}`
  const [yearMonth, setYearMonth] = useState(defaultMonth)
  const [published, setPublished] = useState(false)
  const [events, setEvents] = useState<any[]>([])
  const [myUserId, setMyUserId] = useState<number | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setLoading(true)
    client.get('/schedule', { params: { yearMonth } }).then(res => {
      setPublished(res.data.published)
      if (res.data.published) {
        setMyUserId(res.data.myUserId)
        const data: ScheduleItem[] = res.data.data
        const evts = data
          .filter((d: ScheduleItem) => d.shift !== 'OFF')
          .map((d: ScheduleItem) => ({
            title: d.userName + (d.shift === 'MORNING' ? ' 早' : ' 晚'),
            date: d.date,
            backgroundColor: d.shift === 'MORNING' ? '#22c55e' : '#3b82f6',
            borderColor: d.userId === res.data.myUserId ? '#f59e0b' : (d.shift === 'MORNING' ? '#16a34a' : '#2563eb'),
            textColor: '#fff',
            classNames: d.userId === res.data.myUserId ? ['my-shift'] : [],
          }))
        setEvents(evts)
      }
    }).catch(() => {}).finally(() => setLoading(false))
  }, [yearMonth])

  const handleLogout = async () => {
    await client.post('/auth/logout')
    onLogout()
  }

  return (
    <div style={{ minHeight: '100vh', background: '#f3f4f6' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 24px', background: '#1e3a5f' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 24 }}>
          <span style={{ fontSize: 18, fontWeight: 700, color: '#fff' }}>排班系统</span>
          <a href="/requirements" style={{ fontSize: 13, color: '#94a3b8', textDecoration: 'none' }}>我的需求</a>
          <span style={{ fontSize: 13, color: '#60a5fa', cursor: 'pointer', borderBottom: '2px solid #60a5fa', paddingBottom: 2 }}>排班表</span>
        </div>
        <span style={{ fontSize: 13, color: '#cbd5e1' }}>
          {user.name} (员工) &nbsp;
          <span style={{ color: '#94a3b8', cursor: 'pointer' }} onClick={handleLogout}>退出</span>
        </span>
      </div>

      <div style={{ maxWidth: 800, margin: '32px auto', padding: '0 16px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <h2 style={{ margin: 0, fontSize: 18 }}>我的排班表</h2>
          <select value={yearMonth} onChange={e => setYearMonth(e.target.value)}
            style={{ padding: '8px 12px', border: '1px solid #d1d5db', borderRadius: 6, fontSize: 14 }}>
            {generateMonthOptions().map(m => <option key={m} value={m}>{m}</option>)}
          </select>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: 60, color: '#9ca3af' }}>加载中...</div>
        ) : !published ? (
          <div style={{ textAlign: 'center', padding: '60px 20px', color: '#9ca3af', background: '#fff', borderRadius: 8, border: '1px solid #e5e7eb' }}>
            <div style={{ fontSize: 48, marginBottom: 12 }}>📅</div>
            <div style={{ fontSize: 16, fontWeight: 500 }}>排班尚未发布</div>
            <div style={{ fontSize: 13, marginTop: 4 }}>管理员发布后此处将显示排班表</div>
          </div>
        ) : (
          <>
            <div style={{ display: 'flex', gap: 16, marginBottom: 12, fontSize: 12, color: '#6b7280' }}>
              <span>🟢 早班</span><span>🔵 晚班</span><span>🟡 边框 = 我的班次</span>
            </div>
            <style>{`.fc .my-shift { box-shadow: 0 0 0 3px #f59e0b !important; font-weight: 700 !important; }`}</style>
            <div style={{ background: '#fff', borderRadius: 8, border: '1px solid #e5e7eb', padding: 12 }}>
              <FullCalendar
                plugins={[dayGridPlugin]}
                initialView="dayGridMonth"
                events={events}
                height="auto"
                locale="zh-cn"
                headerToolbar={{ left: 'prev', center: 'title', right: 'next' }}
                buttonText={{ today: '今天' }}
                titleFormat={{ year: 'numeric', month: 'numeric' }}
              />
            </div>
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
