/**
 * TASK-E2E-02 浏览 + 搜索。
 * 前置：后端 8081 + 前端 `npm run dev`（8787）+ 本机 Chrome。
 * 管理员账号默认 demo_admin / 123456（可用 E2E_ADMIN_USER / E2E_ADMIN_PASSWORD 覆盖）。
 * 覆盖：未登录浏览首页、未登录搜索空结果、上传公开视频并过审后首页列表出现、
 *      点击进入视频详情、按关键词搜到视频、按标题搜到直播间、按昵称搜到用户、
 *      搜索不存在关键词时视频/直播/用户三个 Tab 均为空。
 * 证据：e2e/artifacts/ 下自动保存关键步骤截图。
 */
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { By, Key, until } from 'selenium-webdriver'
import {
  BASE_URL,
  createDriver,
  uniqueUsername,
  register,
  login,
  waitLoggedIn,
  fillByPlaceholder,
} from './helpers.js'

const API = process.env.E2E_API || 'http://127.0.0.1:8081'
const ADMIN_USER = process.env.E2E_ADMIN_USER || 'demo_admin'
const ADMIN_PASS = process.env.E2E_ADMIN_PASSWORD || '123456'
const __dirname = path.dirname(fileURLToPath(import.meta.url))
const FIXTURE_VIDEO = path.join(__dirname, 'fixtures', 'test-video.mp4')

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function waitToast(driver, text, timeoutMs = 8000) {
  await driver.wait(
    async () => (await driver.getPageSource()).includes(text),
    timeoutMs,
    `页面未出现预期文案: ${text}`
  )
}

