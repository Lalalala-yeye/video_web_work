#!/usr/bin/env node
/**
 * 课设第 4 项：单体 vs 微服务，同一脚本对比压测。
 * 需要 Node 18+（web 目录已经在用），无其它依赖。
 *
 *   node bench/run.mjs --label micro --base http://127.0.0.1:8081
 *   node bench/run.mjs --label mono  --base http://127.0.0.1:8081
 */

import { execFileSync } from 'node:child_process'
import { mkdirSync, writeFileSync } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = path.dirname(fileURLToPath(import.meta.url))
const OUT_DIR = path.join(ROOT, 'results', 'out')

const DEFAULTS = {
  base: 'http://127.0.0.1:8081',
  label: 'micro',
  vus: 50,
  duration: 30,
  warmup: 10,
  rounds: 3,
  scenario: 'all',
  user: 'demo_user',
  password: '123456',
  keyword: '像素',
  timeout: 10000,
  containers: '',
  dryRun: false,
}

const CONTAINERS = {
  mono: ['doinb-backend', 'doinb-mysql'],
  micro: [
    'doinb-gateway',
    'doinb-user',
    'doinb-video',
    'doinb-live',
    'doinb-interact',
    'doinb-message',
  ],
}

function parseArgs(argv) {
  const out = { ...DEFAULTS }
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i]
    if (a === '--dry-run') {
      out.dryRun = true
      continue
    }
    if (!a.startsWith('--')) continue
    const key = a.slice(2).replace(/-([a-z])/g, (_, c) => c.toUpperCase())
    const raw = argv[i + 1] && !argv[i + 1].startsWith('--') ? argv[++i] : 'true'
    if (key === 'vus' || key === 'duration' || key === 'warmup' || key === 'rounds' || key === 'timeout') {
      out[key] = Number(raw)
    } else if (key === 'dryRun') {
      out.dryRun = raw !== 'false'
    } else {
      out[key] = raw
    }
  }
  if (out.label !== 'mono' && out.label !== 'micro') {
    throw new Error('--label 只能是 mono 或 micro')
  }
  out.base = String(out.base).replace(/\/+$/, '')
  return out
}

function scenarios(opt) {
  const keyword = encodeURIComponent(opt.keyword)
  const all = [
    {
      id: 'login',
      title: '登录 POST /user/account/login',
      method: 'POST',
      path: '/user/account/login',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: opt.user, password: opt.password }),
    },
    {
      id: 'video-list',
      title: '视频列表 GET /video/list',
      method: 'GET',
      path: '/video/list?page=1&size=12',
    },
    {
      id: 'search',
      title: `搜索 GET /search?keyword=${opt.keyword}`,
      method: 'GET',
      path: `/search?keyword=${keyword}&videoLimit=10&liveLimit=10&userLimit=10`,
    },
  ]
  if (opt.scenario === 'all') return all
  const one = all.find((s) => s.id === opt.scenario)
  if (!one) throw new Error(`--scenario 只能是 all / login / video-list / search，收到 ${opt.scenario}`)
  return [one]
}

function percentile(sorted, p) {
  if (!sorted.length) return 0
  const idx = (p / 100) * (sorted.length - 1)
  const lo = Math.floor(idx)
  const hi = Math.ceil(idx)
  if (lo === hi) return sorted[lo]
  return sorted[lo] + (sorted[hi] - sorted[lo]) * (idx - lo)
}

function round(n, d = 2) {
  const f = 10 ** d
  return Math.round(n * f) / f
}

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms))
}

async function readJson(res) {
  const text = await res.text()
  try {
    return JSON.parse(text)
  } catch {
    return { _raw: text }
  }
}

async function oneRequest(opt, scenario) {
  const t0 = performance.now()
  try {
    const res = await fetch(`${opt.base}${scenario.path}`, {
      method: scenario.method,
      headers: scenario.headers,
      body: scenario.body,
      signal: AbortSignal.timeout(opt.timeout),
    })
    const json = await readJson(res)
    const ms = performance.now() - t0
    const ok = res.ok && json && json.code === 200
    return { ok, ms, http: res.status, code: json?.code, message: json?.message }
  } catch (err) {
    return { ok: false, ms: performance.now() - t0, error: err.name || 'Error', message: err.message }
  }
}

async function probe(opt, scenario) {
  const r = await oneRequest(opt, scenario)
  if (!r.ok) {
    const detail = r.message || r.error || `HTTP ${r.http} code=${r.code}`
    throw new Error(`预检失败 ${scenario.method} ${scenario.path}：${detail}`)
  }
  return r
}

