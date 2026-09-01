/**
 * TASK-E2E-05 通知 + 私信 + 后台。
 * 前置：后端 8081 + 前端 `npm run dev`（8787）+ 本机 Chrome。
 * 后台管理员默认 demo_admin / 123456（可用 E2E_ADMIN_USER、E2E_ADMIN_PASSWORD 覆盖）。
 * 覆盖：发私信、通知面板、非管理员进后台被拦、管理员概览/待审/通过、
 *      页面点赞后作者收到通知、3 名用户举报后进入举报复审队列（UC-07、UC-13）。
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
  cleanupUserVideos,
  injectSession,
  apiLogin,
  waitMessageContains,
  setVueInputValue,
  approvePendingVideo,
} from './helpers.js'

const ADMIN_USER = process.env.E2E_ADMIN_USER || 'demo_admin'
const ADMIN_PASS = process.env.E2E_ADMIN_PASSWORD || '123456'
const __dirname = path.dirname(fileURLToPath(import.meta.url))
const FIXTURE_VIDEO = path.join(__dirname, 'fixtures', 'test-video.mp4')

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

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

async function clickXpath(driver, xpath, timeoutMs = 12000) {
  const el = await driver.wait(until.elementLocated(By.xpath(xpath)), timeoutMs)
  await driver.wait(until.elementIsVisible(el), timeoutMs)
  await el.click()
  return el
}

async function reportCurrentVideo(driver, reason) {
  const reportBtn = await driver.wait(until.elementLocated(By.css('.report-btn')), 12000)
  await driver.executeScript(
    'arguments[0].scrollIntoView({block:"center"}); arguments[0].click();',
    reportBtn
  )
  const reportInput = await driver.wait(
    until.elementLocated(By.css('.el-message-box input, .el-message-box textarea')),
    12000
  )
  await reportInput.sendKeys(reason)
  const submitReport = await driver.wait(
    until.elementLocated(By.xpath("//div[contains(@class,'el-message-box')]//button[contains(., '提交')]")),
    12000
  )
  await submitReport.click()
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
  await setVueInputValue(driver, titleInput, title)
  await clickXpath(driver, "//button[contains(., '提交上传')]")
  await waitMessageContains(driver, '上传成功')
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
    await waitMessageContains(driver, '发来私信', 12000)
    await waitMessageContains(driver, dmText, 12000)
    console.log('OK  通知面板出现「发来私信」')
    await shot(driver, '05-2-notify')

    /* ---------- 4. 非管理员进后台 ---------- */
    await driver.get(`${BASE_URL}/admin`)
    await waitMessageContains(driver, '需要管理员权限')
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
    assert.ok(videoId, '上传后应拿到视频 id')
    await approvePendingVideo(driver, videoId, videoTitle)
    await waitMessageContains(driver, '已通过审核', 12000)
    console.log('OK  待审视频通过', videoTitle)
    await shot(driver, '05-4-approve')

    /* ---------- 6. 用户B 在页面点赞 → 作者通知「赞了你的视频」 ---------- */
    await injectSession(driver, viewerName, password)
    await driver.get(`${BASE_URL}/video/${videoId}`)
    await driver.wait(until.elementLocated(By.css('.video-title')), 12000)
    const likeBtn = await driver.wait(
      until.elementLocated(By.xpath("//div[contains(@class,'video-actions')]//button[.//img[@alt='赞']]")),
      12000
    )
    await likeBtn.click()
    await waitMessageContains(driver, '点赞成功')
    console.log('OK  页面点赞已过审视频')
    await shot(driver, '05-5-like')

    await injectSession(driver, authorName, password)
    const notifyBtn2 = await driver.wait(
      until.elementLocated(By.css('button.notify-btn[aria-label="通知"]')),
      12000
    )
    await driver.executeScript('arguments[0].click()', notifyBtn2)
    await driver.wait(until.elementLocated(By.css('.panel .title')), 12000)
    await waitMessageContains(driver, '赞了你的视频', 12000)
    console.log('OK  通知面板出现「赞了你的视频」')
    await shot(driver, '05-5-like-notify')

    /* ---------- 7. 3 名用户举报后进入复审（阈值 3，不改产品） ---------- */
    const reporter2 = uniqueUsername('e2e5c')
    const reporter3 = uniqueUsername('e2e5d')

    await injectSession(driver, viewerName, password)
    await driver.get(`${BASE_URL}/video/${videoId}`)
    await driver.wait(until.elementLocated(By.css('.video-title')), 12000)
    await reportCurrentVideo(driver, 'E2E举报1')
    await waitMessageContains(driver, '举报已提交', 12000)
    assert.ok(
      !(await driver.getPageSource()).includes('该视频已进入复审'),
      '第 1 次举报不应进入复审'
    )
    console.log('OK  第 1 次举报已提交')

    await register(driver, reporter2, password)
    await driver.wait(until.urlContains('/login'), 12000)
    await login(driver, reporter2, password)
    await waitLoggedIn(driver)
    await driver.get(`${BASE_URL}/video/${videoId}`)
    await driver.wait(until.elementLocated(By.css('.video-title')), 12000)
    await reportCurrentVideo(driver, 'E2E举报2')
    await waitMessageContains(driver, '举报已提交', 12000)
    assert.ok(
      !(await driver.getPageSource()).includes('该视频已进入复审'),
      '第 2 次举报不应进入复审'
    )
    console.log('OK  第 2 次举报已提交')

    await register(driver, reporter3, password)
    await driver.wait(until.urlContains('/login'), 12000)
    await login(driver, reporter3, password)
    await waitLoggedIn(driver)
    await driver.get(`${BASE_URL}/video/${videoId}`)
    await driver.wait(until.elementLocated(By.css('.video-title')), 12000)
    await reportCurrentVideo(driver, 'E2E举报3')
    await waitMessageContains(driver, '举报已提交，该视频已进入复审', 12000)
    console.log('OK  第 3 次举报，视频进入复审')
    await shot(driver, '05-4b-report')

    await injectSession(driver, ADMIN_USER, ADMIN_PASS)
    await driver.get(`${BASE_URL}/admin/report`)
    await driver.wait(until.elementLocated(By.xpath("//h1[contains(., '举报复审')]")), 12000)
    await driver.wait(
      until.elementLocated(
        By.xpath(
          `//div[contains(@class,'el-table')]//tr[contains(., '${videoTitle}') and contains(., '举报待复核')]`
        )
      ),
      15000
    )
    const countText = await driver
      .findElement(By.xpath(`//div[contains(@class,'el-table')]//tr[contains(., '${videoTitle}')]/td[4]`))
      .getText()
    assert.equal(countText.trim(), '3', `复审列表举报次数应为 3，实际「${countText.trim()}」`)
    console.log('OK  举报复审队列出现该视频')
    await shot(driver, '05-6-report-queue')

    console.log('\n全部通过 ✅  TASK-E2E-05 通知+私信+后台')
  } finally {
    await cleanupUserVideos(authorName, password)
    await driver.quit()
  }
}

run().catch((e) => {
  console.error('FAILED:', e.message)
  if (e.stack) console.error(e.stack)
  process.exit(1)
})