async function apiLogin(username, password) {
  const res = await fetch(`${API}/user/account/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  return res.json()
}

async function shot(driver, name) {
  const img = await driver.takeScreenshot()
  const dir = path.join(__dirname, 'artifacts')
  fs.mkdirSync(dir, { recursive: true })
  fs.writeFileSync(path.join(dir, `${name}.png`), img, 'base64')
  console.log('📷 已保存截图 artifacts/' + name + '.png')
}

/** 页面登录在已有其他账号 token 时偶发填表失败；后续切账号改写 session（登录页已由 01 覆盖） */
async function injectSession(driver, username, password) {
  const body = await apiLogin(username, password)
  assert.equal(body.code, 200, `切换账号失败 ${username}: ${body.message}`)
  const { token, user } = body.data
  await driver.get(BASE_URL)
  await driver.executeScript(
    `const token = arguments[0];
     const user = arguments[1];
     const key = 'doinb_accounts';
     let list = [];
     try { list = JSON.parse(localStorage.getItem(key) || '[]'); } catch (e) { list = []; }
     if (!Array.isArray(list)) list = [];
     const entry = { user: user, token: token, updatedAt: Date.now() };
     const idx = list.findIndex(function (a) { return Number(a.user && a.user.id) === Number(user.id); });
     if (idx >= 0) list[idx] = entry; else list.push(entry);
     localStorage.setItem(key, JSON.stringify(list));
     sessionStorage.setItem('doinb_active_id', String(user.id));`,
    token,
    user
  )
  await driver.navigate().refresh()
  await driver.wait(until.elementLocated(By.css('.user-name')), 20000, `注入登录后顶栏应显示用户名: ${username}`)
}

async function clickXpath(driver, xpath, timeoutMs = 12000) {
  const el = await driver.wait(until.elementLocated(By.xpath(xpath)), timeoutMs)
  await driver.wait(until.elementIsVisible(el), timeoutMs)
  await el.click()
  return el
}

/** 创作中心上传"他人可见"视频（进入待审），返回视频 id */
async function uploadPublicVideo(driver, title) {
  await driver.get(`${BASE_URL}/studio/upload`)
  await driver.wait(until.elementLocated(By.css('.page-title')), 12000)
  const fileInputs = await driver.findElements(By.css('input[type="file"]'))
  assert.ok(fileInputs.length >= 1, '上传页应有文件选择框')
  await fileInputs[0].sendKeys(FIXTURE_VIDEO)
  const titleInput = await driver.wait(
    until.elementLocated(By.css('input[placeholder="请输入视频标题"]')),
    12000
  )
  await titleInput.sendKeys(title)
  await clickXpath(driver, "//button[contains(., '提交上传')]")
  await waitToast(driver, '上传成功')
  await driver.wait(until.urlContains('/studio/edit'), 12000)
  const url = await driver.getCurrentUrl()
  const match = url.match(/\/studio\/edit\/(\d+)/)
  return match ? match[1] : null
}

/** 在搜索页输入关键词并点「搜索」，等待跳转到结果页（不要用顶栏同 placeholder 的输入框） */
async function doSearch(driver, keyword) {
  await driver.get(`${BASE_URL}/search`)
  const input = await driver.wait(until.elementLocated(By.css('.search-bar input')), 12000)
  await input.click()
  await input.sendKeys(Key.CONTROL, 'a', Key.BACK_SPACE)
  await input.sendKeys(keyword)
  const btn = await driver.wait(
    until.elementLocated(By.css('.search-bar button, .search-bar .el-button')),
    12000
  )
  await driver.executeScript('arguments[0].click()', btn)
  await driver.wait(
    async () => {
      const url = await driver.getCurrentUrl()
      return url.includes('/search?') && url.includes('keyword=')
    },
    12000,
    '搜索后 URL 应带 keyword 查询参数'
  )
}

async function run() {
  assert.ok(fs.existsSync(FIXTURE_VIDEO), `缺少测试视频: ${FIXTURE_VIDEO}`)
  console.log('启动 Chrome…  目标:', BASE_URL)
  const driver = await createDriver()
  const password = '123456'
  const authorName = uniqueUsername('e2e2a')
  const videoTitle = `E2E浏览搜索视频_${Date.now()}`
  const liveTitle = `E2E浏览搜索直播_${Date.now()}`
  const garbage = `e2e不存在xyz_${Date.now()}`

  try {
    /* ---------- 1. 未登录：打开首页 ---------- */
    await driver.get(BASE_URL)
    await driver.wait(until.elementLocated(By.css('.logo-text, .page-container')), 12000)
    console.log('OK  未登录可打开首页')

    /* ---------- 2. 未登录：搜索不存在关键词 → 三个 Tab 均为空 ---------- */
    await doSearch(driver, garbage)
    await waitToast(driver, '没有找到相关视频')
    await clickXpath(driver, "//div[contains(@role,'tab')][contains(., '直播')]")
    await waitToast(driver, '没有找到相关直播')
    await clickXpath(driver, "//div[contains(@role,'tab')][contains(., '用户')]")
    await waitToast(driver, '没有找到相关用户')
    console.log('OK  未登录搜索空结果：视频/直播/用户均为空')
    await shot(driver, '02-0-search-empty-anonymous')

    /* ---------- 3. 作者注册登录并上传公开视频（进入待审） ---------- */
    await register(driver, authorName, password)
    await driver.wait(until.urlContains('/login'), 12000)
    await login(driver, authorName, password)
    await waitLoggedIn(driver)
    const videoId = await uploadPublicVideo(driver, videoTitle)
    assert.ok(videoId, '上传后应拿到视频 id')
    console.log('OK  上传公开视频（待审）', videoTitle, `id=${videoId}`)

    /* ---------- 4. 管理员过审 ---------- */
    const adminBody = await apiLogin(ADMIN_USER, ADMIN_PASS)
    if (adminBody.code !== 200 || Number(adminBody.data?.user?.role) !== 2) {
      throw new Error(
        `管理员登录失败（${ADMIN_USER}）：${adminBody.message || '非管理员'}。` +
          '请确认库里有 role=2 的账号，或设置 E2E_ADMIN_USER / E2E_ADMIN_PASSWORD'
      )
    }
    await injectSession(driver, ADMIN_USER, ADMIN_PASS)
    await driver.get(`${BASE_URL}/admin/pending`)
    await driver.wait(until.elementLocated(By.xpath("//h1[contains(., '待审视频')]")), 12000)
    await driver.wait(
      until.elementLocated(By.xpath(`//div[contains(@class,'el-table')]//tr[contains(., '${videoTitle}')]`)),
      15000
    )
    await sleep(400)
    const approveBtn = await driver.findElement(
      By.xpath(`//div[contains(@class,'el-table')]//tr[contains(., '${videoTitle}')]//button[contains(., '通过')]`)
    )
    await driver.executeScript('arguments[0].scrollIntoView({block:"center"}); arguments[0].click();', approveBtn)
    await waitToast(driver, '已通过审核', 12000)
    console.log('OK  管理员通过审核')

    /* ---------- 5. 切回作者：创建直播间（标题含关键词，供搜索验证） ---------- */
    await injectSession(driver, authorName, password)
    await driver.get(`${BASE_URL}/studio/live`)
    await driver.wait(until.elementLocated(By.css('input[placeholder="直播间标题"]')), 12000)
    await fillByPlaceholder(driver, '直播间标题', liveTitle)
    await clickXpath(driver, "//div[contains(@class,'create-row')]//button[contains(., '创建')]")
    await waitToast(driver, '创建成功')
    console.log('OK  创建直播间', liveTitle)

    /* ---------- 6. 首页浏览：已发布视频出现在列表，点击进入详情 ---------- */
    await driver.get(BASE_URL)
    const card = await driver.wait(
      until.elementLocated(By.xpath(`//a[contains(@class,'video-card')][contains(@href, '/video/${videoId}')]`)),
      12000
    )
    await driver.wait(until.elementIsVisible(card), 12000)
    assert.ok((await card.getText()).includes(videoTitle), '首页卡片应显示视频标题')
    console.log('OK  首页列表出现已发布视频')
    await shot(driver, '02-1-home-list')

    await card.click()
    await driver.wait(until.urlContains(`/video/${videoId}`), 12000)
    const detailTitle = await driver.wait(until.elementLocated(By.css('.video-title')), 12000)
    assert.equal(await detailTitle.getText(), videoTitle, '详情页标题应与上传一致')
    await driver.wait(until.elementLocated(By.css('video.player')), 12000)
    const authorNameEl = await driver.wait(until.elementLocated(By.css('.author-name')), 12000)
    assert.ok((await authorNameEl.getText()).length > 0, '详情页应展示作者昵称')
    console.log('OK  视频详情页：标题/播放器/作者均展示')
    await shot(driver, '02-2-video-detail')

    /* ---------- 7. 搜索关键词 → 视频结果 ---------- */
    await doSearch(driver, videoTitle)
    await driver.wait(
      until.elementLocated(By.xpath(`//a[contains(@class,'video-card')][contains(., '${videoTitle}')]`)),
      12000
    )
    console.log('OK  按关键词搜索到视频')
    await shot(driver, '02-3-search-video')

    /* ---------- 8. 搜索直播间标题 → 直播结果 ---------- */
    await doSearch(driver, liveTitle)
    await clickXpath(driver, "//div[contains(@role,'tab')][contains(., '直播')]")
    await driver.wait(
      until.elementLocated(By.xpath(`//a[contains(@class,'live-card')][contains(., '${liveTitle}')]`)),
      12000
    )
    console.log('OK  按标题搜索到直播间')
    await shot(driver, '02-4-search-live')

    /* ---------- 9. 搜索作者昵称 → 用户结果 ---------- */
    const nickname = `用户_${authorName}`
    await doSearch(driver, nickname)
    await clickXpath(driver, "//div[contains(@role,'tab')][contains(., '用户')]")
    const userItem = await driver.wait(
      until.elementLocated(By.xpath(`//div[contains(@class,'user-item')][contains(., '${nickname}')]`)),
      12000
    )
    await driver.wait(until.elementIsVisible(userItem), 12000)
    assert.ok((await userItem.getText()).includes(nickname), '用户结果应显示昵称')
    console.log('OK  按昵称搜索到用户')
    await shot(driver, '02-5-search-user')

    /* ---------- 10. 登录态搜索不存在关键词 → 三个 Tab 均为空 ---------- */
    await doSearch(driver, garbage)
    await waitToast(driver, '没有找到相关视频')
    await clickXpath(driver, "//div[contains(@role,'tab')][contains(., '直播')]")
    await waitToast(driver, '没有找到相关直播')
    await clickXpath(driver, "//div[contains(@role,'tab')][contains(., '用户')]")
    await waitToast(driver, '没有找到相关用户')
    console.log('OK  登录态搜索空结果：视频/直播/用户均为空')
    await shot(driver, '02-6-search-empty')

    console.log('\n全部通过 ✅  TASK-E2E-02 浏览+搜索')
  } finally {
    await driver.quit()
  }
}

run().catch((e) => {
  console.error('FAILED:', e.message)
  if (e.stack) console.error(e.stack)
  process.exit(1)
})
