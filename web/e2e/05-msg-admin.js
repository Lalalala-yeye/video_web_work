/**
 * TASK-E2E-05 通知 + 私信 + 后台。
 * 前置：后端 8081 + 前端 `npm run dev`（8787）+ 本机 Chrome。
 * 后台管理员默认 demo_admin / 123456（可用 E2E_ADMIN_USER、E2E_ADMIN_PASSWORD 覆盖）。
 * 覆盖：发私信、通知面板、非管理员进后台被拦、管理员概览/待审/通过、举报复审页。
 * 证据：e2e/artifacts/ 下自动保存关键步骤截图。
 */
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { By, until } from 'selenium-webdriver'
import {
  BASE_URL,
  createDriver,
  uniqueUsername,
  register,
  login,
  waitLoggedIn,
  logout,
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
  const body = await res.json()
  return body
}

async function apiGetUserId(username, password) {
  const body = await apiLogin(username, password)
  assert.equal(body.code, 200, `API 登录失败: ${body.message}`)
  return body.data.user.id
}

async function shot(driver, name) {
  const img = await driver.takeScreenshot()
  const dir = path.join(__dirname, 'artifacts')
  fs.mkdirSync(dir, { recursive: true })
  fs.writeFileSync(path.join(dir, `${name}.png`), img, 'base64')
  console.log('📷 已保存截图 artifacts/' + name + '.png')
}

/** 页面登录在已有其他账号 token 时偶发填表失败；后续切账号改写 session（前面已测过登录页） */
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

