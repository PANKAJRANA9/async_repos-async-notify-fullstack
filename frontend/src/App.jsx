import { useState, useEffect, useRef } from 'react'

const API = ''
const fmtDate = (iso) => {
  if(!iso) return '-'
  try{
    const d = new Date(iso)
    return d.toLocaleString('en-IN', { day:'2-digit', month:'short', year:'numeric', hour:'2-digit', minute:'2-digit', second:'2-digit', hour12:true })
  }catch{ return iso }
}
const shortKey = (k) => k ? (k.length > 22 ? k.slice(0,22)+'…' : k) : '-'

export default function App(){
  const [form, setForm] = useState({ userId:'user-123', channel:'EMAIL', template:'WELCOME', idempotencyKey:'evt-'+Date.now() })
  const [logs, setLogs] = useState([])
  const [dlq, setDlq] = useState([])
  const [stats, setStats] = useState({ total:0, successRate:0, queued:0, sent:0, retry:0, dedup:0 })
  const [tab, setTab] = useState('events') // events | dlq
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [channelFilter, setChannelFilter] = useState('ALL')
  // GAP FIX 1: No alert() - inline banner + toast
  const [duplicateBanner, setDuplicateBanner] = useState(null) // {key, message, dedupCount, at}
  const [toast, setToast] = useState(null) // {type, msg}
  const [exporting, setExporting] = useState(false)
  const [loading, setLoading] = useState(false)
  const toastTimer = useRef(null)

  const showToast = (type, msg) => {
    setToast({type, msg})
    clearTimeout(toastTimer.current)
    toastTimer.current = setTimeout(()=> setToast(null), 4000)
  }

  const load = async () => {
    try{
      const [lRes, sRes, dRes] = await Promise.all([
        fetch(`${API}/api/v1/events/logs`).then(r=>r.json()).catch(()=>[]),
        fetch(`${API}/api/v1/notifications/stats`).then(r=>r.json()).catch(()=>null),
        fetch(`${API}/api/v1/report/dlq`).then(r=>r.json()).catch(()=>[]),
      ])
      if(Array.isArray(lRes)) setLogs(lRes)
      if(sRes) setStats({ total: sRes.total ?? lRes.length, successRate: sRes.successRate ?? 0, queued: sRes.queued ?? 0, sent: sRes.sent ?? 0, retry: sRes.retry ?? sRes.failed ?? 0, dedup: sRes.dedup ?? 0 })
      if(Array.isArray(dRes)) setDlq(dRes)
    }catch(e){ console.error(e) }
  }
  useEffect(()=>{ load(); const t=setInterval(load, 3000); return ()=>{ clearInterval(t); clearTimeout(toastTimer.current) } }, [])

  const newKey = () => setForm(f => ({...f, idempotencyKey:'evt-'+Date.now()+Math.floor(Math.random()*1000)}))
  const copyKey = async () => { try{ await navigator.clipboard.writeText(form.idempotencyKey); showToast('success','Idempotency Key copied') }catch{ showToast('error','Copy failed') } }

  // GAP FIX 1: sendEvent - NO alert(JSON.stringify)
  const sendEvent = async (e) => {
    e.preventDefault()
    if(!form.idempotencyKey?.trim()){ showToast('error','Idempotency Key is required'); return }
    setLoading(true); setDuplicateBanner(null)
    try{
      const res = await fetch(`${API}/api/v1/events`, { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(form) })
      const data = await res.json()
      console.log('SEND RESPONSE:', data)
      if(data.status === 'DEDUPLICATED'){
        // INLINE BANNER, not alert()
        setDuplicateBanner({ key: data.idempotencyKey, message: data.message || 'Already processed - spam prevented', dedupCount: data.dedupCount ?? stats.dedup, at: new Date() })
        showToast('duplicate', 'Duplicate blocked - see yellow banner')
        // Do NOT auto-change key - user needs to see SAME key proof. New Key button is manual.
      } else if(data.status === 'QUEUED' || data.status === 'SENT'){
        showToast('success', `Event queued - ${data.channel} - Key: ${shortKey(data.idempotencyKey)}`)
        newKey()
      } else {
        showToast('error', data.message || JSON.stringify(data))
      }
      await load()
    }catch(err){ showToast('error','Backend not reachable - http://localhost:8080 up? ' + err.message) }
    finally{ setLoading(false) }
  }

  // Demo: same key twice
  const demoDuplicate = async () => {
    const key = 'evt-'+Date.now()
    const payload = { userId:'demo-user', channel:'EMAIL', template:'WELCOME', idempotencyKey: key }
    setForm(f=>({...f, idempotencyKey:key}))
    showToast('info','Demo: sending same key twice...')
    await fetch(`${API}/api/v1/events`, {method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload)})
    await new Promise(r=>setTimeout(r, 300))
    const res2 = await fetch(`${API}/api/v1/events`, {method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload)})
    const data2 = await res2.json()
    if(data2.status === 'DEDUPLICATED'){
      setDuplicateBanner({ key, message:'Demo Proof: 2nd send with SAME key was BLOCKED', dedupCount: data2.dedupCount ?? stats.dedup, at: new Date() })
      showToast('duplicate','Demo duplicate blocked - banner shown')
    }
    await load()
  }

  // GAP FIX 2: Export - MUST download CSV, never open UI tab
  const exportCsv = async () => {
    setExporting(true)
    try{
      const res = await fetch(`${API}/api/v1/report/export`, { headers:{ 'Accept':'text/csv' } })
      const contentType = res.headers.get('content-type') || ''
      const text = await res.clone().text()
      // If proxy returned HTML (SPA fallback), it is NOT CSV
      if(!res.ok || text.trimStart().startsWith('<!DOCTYPE') || text.trimStart().startsWith('<html') || contentType.includes('text/html')){
        showToast('error','Export failed: Got HTML instead of CSV - Nginx/Vite proxy for /api/ is broken. Use Docker rebuild or check nginx.conf')
        return
      }
      const blob = await res.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `async-notify-report-${new Date().toISOString().slice(0,10)}.csv`
      document.body.appendChild(a); a.click(); a.remove()
      window.URL.revokeObjectURL(url)
      showToast('success','CSV exported - check Downloads')
    }catch(err){ showToast('error','Export failed: ' + err.message) }
    finally{ setExporting(false) }
  }

  const replayDlq = async (id) => {
    try{
      const res = await fetch(`${API}/api/v1/report/dlq/${id}/retry`, {method:'POST'})
      const data = await res.json()
      showToast('success', `DLQ ${id} replayed - ${data.status || 'RETRY_QUEUED'}`)
      await load()
    }catch(e){ showToast('error','Replay failed: '+e.message) }
  }

  // GAP FIX 3: Search + Filter + Formatted dates
  const filteredLogs = logs.filter(l => {
    const q = search.toLowerCase()
    const matchSearch = !q || (l.idempotencyKey||'').toLowerCase().includes(q) || (l.userId||'').toLowerCase().includes(q) || String(l.id||'').includes(q)
    const matchStatus = statusFilter==='ALL' || l.status===statusFilter
    const matchChannel = channelFilter==='ALL' || l.channel===channelFilter
    return matchSearch && matchStatus && matchChannel
  })

  const card = (label, val, sub, color) => (
    <div style={{background:'#1e2a3a', borderRadius:12, padding:16, borderLeft:`4px solid ${color}`}}>
      <div style={{fontSize:11, letterSpacing:1, color:'#8a9bb0', fontWeight:700}}>{label}</div>
      <div style={{fontSize:32, fontWeight:800, color:'#fff', lineHeight:1.2}}>{val}</div>
      <div style={{fontSize:11, color:'#8a9bb0'}}>{sub}</div>
    </div>
  )

  return (
    <div style={{background:'#0f172a', minHeight:'100vh', color:'#e2e8f0', fontFamily:'Inter,system-ui', padding:20}}>
      <div style={{maxWidth:1200, margin:'0 auto'}}>
        <div style={{display:'flex', justifyContent:'space-between', alignItems:'center', flexWrap:'wrap', gap:12}}>
          <h1 style={{margin:0, fontSize:26}}>AsyncNotify <span style={{color:'#34d399', fontSize:13, verticalAlign:'middle'}}>● PRODUCTION</span></h1>
          <div style={{display:'flex', gap:10}}>
            <button onClick={exportCsv} disabled={exporting} style={{padding:'10px 16px', background:'#2563eb', color:'#fff', border:'none', borderRadius:8, cursor:'pointer', fontWeight:700}}>{exporting?'Exporting...':'📥 Export CSV Report'}</button>
            <button onClick={sendEvent} disabled={loading} style={{padding:'10px 16px', background:'#16a34a', color:'#fff', border:'none', borderRadius:8, cursor:'pointer', fontWeight:700}}>{loading?'Sending...':'Fire Event'}</button>
          </div>
        </div>

        {/* TOAST - replaces alert() */}
        {toast && (
          <div style={{position:'fixed', top:16, right:16, zIndex:9999, padding:'12px 16px', borderRadius:10, background: toast.type==='success' ? '#14532d' : toast.type==='duplicate' ? '#713f12' : toast.type==='info' ? '#1e3a5f' : '#7f1d1d', color:'#fff', boxShadow:'0 8px 24px rgba(0,0,0,.4)', maxWidth:380, fontWeight:600}}>
            {toast.type==='duplicate' ? '⚠️ ' : toast.type==='success' ? '✅ ' : toast.type==='info' ? 'ℹ️ ' : '❌ '}{toast.msg}
            <button onClick={()=>setToast(null)} style={{marginLeft:10, background:'transparent', border:'none', color:'#fff', cursor:'pointer'}}>✕</button>
          </div>
        )}

        {/* STATS - GAP: Dedup now shows rate, not just count */}
        <div style={{display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(150px,1fr))', gap:12, margin:'20px 0'}}>
          {card('TOTAL', stats.total, 'MySQL + Redis REAL', '#64748b')}
          {card('SUCCESS RATE', `${stats.successRate}%`, 'MySQL + Redis REAL', '#34d399')}
          {card('QUEUED', stats.queued, 'MySQL + Redis', '#60a5fa')}
          {card('SENT', stats.sent, 'MySQL status=SENT', '#22c55e')}
          {card('RETRY', stats.retry, 'MySQL + Redis REAL', '#f59e0b')}
          {card('DEDUP', stats.dedup, `Redis KEYS idem:* • ${stats.total+stats.dedup>0 ? Math.round(stats.dedup/(stats.total+stats.dedup)*100) : 0}% blocked`, '#eab308')}
        </div>

        {/* GAP FIX 1: DUPLICATE BANNER - INLINE, NOT alert() */}
        {duplicateBanner && (
          <div style={{background:'#fef3c7', color:'#78350f', border:'2px solid #f59e0b', borderRadius:12, padding:16, marginBottom:16}}>
            <div style={{display:'flex', justifyContent:'space-between', gap:12, flexWrap:'wrap'}}>
              <div>
                <div style={{fontWeight:800, fontSize:17}}>⚠️ DUPLICATE EVENT BLOCKED — Spam Prevented</div>
                <div style={{marginTop:6}}>{duplicateBanner.message}</div>
                <div style={{marginTop:8, fontFamily:'monospace', fontSize:13}}>IdempotencyKey: <b>{duplicateBanner.key}</b></div>
                <div style={{fontSize:13}}>Dedup Count (Redis): <b>{duplicateBanner.dedupCount}</b> • Blocked at: {duplicateBanner.at.toLocaleString('en-IN')}</div>
                <div style={{fontSize:12, color:'#92400e', marginTop:6}}>Proof: Same key was already processed. Event NOT queued again. Click <b>New Key</b> to send a fresh event.</div>
              </div>
              <div style={{display:'flex', gap:8, alignItems:'start'}}>
                <button onClick={()=>{ setDuplicateBanner(null); newKey() }} style={{padding:'8px 12px', background:'#f59e0b', border:'none', borderRadius:8, cursor:'pointer', fontWeight:700}}>New Key →</button>
                <button onClick={()=>setDuplicateBanner(null)} style={{padding:'8px 12px', background:'#fff', border:'1px solid #f59e0b', borderRadius:8, cursor:'pointer'}}>Dismiss ✕</button>
              </div>
            </div>
          </div>
        )}

        {/* IDEMPOTENCY FORM */}
        <form onSubmit={sendEvent} style={{background:'#1e2a3a', borderRadius:12, padding:16}}>
          <div style={{fontWeight:700, marginBottom:10}}>Idempotency Key — Same key twice = DEDUPLICATED (PROOF)</div>
          <div style={{display:'flex', gap:10, flexWrap:'wrap'}}>
            <input value={form.idempotencyKey} onChange={e=>setForm({...form, idempotencyKey:e.target.value})} style={{flex:1, minWidth:260, padding:12, background:'#0f172a', color:'#fff', border:'1px solid #334155', borderRadius:8}} placeholder="evt-..." />
            <button type="button" onClick={copyKey} style={{padding:'10px 14px', background:'#334155', color:'#fff', border:'none', borderRadius:8, cursor:'pointer'}}>Copy</button>
            <button type="button" onClick={newKey} style={{padding:'10px 14px', background:'#0ea5e9', color:'#fff', border:'none', borderRadius:8, cursor:'pointer', fontWeight:700}}>New Key</button>
            <button type="button" onClick={demoDuplicate} style={{padding:'10px 14px', background:'#eab308', color:'#000', border:'none', borderRadius:8, cursor:'pointer', fontWeight:700}}>🧪 Demo Duplicate (Same Key ×2)</button>
          </div>
          <div style={{fontSize:12, color:'#94a3b8', marginTop:8}}>Backoff: 1s → 2s → 4s → 8s → 16s + jitter | Max 5 → DLQ | Polling every 3s from REAL backend</div>
          <div style={{display:'grid', gridTemplateColumns:'repeat(auto-fit,minmax(140px,1fr))', gap:10, marginTop:12}}>
            <input value={form.userId} onChange={e=>setForm({...form, userId:e.target.value})} placeholder="User ID" style={{padding:10, background:'#0f172a', color:'#fff', border:'1px solid #334155', borderRadius:8}} />
            <select value={form.channel} onChange={e=>setForm({...form, channel:e.target.value})} style={{padding:10, background:'#0f172a', color:'#fff', border:'1px solid #334155', borderRadius:8}}><option>EMAIL</option><option>PUSH</option><option>SMS</option></select>
            <input value={form.template} onChange={e=>setForm({...form, template:e.target.value})} placeholder="Template" style={{padding:10, background:'#0f172a', color:'#fff', border:'1px solid #334155', borderRadius:8}} />
          </div>
        </form>

        {/* TABS + FILTERS - GAP: Search/Filter added */}
        <div style={{display:'flex', gap:10, flexWrap:'wrap', alignItems:'center', margin:'20px 0 12px'}}>
          <button onClick={()=>setTab('events')} style={{padding:'8px 14px', borderRadius:8, border:'none', cursor:'pointer', background: tab==='events' ? '#16a34a' : '#1e2a3a', color:'#fff', fontWeight:700}}>Events Log ({filteredLogs.length})</button>
          <button onClick={()=>setTab('dlq')} style={{padding:'8px 14px', borderRadius:8, border:'none', cursor:'pointer', background: tab==='dlq' ? '#dc2626' : '#1e2a3a', color:'#fff', fontWeight:700}}>DLQ View ({dlq.length}) — Replay Proof</button>
          <input value={search} onChange={e=>setSearch(e.target.value)} placeholder="🔍 Search by Key / User / ID..." style={{padding:10, background:'#1e2a3a', color:'#fff', border:'1px solid #334155', borderRadius:8, flex:1, minWidth:200}} />
          <select value={statusFilter} onChange={e=>setStatusFilter(e.target.value)} style={{padding:10, background:'#1e2a3a', color:'#fff', border:'1px solid #334155', borderRadius:8}}><option value="ALL">All Status</option><option>SENT</option><option>QUEUED</option><option>FAILED</option></select>
          <select value={channelFilter} onChange={e=>setChannelFilter(e.target.value)} style={{padding:10, background:'#1e2a3a', color:'#fff', border:'1px solid #334155', borderRadius:8}}><option value="ALL">All Channels</option><option>EMAIL</option><option>PUSH</option><option>SMS</option></select>
        </div>

        {tab==='events' ? (
          <div style={{background:'#1e2a3a', borderRadius:12, overflow:'hidden'}}>
            <table style={{width:'100%', borderCollapse:'collapse'}}>
              <thead><tr style={{background:'#0f172a', textAlign:'left'}}><th style={{padding:12}}>ID</th><th>Key</th><th>Channel</th><th>Status</th><th>Attempts</th><th>Created</th></tr></thead>
              <tbody>
                {filteredLogs.map(l=>(
                  <tr key={l.id} style={{borderTop:'1px solid #334155'}}>
                    <td style={{padding:12}}>{l.id}</td>
                    <td style={{fontFamily:'monospace', fontSize:12}} title={l.idempotencyKey}>{shortKey(l.idempotencyKey)}</td>
                    <td>{l.channel}</td>
                    <td><span style={{padding:'4px 10px', borderRadius:12, fontSize:12, fontWeight:700, background: l.status==='SENT' ? '#14532d' : l.status==='QUEUED' ? '#1e3a5f' : '#7f1d1d'}}>{l.status}</span></td>
                    <td>{l.attempts}</td>
                    <td style={{fontSize:13}}>{fmtDate(l.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {filteredLogs.length===0 && <div style={{padding:20, color:'#94a3b8', textAlign:'center'}}>No events match filter — try clearing search / filter, or Fire Event.</div>}
          </div>
        ) : (
          <div style={{background:'#1e2a3a', borderRadius:12, padding:16}}>
            <div style={{fontWeight:700, marginBottom:8}}>DLQ — Dead Letter Queue • Replay Proof</div>
            {dlq.length===0 ? (
              <div style={{color:'#94a3b8', fontSize:14}}>DLQ is empty (good). To test Replay: Make consumer fail 5 times (SMTP timeout) → message moves to DLQ → Replay button appears here → Replay re-publishes with audit (replayedAt, replayedBy).</div>
            ) : dlq.map(m=>(
              <div key={m.id} style={{display:'flex', justifyContent:'space-between', gap:12, padding:12, border:'1px solid #334155', borderRadius:8, marginBottom:8, flexWrap:'wrap'}}>
                <div><div style={{fontFamily:'monospace', fontSize:13}}>#{m.id} • {shortKey(m.idempotencyKey)}</div><div style={{fontSize:12, color:'#94a3b8'}}>{m.error || m.payload?.slice(0,120)}</div><div style={{fontSize:12}}>{m.replayedAt ? `✅ Replayed at ${fmtDate(m.replayedAt)} by ${m.replayedBy || 'admin'}` : '⏳ Not yet replayed'}</div></div>
                <button onClick={()=>replayDlq(m.id)} style={{padding:'8px 14px', background:'#dc2626', color:'#fff', border:'none', borderRadius:8, cursor:'pointer', fontWeight:700}}>↻ Replay</button>
              </div>
            ))}
          </div>
        )}

        <div style={{textAlign:'center', color:'#64748b', fontSize:12, marginTop:24}}>Production Ready by PANKAJRANA9 • 11yr FSD & Architect • MySQL + Redis + RabbitMQ • At-least-once + Idempotent = Exactly-once</div>
      </div>
    </div>
  )
}