async function loadOnce(opt, scenario, seconds, tag) {
  const end = Date.now() + seconds * 1000
  const latencies = []
  let ok = 0
  let fail = 0
  const errors = new Map()

  async function worker() {
    while (Date.now() < end) {
      const r = await oneRequest(opt, scenario)
      latencies.push(r.ms)
      if (r.ok) {
        ok++
      } else {
        fail++
        const key = r.error || `http=${r.http},code=${r.code}`
        errors.set(key, (errors.get(key) || 0) + 1)
      }
    }
  }

  const started = Date.now()
  await Promise.all(Array.from({ length: opt.vus }, () => worker()))
  const elapsed = (Date.now() - started) / 1000
  const sorted = latencies.slice().sort((a, b) => a - b)
  const total = ok + fail
  return {
    tag,
    scenario: scenario.id,
    vus: opt.vus,
    durationSec: seconds,
    elapsedSec: round(elapsed, 3),
    total,
    ok,
    fail,
    rps: round(total / elapsed, 2),
    successRps: round(ok / elapsed, 2),
    errorRatePct: total ? round((fail / total) * 100, 3) : 0,
    avgMs: sorted.length ? round(sorted.reduce((s, n) => s + n, 0) / sorted.length, 2) : 0,
    p50Ms: round(percentile(sorted, 50), 2),
    p95Ms: round(percentile(sorted, 95), 2),
    p99Ms: round(percentile(sorted, 99), 2),
    maxMs: sorted.length ? round(sorted[sorted.length - 1], 2) : 0,
    errors: Object.fromEntries(errors),
  }
}

function gitSha() {
  try {
    return execFileSync('git', ['rev-parse', '--short', 'HEAD'], { cwd: path.join(ROOT, '..'), encoding: 'utf8' }).trim()
  } catch {
    return ''
  }
}

function machineInfo() {
  const cpu = os.cpus()[0]
  return {
    platform: `${os.platform()} ${os.release()} ${os.arch()}`,
    cpu: cpu ? `${cpu.model} x${os.cpus().length}` : `${os.cpus().length} cores`,
    cores: os.cpus().length,
    memoryGiB: round(os.totalmem() / 1024 / 1024 / 1024, 1),
    node: process.version,
    git: gitSha(),
  }
}

function parseMemToMiB(text) {
  const used = String(text).split('/')[0].trim()
  const m = used.match(/^([\d.]+)\s*([KMGT]i?B)$/i)
  if (!m) return 0
  const n = Number(m[1])
  const u = m[2].toUpperCase()
  if (u.startsWith('K')) return n / 1024
  if (u.startsWith('G')) return n * 1024
  if (u.startsWith('T')) return n * 1024 * 1024
  return n
}

function runningContainers(wanted) {
  try {
    const raw = execFileSync('docker', ['ps', '--format', '{{.Names}}'], { encoding: 'utf8' })
    const have = new Set(raw.split(/\r?\n/).map((s) => s.trim()).filter(Boolean))
    return wanted.filter((n) => have.has(n))
  } catch {
    return []
  }
}

function sampleDocker(names) {
  if (!names.length) return []
  try {
    const fmt = '{{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}'
    const raw = execFileSync('docker', ['stats', '--no-stream', '--format', fmt, ...names], { encoding: 'utf8' })
    return raw
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter(Boolean)
      .map((line) => {
        const [name, cpu, mem] = line.split('\t')
        return {
          name,
          cpuPct: Number(String(cpu).replace('%', '')) || 0,
          memMiB: round(parseMemToMiB(mem), 1),
        }
      })
  } catch {
    return []
  }
}

function startSampler(names) {
  const samples = []
  if (!names.length) {
    return {
      samples,
      stop() {},
    }
  }
  const tick = () => {
    const row = sampleDocker(names)
    if (row.length) samples.push({ at: new Date().toISOString(), row })
  }
  tick()
  const timer = setInterval(tick, 2000)
  return {
    samples,
    stop() {
      clearInterval(timer)
      tick()
    },
  }
}

function summarizeResources(samples) {
  const byName = new Map()
  for (const s of samples) {
    for (const c of s.row) {
      if (!byName.has(c.name)) byName.set(c.name, { cpu: [], mem: [] })
      const b = byName.get(c.name)
      b.cpu.push(c.cpuPct)
      b.mem.push(c.memMiB)
    }
  }
  const containers = [...byName.entries()].map(([name, v]) => ({
    name,
    cpuAvgPct: round(v.cpu.reduce((a, b) => a + b, 0) / v.cpu.length, 2),
    cpuMaxPct: round(Math.max(...v.cpu), 2),
    memAvgMiB: round(v.mem.reduce((a, b) => a + b, 0) / v.mem.length, 1),
    memMaxMiB: round(Math.max(...v.mem), 1),
  }))
  return {
    containers,
    cpuAvgPctSum: round(containers.reduce((s, c) => s + c.cpuAvgPct, 0), 2),
    cpuMaxPctSum: round(containers.reduce((s, c) => s + c.cpuMaxPct, 0), 2),
    memAvgMiBSum: round(containers.reduce((s, c) => s + c.memAvgMiB, 0), 1),
    memMaxMiBSum: round(containers.reduce((s, c) => s + c.memMaxMiB, 0), 1),
  }
}

