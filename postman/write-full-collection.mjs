/**
 * 写出可导入 Postman / Newman 的全量 59 条集合（含预备请求）。
 * 文件上传路径相对仓库根；Newman 需 --working-dir 指向仓库根。
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const out = path.join(__dirname, 'doinb.full.postman_collection.json')

function tests(lines) {
  return [
    {
      listen: 'test',
      script: { type: 'text/javascript', exec: lines },
    },
  ]
}

function jsonBody(raw) {
  return {
    mode: 'raw',
    raw,
    options: { raw: { language: 'json' } },
  }
}

function urlencoded(pairs) {
  return {
    mode: 'urlencoded',
    urlencoded: pairs.map(([key, value]) => ({ key, value, type: 'text' })),
  }
}

function formdata(items) {
  return { mode: 'formdata', formdata: items }
}

function req(name, method, urlPath, extra = {}) {
  const item = {
    name,
    event: extra.event || [],
    request: {
      method,
      header: extra.header || [],
      url: `{{baseUrl}}${urlPath}`,
    },
  }
  if (extra.body) item.request.body = extra.body
  return item
}

function authHeader() {
  return [{ key: 'Authorization', value: 'Bearer {{tokenA}}' }]
}
function authB() {
  return [{ key: 'Authorization', value: 'Bearer {{tokenB}}' }]
}
function authAdmin() {
  return [{ key: 'Authorization', value: 'Bearer {{tokenAdmin}}' }]
}

function expectCode(id, code, extra = []) {
  return tests([
    'let j;',
    'try { j = pm.response.json(); } catch (e) { j = null; }',
    `pm.test('${id} HTTP 可解析且 code=${code}', () => {`,
    '  pm.expect(j).to.be.an("object");',
    `  pm.expect(j.code).to.eql(${code});`,
    '});',
    ...extra,
  ])
}

function expectHttp(id, status) {
  return tests([
    `pm.test('${id} HTTP ${status}', () => pm.response.to.have.status(${status}));`,
  ])
}

const jsonHeader = [{ key: 'Content-Type', value: 'application/json' }]

const items = [
  req('H000 健康检查成功', 'GET', '/health', {
    event: expectCode('H000', 200),
  }),
  req('U001 注册失败-密码不一致', 'POST', '/user/account/register', {
    header: jsonHeader,
    body: jsonBody(
      '{\n  "username": "{{usernameA}}",\n  "password": "{{password}}",\n  "confirmedPassword": "654321"\n}'
    ),
    event: expectCode('U001', 403),
  }),
  req('U000 注册成功', 'POST', '/user/account/register', {
    header: jsonHeader,
    body: jsonBody(
      '{\n  "username": "{{usernameA}}",\n  "password": "{{password}}",\n  "confirmedPassword": "{{password}}"\n}'
    ),
    event: expectCode('U000', 200),
  }),
  req('U002 注册失败-账号已存在', 'POST', '/user/account/register', {
    header: jsonHeader,
    body: jsonBody(
      '{\n  "username": "{{usernameA}}",\n  "password": "{{password}}",\n  "confirmedPassword": "{{password}}"\n}'
    ),
    event: expectCode('U002', 403),
  }),
  req('预备 注册用户B', 'POST', '/user/account/register', {
    header: jsonHeader,
    body: jsonBody(
      '{\n  "username": "{{usernameB}}",\n  "password": "{{password}}",\n  "confirmedPassword": "{{password}}"\n}'
    ),
    event: expectCode('预备-注册B', 200),
  }),
  req('U011 登录失败-密码错误', 'POST', '/user/account/login', {
    header: jsonHeader,
    body: jsonBody(
      '{\n  "username": "{{usernameA}}",\n  "password": "wrong_pass"\n}'
    ),
    event: expectCode('U011', 403),
  }),
  req('U010 登录成功', 'POST', '/user/account/login', {
    header: jsonHeader,
    body: jsonBody(
      '{\n  "username": "{{usernameA}}",\n  "password": "{{password}}"\n}'
    ),
    event: expectCode('U010', 200, [
      'if (j && j.data) {',
      '  pm.collectionVariables.set("tokenA", j.data.token);',
      '  pm.collectionVariables.set("userAId", String(j.data.user.id));',
      '}',
    ]),
  }),
  req('预备 登录用户B', 'POST', '/user/account/login', {
    header: jsonHeader,
    body: jsonBody(
      '{\n  "username": "{{usernameB}}",\n  "password": "{{password}}"\n}'
    ),
    event: expectCode('预备-登录B', 200, [
      'if (j && j.data) {',
      '  pm.collectionVariables.set("tokenB", j.data.token);',
      '  pm.collectionVariables.set("userBId", String(j.data.user.id));',
      '}',
    ]),
  }),
  req('U020 管理员登录成功', 'POST', '/admin/account/login', {
    header: jsonHeader,
    body: jsonBody(
      '{\n  "username": "{{adminUser}}",\n  "password": "{{adminPassword}}"\n}'
    ),
    event: expectCode('U020', 200, [
      'if (j && j.data) { pm.collectionVariables.set("tokenAdmin", j.data.token); }',
    ]),
  }),
  req('U031 未登录获取信息', 'GET', '/user/personal/info', {
    event: expectHttp('U031', 403),
  }),
  req('U030 获取个人信息', 'GET', '/user/personal/info', {
    header: authHeader(),
    event: expectCode('U030', 200),
  }),
  req('U060 公开资料', 'GET', '/user/info/get-one?uid={{userAId}}', {
    event: expectCode('U060', 200),
  }),
  req('U061 用户不存在', 'GET', '/user/info/get-one?uid=99999', {
    event: expectCode('U061', 404),
  }),
  req('U080 修改昵称简介', 'POST', '/user/info/update', {
    header: authHeader(),
    body: urlencoded([
      ['nickname', '测试昵称'],
      ['bio', '这是简介'],
    ]),
    event: expectCode('U080', 200),
  }),
  req('U081 上传头像', 'POST', '/user/avatar/upload', {
    header: authHeader(),
    body: formdata([
      {
        key: 'file',
        type: 'file',
        src: 'backend/demo-media/covers/demo-pixel.png',
      },
    ]),
    event: expectCode('U081', 200),
  }),
  req('V031 上传失败-缺文件', 'POST', '/video/upload', {
    header: authHeader(),
    body: urlencoded([
      ['title', '测试'],
      ['visibility', 'public'],
    ]),
    event: expectCode('V031', 400),
  }),
  req('V030 上传视频', 'POST', '/video/upload', {
    header: authHeader(),
    body: formdata([
      { key: 'title', value: '测试视频标题', type: 'text' },
      { key: 'description', value: '测试简介', type: 'text' },
      { key: 'visibility', value: 'public', type: 'text' },
      { key: 'file', type: 'file', src: 'web/e2e/fixtures/test-video.mp4' },
    ]),
    event: expectCode('V030', 200, [
      'if (j && j.data) { pm.collectionVariables.set("videoId", String(j.data.id)); }',
    ]),
  }),
  req('预备 上传待驳回', 'POST', '/video/upload', {
    header: authHeader(),
    body: formdata([
      { key: 'title', value: '待驳回稿件', type: 'text' },
      { key: 'description', value: 'A011', type: 'text' },
      { key: 'visibility', value: 'public', type: 'text' },
      { key: 'file', type: 'file', src: 'web/e2e/fixtures/test-video.mp4' },
    ]),
    event: expectCode('预备-上传待驳回', 200, [
      'if (j && j.data) { pm.collectionVariables.set("videoIdReject", String(j.data.id)); }',
    ]),
  }),
  req('预备 上传待删除', 'POST', '/video/upload', {
    header: authHeader(),
    body: formdata([
      { key: 'title', value: '待删除稿件', type: 'text' },
      { key: 'description', value: 'A040', type: 'text' },
      { key: 'visibility', value: 'public', type: 'text' },
      { key: 'file', type: 'file', src: 'web/e2e/fixtures/test-video.mp4' },
    ]),
    event: expectCode('预备-上传待删除', 200, [
      'if (j && j.data) { pm.collectionVariables.set("videoIdDelete", String(j.data.id)); }',
    ]),
  }),
  req('V040 我的视频列表', 'GET', '/video/my/list?page=1&size=12', {
    header: authHeader(),
    event: expectCode('V040', 200),
  }),
  req('A001 无权限访问', 'GET', '/admin/video/pending?page=1&size=10', {
    header: authHeader(),
    event: expectCode('A001', 403),
  }),
  req('A000 待审列表', 'GET', '/admin/video/pending?page=1&size=10', {
    header: authAdmin(),
    event: expectCode('A000', 200),
  }),
  req('A010 审核通过', 'POST', '/admin/video/approve', {
    header: authAdmin(),
    body: urlencoded([['videoId', '{{videoId}}']]),
    event: expectCode('A010', 200),
  }),
  req('A011 审核驳回', 'POST', '/admin/video/reject', {
    header: authAdmin(),
    body: urlencoded([['videoId', '{{videoIdReject}}']]),
    event: expectCode('A011', 200),
  }),
  req('V000 视频列表', 'GET', '/video/list?page=1&size=12', {
    event: expectCode('V000', 200),
  }),
  req('V010 视频详情', 'GET', '/video/getone?id={{videoId}}', {
    event: expectCode('V010', 200),
  }),
  req('V011 视频不存在', 'GET', '/video/getone?id=99999', {
    event: expectCode('V011', 404),
  }),
  req('V020 赞踩汇总', 'GET', '/video/reaction/summary?videoId={{videoId}}', {
    event: expectCode('V020', 200),
  }),
  req('V050 保存播放进度', 'POST', '/video/history/progress', {
    header: authHeader(),
    body: urlencoded([
      ['videoId', '{{videoId}}'],
      ['progress', '120'],
    ]),
    event: expectCode('V050', 200),
  }),
  req('V051 播放历史', 'GET', '/video/history/list?page=1&size=12', {
    header: authHeader(),
    event: expectCode('V051', 200),
  }),
  req('V060 修改视频', 'POST', '/video/update', {
    header: authHeader(),
    body: urlencoded([
      ['id', '{{videoId}}'],
      ['title', '新标题'],
      ['description', '新简介'],
      ['visibility', 'public'],
    ]),
    event: expectCode('V060', 200),
  }),
  req('预备 再审通过', 'POST', '/admin/video/approve', {
    header: authAdmin(),
    body: urlencoded([['videoId', '{{videoId}}']]),
    event: expectCode('预备-再审通过', 200),
  }),
  req('V061 改可见性', 'POST', '/video/visibility', {
    header: authHeader(),
    body: urlencoded([
      ['id', '{{videoId}}'],
      ['visibility', 'private'],
    ]),
    event: expectCode('V061', 200),
  }),
  req('预备 改回公开', 'POST', '/video/visibility', {
    header: authHeader(),
    body: urlencoded([
      ['id', '{{videoId}}'],
      ['visibility', 'public'],
    ]),
    event: expectCode('预备-改回公开', 200),
  }),
  req('预备 公开再审', 'POST', '/admin/video/approve', {
    header: authAdmin(),
    body: urlencoded([['videoId', '{{videoId}}']]),
    event: expectCode('预备-公开再审', 200),
  }),
  req('V063 举报视频', 'POST', '/video/report', {
    header: authB(),
    body: urlencoded([
      ['id', '{{videoId}}'],
      ['reason', '测试举报'],
    ]),
    event: expectCode('V063', 200),
  }),
  req('C001 未登录发评论', 'POST', '/comment/add', {
    body: urlencoded([
      ['targetId', '{{videoId}}'],
      ['targetType', '1'],
      ['content', '测试评论'],
    ]),
    event: expectHttp('C001', 403),
  }),
  req('C002 target 不存在', 'POST', '/comment/add', {
    header: authHeader(),
    body: urlencoded([
      ['targetId', '99999'],
      ['targetType', '1'],
      ['content', 'xx'],
    ]),
    event: expectCode('C002', 404),
  }),
  req('C000 发评论-视频', 'POST', '/comment/add', {
    header: authHeader(),
    body: urlencoded([
      ['targetId', '{{videoId}}'],
      ['targetType', '1'],
      ['content', '测试评论'],
    ]),
    event: expectCode('C000', 200, [
      'if (j && j.data) { pm.collectionVariables.set("commentId", String(j.data.id)); }',
    ]),
  }),
  req('C010 评论列表', 'GET', '/comment/list?targetId={{videoId}}&targetType=1&page=1&size=20', {
    event: expectCode('C010', 200),
  }),
  req('R000 视频点赞', 'POST', '/video/reaction', {
    header: authB(),
    body: urlencoded([
      ['videoId', '{{videoId}}'],
      ['reaction', '1'],
    ]),
    event: expectCode('R000', 200),
  }),
  req('R001 视频点踩', 'POST', '/video/reaction', {
    header: authHeader(),
    body: urlencoded([
      ['videoId', '{{videoId}}'],
      ['reaction', '-1'],
    ]),
    event: expectCode('R001', 200),
  }),
  req('R010 评论点赞', 'POST', '/comment/reaction', {
    header: authB(),
    body: urlencoded([
      ['commentId', '{{commentId}}'],
      ['reaction', '1'],
    ]),
    event: expectCode('R010', 200),
  }),
  req('F000 关注', 'POST', '/subscription/follow', {
    header: authHeader(),
    body: urlencoded([['targetId', '{{userBId}}']]),
    event: expectCode('F000', 200),
  }),
  req('F001 取消关注', 'POST', '/subscription/unfollow', {
    header: authHeader(),
    body: urlencoded([['targetId', '{{userBId}}']]),
    event: expectCode('F001', 200),
  }),
  req('F010 关注状态', 'GET', '/subscription/status?targetId={{userBId}}', {
    header: authHeader(),
    event: expectCode('F010', 200),
  }),
  req('F030 订阅动态', 'GET', '/subscription/feed?page=1&size=12', {
    header: authHeader(),
    event: expectCode('F030', 200),
  }),
  req('S000 搜索成功', 'GET', '/search?keyword=%E6%B5%8B%E8%AF%95%E6%98%B5%E7%A7%B0&videoLimit=10&liveLimit=10&userLimit=10', {
    event: expectCode('S000', 200),
  }),
  req('S001 搜索无结果', 'GET', '/search?keyword=xyznotexist123&videoLimit=10&liveLimit=10&userLimit=10', {
    event: expectCode('S001', 200),
  }),
  req('S002 搜索无结果-数字关键词', 'GET', '/search?keyword=11111xyznotexist&videoLimit=10&liveLimit=10&userLimit=10', {
    event: expectCode('S002', 200),
  }),
  req('L020 创建直播间', 'POST', '/live/create', {
    header: authHeader(),
    body: urlencoded([['title', '测试直播间']]),
    event: expectCode('L020', 200, [
      'if (j && j.data) { pm.collectionVariables.set("liveId", String(j.data.id)); }',
    ]),
  }),
  req('L021 开播', 'POST', '/live/start', {
    header: authHeader(),
    body: urlencoded([['id', '{{liveId}}']]),
    event: expectCode('L021', 200),
  }),
  req('L000 直播列表', 'GET', '/live/list?page=1&size=12', {
    event: expectCode('L000', 200),
  }),
  req('L010 直播详情', 'GET', '/live/getone?id={{liveId}}', {
    event: expectCode('L010', 200),
  }),
  req('L041 开播后发弹幕', 'POST', '/comment/add', {
    header: authHeader(),
    body: urlencoded([
      ['targetId', '{{liveId}}'],
      ['targetType', '2'],
      ['content', '直播弹幕'],
    ]),
    event: expectCode('L041', 200),
  }),
  req('L022 停播', 'POST', '/live/stop', {
    header: authHeader(),
    body: urlencoded([['id', '{{liveId}}']]),
    event: expectCode('L022', 200),
  }),
  req('L040 停播后发弹幕', 'POST', '/comment/add', {
    header: authHeader(),
    body: urlencoded([
      ['targetId', '{{liveId}}'],
      ['targetType', '2'],
      ['content', '直播弹幕'],
    ]),
    event: expectCode('L040', 400),
  }),
  req('N000 通知列表', 'GET', '/notification/list?page=1&size=20', {
    header: authHeader(),
    event: expectCode('N000', 200),
  }),
  req('N010 未读数', 'GET', '/notification/unread-count', {
    header: authHeader(),
    event: expectCode('N010', 200),
  }),
  req('N021 全部已读', 'POST', '/notification/read', {
    header: authHeader(),
    body: urlencoded([]),
    event: expectCode('N021', 200),
  }),
  req('M000 打开会话', 'POST', '/message/room/open', {
    header: authHeader(),
    body: urlencoded([['peerId', '{{userBId}}']]),
    event: expectCode('M000', 200, [
      'if (j && j.data) { pm.collectionVariables.set("roomId", String(j.data.roomId)); }',
    ]),
  }),
  req('M020 发送消息', 'POST', '/message/send', {
    header: authHeader(),
    body: urlencoded([
      ['roomId', '{{roomId}}'],
      ['content', '你好，测试私信'],
    ]),
    event: expectCode('M020', 200),
  }),
  req('M010 获取消息', 'GET', '/message/room/get?roomId={{roomId}}&page=1&size=50', {
    header: authHeader(),
    event: expectCode('M010', 200),
  }),
  req('A020 举报复审列表', 'GET', '/admin/video/report-review?page=1&size=10', {
    header: authAdmin(),
    event: expectCode('A020', 200),
  }),
  req('A040 管理员删视频', 'POST', '/admin/video/delete', {
    header: authAdmin(),
    body: urlencoded([['videoId', '{{videoIdDelete}}']]),
    event: expectCode('A040', 200),
  }),
  req('U040 登出', 'GET', '/user/account/logout', {
    header: authHeader(),
    event: expectCode('U040', 200),
  }),
]

const collection = {
  info: {
    name: 'doinb 全量系统测试（59 条）',
    description:
      '对应交付文档/测试报告第 4 节。必须按顺序整集跑。Newman：npx newman run postman/doinb.full.postman_collection.json -e postman/doinb.ci.postman_environment.json --working-dir . --reporters cli,json --reporter-json-export postman/out/newman-report.json。写进报告请用 node postman/run-full-report.mjs（会落每条真实 JSON）。',
    schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
  },
  variable: [
    { key: 'usernameA', value: '' },
    { key: 'usernameB', value: '' },
    { key: 'password', value: '123456' },
    { key: 'adminUser', value: 'demo_admin' },
    { key: 'adminPassword', value: '123456' },
    { key: 'tokenA', value: '' },
    { key: 'tokenB', value: '' },
    { key: 'tokenAdmin', value: '' },
    { key: 'userAId', value: '' },
    { key: 'userBId', value: '' },
    { key: 'videoId', value: '' },
    { key: 'videoIdReject', value: '' },
    { key: 'videoIdDelete', value: '' },
    { key: 'liveId', value: '' },
    { key: 'commentId', value: '' },
    { key: 'roomId', value: '' },
  ],
  event: [
    {
      listen: 'prerequest',
      script: {
        type: 'text/javascript',
        exec: [
          "if (!pm.collectionVariables.get('usernameA')) {",
          "  const ts = Date.now();",
          "  pm.collectionVariables.set('usernameA', 'rpt_a_' + ts);",
          "  pm.collectionVariables.set('usernameB', 'rpt_b_' + ts);",
          "  pm.collectionVariables.set('password', '123456');",
          '}',
        ],
      },
    },
  ],
  item: items,
}

fs.writeFileSync(out, JSON.stringify(collection, null, 2), 'utf8')
console.log('wrote', out, 'items=', items.length)