async function run() {
  assert.ok(fs.existsSync(FIXTURE_VIDEO), `缺少测试视频: ${FIXTURE_VIDEO}`)
  console.log('启动 Chrome…  目标:', BASE_URL)
  const driver = await createDriver()
  const password = '123456'
  const authorName = uniqueUsername('e2e5a')
  const viewerName = uniqueUsername('e2e5b')
  const videoTitle = `E2E待审_${Date.now()}`
  const dmText = `E2E私信_${Date.now()}`

  try {
    /* ---------- 1. 作者注册并上传他人可见视频（进入待审） ---------- */
    await register(driver, authorName, password)
    await driver.wait(until.urlContains('/login'), 12000)
    await login(driver, authorName, password)
    await waitLoggedIn(driver)
    console.log('OK  作者登录', authorName)
    const videoId = await uploadPublicVideo(driver, videoTitle)
    console.log('OK  上传待审视频', videoTitle, videoId ? `id=${videoId}` : '')

    const authorId = await apiGetUserId(authorName, password)

    /* ---------- 2. 用户B 发私信 ---------- */
    await logout(driver)
    await register(driver, viewerName, password)
    await driver.wait(until.urlContains('/login'), 12000)
    await login(driver, viewerName, password)
    await waitLoggedIn(driver)
    console.log('OK  用户B登录', viewerName)

    await driver.get(`${BASE_URL}/user/${authorId}`)
    const msgBtn = await driver.wait(until.elementLocated(By.css('.msg-btn')), 12000)
    assert.equal(await msgBtn.getAttribute('title'), '发私信')
    await msgBtn.click()
    await driver.wait(until.urlContains('/messages/'), 12000)
    const box = await driver.wait(
      until.elementLocated(By.css('textarea[placeholder="输入消息..."]')),
      12000
    )
    await box.sendKeys(dmText)
    await clickXpath(driver, "//button[contains(., '发送')]")
    await driver.wait(until.elementLocated(By.xpath(`//*[contains(text(), '${dmText}')]`)), 12000)
    console.log('OK  发送私信并上屏')
    await shot(driver, '05-1-dm')

    /* ---------- 3. 作者打开通知：发来私信 ---------- */
    await logout(driver)
    await login(driver, authorName, password)
    await waitLoggedIn(driver)
    const notifyBtn = await driver.wait(
      until.elementLocated(By.css('button.notify-btn[aria-label="通知"]')),
      12000
    )
    await notifyBtn.click()
    await driver.wait(until.elementLocated(By.css('.panel .title')), 12000)
    await waitToast(driver, '发来私信', 12000)
    await waitToast(driver, dmText, 12000)
    console.log('OK  通知面板出现「发来私信」')
    await shot(driver, '05-2-notify')

    /* ---------- 4. 非管理员进后台 ---------- */
    await driver.get(`${BASE_URL}/admin`)
    await waitToast(driver, '需要管理员权限')
    await driver.wait(async () => {
      const url = await driver.getCurrentUrl()
      return /\/$/.test(new URL(url).pathname) || url.endsWith('/')
    }, 12000)
    console.log('OK  普通用户访问后台被拦回首页')

    /* ---------- 5. 管理员后台 ---------- */
    const adminBody = await apiLogin(ADMIN_USER, ADMIN_PASS)
    if (adminBody.code !== 200 || Number(adminBody.data?.user?.role) !== 2) {
      throw new Error(
        `管理员登录失败（${ADMIN_USER}）：${adminBody.message || '非管理员'}。` +
          '请确认库里有 role=2 的账号，或设置 E2E_ADMIN_USER / E2E_ADMIN_PASSWORD'
      )
    }

    await logout(driver)
    await login(driver, ADMIN_USER, ADMIN_PASS)
    await waitLoggedIn(driver)
    console.log('OK  管理员登录', ADMIN_USER)

    await driver.get(`${BASE_URL}/admin`)
    await driver.wait(until.elementLocated(By.xpath("//h1[contains(., '管理概览')]")), 12000)
    await driver.wait(until.elementLocated(By.xpath("//h2[contains(., '管理后台')]")), 12000)
    await driver.wait(until.elementLocated(By.xpath("//span[contains(., '待审视频')]")), 12000)
    console.log('OK  管理概览')
    await shot(driver, '05-3-admin-dashboard')

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
    console.log('OK  待审视频通过', videoTitle)
    await shot(driver, '05-4-approve')

    await driver.get(`${BASE_URL}/admin/report`)
    await driver.wait(until.elementLocated(By.xpath("//h1[contains(., '举报复审')]")), 12000)
    console.log('OK  打开举报复审页')

    /* ---------- 6. 用户B 点赞已过审视频 → 作者通知「赞了你的视频」 ---------- */
    if (videoId) {
      // 直接走登录页切换账号（多账号时「退出当前账号」会切到列表里下一个，不保证登出）
      await injectSession(driver, viewerName, password)
      await driver.get(`${BASE_URL}/video/${videoId}`)
      await driver.wait(until.elementLocated(By.css('.video-title')), 12000)
      const likeResult = await driver.executeAsyncScript(
        `const videoId = arguments[0];
         const done = arguments[1];
         const id = sessionStorage.getItem('doinb_active_id');
         const list = JSON.parse(localStorage.getItem('doinb_accounts') || '[]');
         const acc = list.find(function (a) { return String(a.user && a.user.id) === String(id); });
         if (!acc) { done({ code: 0, message: '浏览器里没有当前账号 token' }); return; }
         fetch('/api/video/reaction', {
           method: 'POST',
           headers: {
             'Content-Type': 'application/x-www-form-urlencoded',
             Authorization: acc.token
           },
           body: 'videoId=' + encodeURIComponent(videoId) + '&reaction=1'
         }).then(function (r) { return r.json(); }).then(done).catch(function (e) { done({ code: 0, message: String(e) }); });`,
        videoId
      )
      assert.equal(likeResult.code, 200, `点赞接口失败: ${likeResult.message}`)
      console.log('OK  点赞已过审视频')
      await shot(driver, '05-5-like')

      await injectSession(driver, authorName, password)
      const notifyBtn2 = await driver.wait(
        until.elementLocated(By.css('button.notify-btn[aria-label="通知"]')),
        12000
      )
      await driver.executeScript('arguments[0].click()', notifyBtn2)
      await driver.wait(until.elementLocated(By.css('.panel .title')), 12000)
      await waitToast(driver, '赞了你的视频', 12000)
      console.log('OK  通知面板出现「赞了你的视频」')
      await shot(driver, '05-5-like-notify')
    }

    console.log('\n全部通过 ✅  TASK-E2E-05 通知+私信+后台')
  } finally {
    await driver.quit()
  }
}

run().catch((e) => {
  console.error('FAILED:', e.message)
  if (e.stack) console.error(e.stack)
  process.exit(1)
})