function markdownTable(opt, machine, rows) {
  const lines = [
    `# 压测记录 ${opt.label}  ${new Date().toISOString()}`,
    '',
    `- 目标：\`${opt.base}\``,
    `- 版本：${opt.label === 'mono' ? '改造前单体 backend' : '改造后微服务（经网关）'}`,
    `- 并发 VU：${opt.vus}，时长 ${opt.duration}s，预热 ${opt.warmup}s，每接口 ${opt.rounds} 轮`,
    `- 机器：${machine.cpu} / ${machine.memoryGiB} GiB / ${machine.platform}`,
    `- Node：${machine.node}${machine.git ? `  git：${machine.git}` : ''}`,
    '',
    '| 接口 | 轮次 | 并发 | 吞吐 req/s | 成功 req/s | 均时 ms | P50 ms | P95 ms | P99 ms | 错误率 % | CPU均%合计 | 内存均MiB合计 |',
    '|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|',
  ]
  for (const r of rows) {
    const res = r.resources || {}
    lines.push(
      `| ${r.scenario} | ${r.round} | ${r.vus} | ${r.rps} | ${r.successRps} | ${r.avgMs} | ${r.p50Ms} | ${r.p95Ms} | ${r.p99Ms} | ${r.errorRatePct} | ${res.cpuAvgPctSum ?? '-'} | ${res.memAvgMiBSum ?? '-'} |`,
    )
  }
  lines.push('')
  return lines.join('\n')
}

function printRow(r) {
  const res = r.resources
  const cpu = res && res.cpuAvgPctSum != null ? `  CPU均Σ=${res.cpuAvgPctSum}%` : ''
  const mem = res && res.memAvgMiBSum != null ? `  MEM均Σ=${res.memAvgMiBSum}MiB` : ''
  console.log(
    `  [${r.scenario} #${r.round}]  ${r.rps} req/s  均时 ${r.avgMs}ms  P95 ${r.p95Ms}ms  错误率 ${r.errorRatePct}%${cpu}${mem}`,
  )
  if (r.fail && Object.keys(r.errors).length) {
    console.log(`    失败分布: ${JSON.stringify(r.errors)}`)
  }
}

async function main() {
  const opt = parseArgs(process.argv.slice(2))
  const list = scenarios(opt)
  const machine = machineInfo()
  mkdirSync(OUT_DIR, { recursive: true })

  console.log('=== doinb 第 4 项对比压测 ===')
  console.log(`label=${opt.label}  base=${opt.base}  vus=${opt.vus}  duration=${opt.duration}s  rounds=${opt.rounds}`)
  console.log(`machine: ${machine.cpu}  mem=${machine.memoryGiB}GiB  ${machine.platform}`)
  if (opt.label === 'micro') {
    console.log('提醒：微服务侧关掉 HPA，副本固定为 1；不要同时注入故障。')
  }

  console.log('\n预检接口...')
  await probe(opt, { id: 'health', method: 'GET', path: '/health' })
  for (const s of list) await probe(opt, s)
  console.log('预检通过。')
  if (opt.dryRun) return

  const wanted = opt.containers
    ? opt.containers.split(',').map((s) => s.trim()).filter(Boolean)
    : CONTAINERS[opt.label]
  const names = runningContainers(wanted)
  if (wanted.length && !names.length) {
    console.log('未找到对应 Docker 容器，CPU/内存请事后用任务管理器或 kubectl top 补填。')
  } else if (names.length) {
    console.log(`资源采样容器: ${names.join(', ')}`)
  }

  const stamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19)
  const rows = []

  for (const scenario of list) {
    console.log(`\n--- ${scenario.title} 预热 ${opt.warmup}s ---`)
    await loadOnce(opt, scenario, opt.warmup, 'warmup')
    await sleep(2000)

    for (let i = 1; i <= opt.rounds; i++) {
      console.log(`--- ${scenario.title} 第 ${i}/${opt.rounds} 轮 ${opt.duration}s ---`)
      const sampler = startSampler(names)
      const metrics = await loadOnce(opt, scenario, opt.duration, `r${i}`)
      sampler.stop()
      const resources = sampler.samples.length ? summarizeResources(sampler.samples) : null
      const row = { ...metrics, round: i, label: opt.label, base: opt.base, resources }
      rows.push(row)
      printRow(row)
      if (i < opt.rounds) await sleep(3000)
    }
  }

  const payload = { opt, machine, generatedAt: new Date().toISOString(), rows }
  const jsonPath = path.join(OUT_DIR, `${stamp}_${opt.label}.json`)
  const mdPath = path.join(OUT_DIR, `${stamp}_${opt.label}.md`)
  writeFileSync(jsonPath, JSON.stringify(payload, null, 2), 'utf8')
  writeFileSync(mdPath, markdownTable(opt, machine, rows), 'utf8')
  console.log('\n' + markdownTable(opt, machine, rows))
  console.log(`已写入:\n  ${jsonPath}\n  ${mdPath}`)
  console.log('把上表贴进 TEMPLATE.md 对应版本。两边测完后各接口取 3 轮平均值做对比。')
}

main().catch((err) => {
  console.error(err.message || err)
  process.exit(1)
})
