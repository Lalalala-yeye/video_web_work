/**
 * 全量接口系统测试：按《交付文档/测试报告》第 4 节 59 条顺序打后端，
 * 把每条真实响应落到 postman/out/（再跑 generate-report-md.mjs 写报告）。
 *
 * 用法（项目根目录，后端 8081 已起）：
 *   node postman/run-full-report.mjs
 * Postman/Newman 同序集合：
 *   npx --yes newman@6 run postman/doinb.full.postman_collection.json -e postman/doinb.ci.postman_environment.json --working-dir .
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const ROOT = path.resolve(__dirname, '..')
const BASE = process.env.REPORT_API || 'http://127.0.0.1:8081'
const OUT = path.join(__dirname, 'out')
const VIDEO_FILE = path.join(ROOT, 'web/e2e/fixtures/test-video.mp4')
const AVATAR_FILE = path.join(ROOT, 'backend/demo-media/covers/demo-pixel.png')

const ts = Date.now()
const userA = `rpt_a_${ts}`
const userB = `rpt_b_${ts}`
const pass = '123456'
const adminUser = process.env.REPORT_ADMIN_USER || 'demo_admin'
const adminPass = process.env.REPORT_ADMIN_PASSWORD || '123456'

const results = []
const vars = {
  tokenA: '',
  tokenB: '',
  tokenAdmin: '',
  userAId: '',
  userBId: '',
  videoId: '',
  videoIdReject: '',
  videoIdDelete: '',
  liveId: '',
  commentId: '',
  roomId: '',
}

function record(id, name, method, url, input, header, expected, status, body, pass) {
  const text = typeof body === 'string' ? body : JSON.stringify(body)
  results.push({ id, name, method, url, input, header, expected, status, body: text, pass })
}

function parseBody(text) {
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

async function req(id, name, method, pathname, { json, form, file, token, expectedCode, expectedHttp, save } = {}) {
  const url = pathname.startsWith('http') ? pathname : BASE + pathname
  const headers = {}
  if (token) headers.Authorization = `Bearer ${token}`
  let body
  let inputDesc = '无'
  let headerDesc = token ? 'Authorization: Bearer <token>' : '无'
  if (json) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(json)
    inputDesc = body
  } else if (form) {
    headers['Content-Type'] = 'application/x-www-form-urlencoded'
    body = new URLSearchParams(form).toString()
    inputDesc = body
  } else if (file) {
    body = file.body
    inputDesc = file.desc
  }

  const res = await fetch(url, { method, headers, body })
  const text = await res.text()
  const parsed = parseBody(text)
  let ok = true
  if (expectedHttp != null && res.status !== expectedHttp) ok = false
  if (expectedCode != null && (typeof parsed !== 'object' || parsed.code !== expectedCode)) ok = false
  if (save) save(parsed, res)

  const expected = expectedHttp != null ? `HTTP ${expectedHttp}` : `code=${expectedCode}`
  record(id, name, method, pathname, inputDesc, headerDesc, expected, res.status, parsed, ok)
  return { res, parsed, text, ok }
}

function formDataUpload(fields, filePath, fieldName, mime) {
  const fd = new FormData()
  for (const [k, v] of Object.entries(fields)) {
    if (v != null) fd.append(k, String(v))
  }
  const buf = fs.readFileSync(filePath)
  fd.append(fieldName, new Blob([buf], { type: mime }), path.basename(filePath))
  return {
    body: fd,
    desc: `form-data: ${Object.entries(fields).map(([k, v]) => `${k}=${v}`).join(', ')}, ${fieldName}=${path.basename(filePath)}`,
  }
}

async function main() {
  fs.mkdirSync(OUT, { recursive: true })
  fs.mkdirSync(path.join(OUT, 'bodies'), { recursive: true })

  const health = await fetch(BASE + '/health')
  if (!health.ok) {
    throw new Error(`后端未就绪: ${BASE}/health -> HTTP ${health.status}`)
  }

  await req('H000', '健康检查成功', 'GET', '/health', { expectedCode: 200 })

  await req('U001', '注册失败-密码不一致', 'POST', '/user/account/register', {
    json: { username: userA, password: pass, confirmedPassword: '654321' },
    expectedCode: 403,
  })
  await req('U000', '注册成功', 'POST', '/user/account/register', {
    json: { username: userA, password: pass, confirmedPassword: pass },
    expectedCode: 200,
  })
  await req('U002', '注册失败-账号已存在', 'POST', '/user/account/register', {
    json: { username: userA, password: pass, confirmedPassword: pass },
    expectedCode: 403,
  })
  await req('预备-注册B', '注册用户B（准备）', 'POST', '/user/account/register', {
    json: { username: userB, password: pass, confirmedPassword: pass },
    expectedCode: 200,
  })
  await req('U011', '登录失败-密码错误', 'POST', '/user/account/login', {
    json: { username: userA, password: 'wrong_pass' },
    expectedCode: 403,
  })
  await req('U010', '登录成功', 'POST', '/user/account/login', {
    json: { username: userA, password: pass },
    expectedCode: 200,
    save: (j) => {
      vars.tokenA = j.data.token
      vars.userAId = String(j.data.user.id)
    },
  })
  await req('预备-登录B', '登录用户B（准备）', 'POST', '/user/account/login', {
    json: { username: userB, password: pass },
    expectedCode: 200,
    save: (j) => {
      vars.tokenB = j.data.token
      vars.userBId = String(j.data.user.id)
    },
  })
  await req('U020', '管理员登录成功', 'POST', '/admin/account/login', {
    json: { username: adminUser, password: adminPass },
    expectedCode: 200,
    save: (j) => {
      vars.tokenAdmin = j.data.token
    },
  })
  await req('U031', '未登录获取信息', 'GET', '/user/personal/info', {
    expectedHttp: 403,
  })
  await req('U030', '获取个人信息', 'GET', '/user/personal/info', {
    token: vars.tokenA,
    expectedCode: 200,
  })
  await req('U060', '公开资料', 'GET', `/user/info/get-one?uid=${vars.userAId}`, {
    expectedCode: 200,
  })
  await req('U061', '用户不存在', 'GET', '/user/info/get-one?uid=99999', {
    expectedCode: 404,
  })
  await req('U080', '修改昵称简介', 'POST', '/user/info/update', {
    form: { nickname: '测试昵称', bio: '这是简介' },
    token: vars.tokenA,
    expectedCode: 200,
  })
  await req('U081', '上传头像', 'POST', '/user/avatar/upload', {
    file: formDataUpload({}, AVATAR_FILE, 'file', 'image/png'),
    token: vars.tokenA,
    expectedCode: 200,
  })

  await req('V031', '上传失败-缺文件', 'POST', '/video/upload', {
    form: { title: '测试', visibility: 'public' },
    token: vars.tokenA,
    expectedCode: 400,
  })
  await req('V030', '上传视频', 'POST', '/video/upload', {
    file: formDataUpload(
      { title: '测试视频标题', description: '测试简介', visibility: 'public' },
      VIDEO_FILE,
      'file',
      'video/mp4'
    ),
    token: vars.tokenA,
    expectedCode: 200,
    save: (j) => {
      vars.videoId = String(j.data.id)
    },
  })
  await req('预备-上传待驳回', '上传待驳回视频（准备）', 'POST', '/video/upload', {
    file: formDataUpload(
      { title: '待驳回稿件', description: 'A011', visibility: 'public' },
      VIDEO_FILE,
      'file',
      'video/mp4'
    ),
    token: vars.tokenA,
    expectedCode: 200,
    save: (j) => {
      vars.videoIdReject = String(j.data.id)
    },
  })
  await req('预备-上传待删除', '上传待删除视频（准备）', 'POST', '/video/upload', {
    file: formDataUpload(
      { title: '待删除稿件', description: 'A040', visibility: 'public' },
      VIDEO_FILE,
      'file',
      'video/mp4'
    ),
    token: vars.tokenA,
    expectedCode: 200,
    save: (j) => {
      vars.videoIdDelete = String(j.data.id)
    },
  })
  await req('V040', '我的视频列表', 'GET', '/video/my/list?page=1&size=12', {
    token: vars.tokenA,
    expectedCode: 200,
  })
  await req('A001', '无权限访问', 'GET', '/admin/video/pending?page=1&size=10', {
    token: vars.tokenA,
    expectedCode: 403,
  })
  await req('A000', '待审列表', 'GET', '/admin/video/pending?page=1&size=10', {
    token: vars.tokenAdmin,
    expectedCode: 200,
  })
  await req('A010', '审核通过', 'POST', '/admin/video/approve', {
    form: { videoId: vars.videoId },
    token: vars.tokenAdmin,
    expectedCode: 200,
  })
  await req('A011', '审核驳回', 'POST', '/admin/video/reject', {
    form: { videoId: vars.videoIdReject },
    token: vars.tokenAdmin,
    expectedCode: 200,
  })

  await req('V000', '视频列表', 'GET', '/video/list?page=1&size=12', { expectedCode: 200 })
  await req('V010', '视频详情', 'GET', `/video/getone?id=${vars.videoId}`, { expectedCode: 200 })
  await req('V011', '视频不存在', 'GET', '/video/getone?id=99999', { expectedCode: 404 })
  await req('V020', '赞踩汇总', 'GET', `/video/reaction/summary?videoId=${vars.videoId}`, { expectedCode: 200 })
  await req('V050', '保存播放进度', 'POST', '/video/history/progress', {
    form: { videoId: vars.videoId, progress: '120' },
    token: vars.tokenA,
    expectedCode: 200,
  })
  await req('V051', '播放历史', 'GET', '/video/history/list?page=1&size=12', {
    token: vars.tokenA,
    expectedCode: 200,
  })
  await req('V060', '修改视频', 'POST', '/video/update', {
    form: { id: vars.videoId, title: '新标题', description: '新简介', visibility: 'public' },
    token: vars.tokenA,
    expectedCode: 200,
  })
  await req('预备-再审通过', '修改后再次审核通过（准备）', 'POST', '/admin/video/approve', {
    form: { videoId: vars.videoId },
    token: vars.tokenAdmin,
    expectedCode: 200,
  })
  await req('V061', '改可见性', 'POST', '/video/visibility', {
    form: { id: vars.videoId, visibility: 'private' },
    token: vars.tokenA,
    expectedCode: 200,
  })
  await req('预备-改回公开', '改回公开并审核（准备）', 'POST', '/video/visibility', {
    form: { id: vars.videoId, visibility: 'public' },
    token: vars.tokenA,
    expectedCode: 200,
  })
  await req('预备-公开再审', '公开后再次审核（准备）', 'POST', '/admin/video/approve', {
    form: { videoId: vars.videoId },
    token: vars.tokenAdmin,
    expectedCode: 200,
  })
  await req('V063', '举报视频', 'POST', '/video/report', {
    form: { id: vars.videoId, reason: '测试举报' },
    token: vars.tokenB,
    expectedCode: 200,
  })

  await req('C001', '未登录发评论', 'POST', '/comment/add', {
    form: { targetId: vars.videoId, targetType: '1', content: '测试评论' },
    expectedHttp: 403,
  })
  await req('C002', 'target 不存在', 'POST', '/comment/add', {
    form: { targetId: '99999', targetType: '1', content: 'xx' },
    token: vars.tokenA,
    expectedCode: 404,
  })
  await req('C000', '发评论-视频', 'POST', '/comment/add', {
    form: { targetId: vars.videoId, targetType: '1', content: '测试评论' },
    token: vars.tokenA,
    expectedCode: 200,
    save: (j) => {
      vars.commentId = String(j.data.id)
    },
  })
  await req('C010', '评论列表', 'GET', `/comment/list?targetId=${vars.videoId}&targetType=1&page=1&size=20`, {
    expectedCode: 200,
  })

  await req('R000', '视频点赞', 'POST', '/video/reaction', {
    form: { videoId: vars.videoId, reaction: '1' },
    token: vars.tokenB,
    expectedCode: 200,
  })
  await req('R001', '视频点踩', 'POST', '/video/reaction', {
    form: { videoId: vars.videoId, reaction: '-1' },
    token: vars.tokenA,
    expectedCode: 200,
  })
  await req('R010', '评论点赞', 'POST', '/comment/reaction', {
    form: { commentId: vars.commentId, reaction: '1' },
    token: vars.tokenB,
    expectedCode: 200,
  })

  await req('F000', '关注', 'POST', '/subscription/follow', {
    form: { targetId: vars.userBId },
    token: vars.tokenA,
    expectedCode: 200,
  })
  await req('F001', '取消关注', 'POST', '/subscription/unfollow', {
    form: { targetId: vars.userBId },
    token: vars.tokenA,
    expectedCode: 200,
  })
  await req('F010', '关注状态', 'GET', `/subscription/status?targetId=${vars.userBId}`, {
    token: vars.tokenA,
    expectedCode: 200,
  })
  await req('F030', '订阅动态', 'GET', '/subscription/feed?page=1&size=12', {
    token: vars.tokenA,
    expectedCode: 200,
  })

  await req('S000', '搜索成功', 'GET', `/search?keyword=${encodeURIComponent('测试昵称')}&videoLimit=10&liveLimit=10&userLimit=10`, {
    expectedCode: 200,
  })
  await req('S001', '搜索无结果', 'GET', '/search?keyword=xyznotexist123&videoLimit=10&liveLimit=10&userLimit=10', {
    expectedCode: 200,
  })
  await req('S002', '搜索无结果-数字关键词', 'GET', '/search?keyword=11111xyznotexist&videoLimit=10&liveLimit=10&userLimit=10', {
    expectedCode: 200,
  })

  await req('L020', '创建直播间', 'POST', '/live/create', {
    form: { title: '测试直播间' },
    token: vars.tokenA,
    expectedCode: 200,
    save: (j) => {
      vars.liveId = String(j.data.id)
    },
  })
  await req('L021', '开播', 'POST', '/live/start', {
    form: { id: vars.liveId },
    token: vars.tokenA,
    expectedCode: 200,
  })
  await req('L000', '直播列表', 'GET', '/live/list?page=1&size=12', { expectedCode: 200 })
  await req('L010', '直播详情', 'GET', `/live/getone?id=${vars.liveId}`, { expectedCode: 200 })
  await req('L041', '开播后发弹幕', 'POST', '/comment/add', {
    form: { targetId: vars.liveId, targetType: '2', content: '直播弹幕' },
    token: vars.tokenA,
    expectedCode: 200,
  })
  await req('L022', '停播', 'POST', '/live/stop', {
    form: { id: vars.liveId },
    token: vars.tokenA,
    expectedCode: 200,
  })
  await req('L040', '停播后发弹幕', 'POST', '/comment/add', {
    form: { targetId: vars.liveId, targetType: '2', content: '直播弹幕' },
    token: vars.tokenA,
    expectedCode: 400,
  })

  await req('N000', '通知列表', 'GET', '/notification/list?page=1&size=20', {
    token: vars.tokenA,
    expectedCode: 200,
  })
  await req('N010', '未读数', 'GET', '/notification/unread-count', {
    token: vars.tokenA,
    expectedCode: 200,
  })
  await req('N021', '全部已读', 'POST', '/notification/read', {
    form: {},
    token: vars.tokenA,
    expectedCode: 200,
  })

  await req('M000', '打开会话', 'POST', '/message/room/open', {
    form: { peerId: vars.userBId },
    token: vars.tokenA,
    expectedCode: 200,
    save: (j) => {
      vars.roomId = String(j.data.roomId)
    },
  })
  await req('M020', '发送消息', 'POST', '/message/send', {
    form: { roomId: vars.roomId, content: '你好，测试私信' },
    token: vars.tokenA,
    expectedCode: 200,
  })
  await req('M010', '获取消息', 'GET', `/message/room/get?roomId=${vars.roomId}&page=1&size=50`, {
    token: vars.tokenA,
    expectedCode: 200,
  })

  await req('A020', '举报复审列表', 'GET', '/admin/video/report-review?page=1&size=10', {
    token: vars.tokenAdmin,
    expectedCode: 200,
  })
  await req('A040', '管理员删视频', 'POST', '/admin/video/delete', {
    form: { videoId: vars.videoIdDelete },
    token: vars.tokenAdmin,
    expectedCode: 200,
  })
  await req('U040', '登出', 'GET', '/user/account/logout', {
    token: vars.tokenA,
    expectedCode: 200,
  })

  for (const r of results) {
    fs.writeFileSync(path.join(OUT, 'bodies', `${r.id}.json`), typeof r.body === 'string' ? r.body : JSON.stringify(r.body, null, 2), 'utf8')
  }
  fs.writeFileSync(path.join(OUT, 'results.json'), JSON.stringify({ ts, base: BASE, userA, userB, vars, results }, null, 2), 'utf8')

  const counted = results.filter((r) => !r.id.startsWith('预备'))
  const passed = counted.filter((r) => r.pass).length
  const failed = counted.filter((r) => !r.pass)
  console.log(`BASE=${BASE}`)
  console.log(`userA=${userA} id=${vars.userAId}`)
  console.log(`userB=${userB} id=${vars.userBId}`)
  console.log(`videoId=${vars.videoId} liveId=${vars.liveId} commentId=${vars.commentId} roomId=${vars.roomId}`)
  console.log(`counted=${counted.length} passed=${passed} failed=${failed.length}`)
  for (const f of failed) {
    console.log(`FAIL ${f.id} HTTP ${f.status} ${f.body}`)
  }
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
