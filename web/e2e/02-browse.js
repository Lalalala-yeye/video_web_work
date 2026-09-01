/**
 * TASK-E2E-02 浏览 + 搜索。
 * 前置：后端 8081 + 前端 `npm run dev`（8787）+ 本机 Chrome。
 * 管理员账号默认 demo_admin / 123456（可用 E2E_ADMIN_USER / E2E_ADMIN_PASSWORD 覆盖）。
 * 覆盖：未登录浏览首页、未登录搜索空结果、上传公开视频并过审后首页列表出现、
 *      点击进入视频详情、用播放器上报进度并在个人中心看到历史/续播（UC-05）、
 *      按关键词搜到视频、按标题搜到直播间、按昵称搜到用户、
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
  cleanupUserVideos,
  openProfile,
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

async function clickSearchTab(driver, label) {
  const tab = await driver.wait(
    until.elementLocated(By.xpath(`//div[@role='tab'][contains(., '${label}')]`)),
    12000
  )
  await driver.executeScript('arguments[0].click()', tab)
}

async function waitDisplayed(driver, locator, timeoutMs = 12000) {
  await driver.wait(
    async () => {
      const els = await driver.findElements(locator)
      for (const el of els) {
        try {
          if (await el.isDisplayed()) return true
        } catch {
          /* stale */
        }
      }
      return false
    },
    timeoutMs,
    `可见元素未出现: ${locator}`
  )
}

/** 空搜索文案在三个 Tab 的 DOM 里都有，必须看当前可见面板，不能扫整页 HTML。 */
async function waitVisibleEmpty(driver, text, timeoutMs = 12000) {
  await driver.wait(
    async () => {
      const panes = await driver.findElements(
        By.css('.el-tab-pane:not([aria-hidden="true"]), .el-tab-pane.is-active')
      )
      const seen = new Set()
      for (const pane of panes) {
        try {
          const id = await pane.getId()
          if (seen.has(id)) continue
          seen.add(id)
          if (await pane.isDisplayed()) {
            const t = await pane.getText()
            if (t.includes(text)) return true
          }
        } catch {
          /* stale */
        }
      }
      return false
    },
    timeoutMs,
    `当前 Tab 未出现空结果文案: ${text}`
  )
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
  await setVueInputValue(driver, titleInput, title)
  await clickXpath(driver, "//button[contains(., '提交上传')]")
  await waitMessageContains(driver, '上传成功')
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
    await waitVisibleEmpty(driver, '没有找到相关视频')
    await clickSearchTab(driver, '直播')
    await waitVisibleEmpty(driver, '没有找到相关直播')
    await clickSearchTab(driver, '用户')
    await waitVisibleEmpty(driver, '没有找到相关用户')
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
    await approvePendingVideo(driver, videoId, videoTitle)
    await waitMessageContains(driver, '已通过审核', 12000)
    console.log('OK  管理员通过审核')

    /* ---------- 5. 切回作者：创建直播间（标题含关键词，供搜索验证） ---------- */
    await injectSession(driver, authorName, password)
    await driver.get(`${BASE_URL}/studio/live`)
    await driver.wait(until.elementLocated(By.css('input[placeholder="直播间标题"]')), 12000)
    await fillByPlaceholder(driver, '直播间标题', liveTitle)
    await clickXpath(driver, "//div[contains(@class,'create-row')]//button[contains(., '创建')]")
    await waitMessageContains(driver, '创建成功')
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

    const progressInfo = await driver.executeAsyncScript(`
      const done = arguments[arguments.length - 1];
      const v = document.querySelector('video.player');
      if (!v) { done({ ok: false, message: '页面没有 video.player' }); return; }
      let finished = false;
      const once = function (result) {
        if (finished) return;
        finished = true;
        done(result);
      };
      const finish = function () {
        const dur = Number(v.duration);
        if (!Number.isFinite(dur) || dur < 1) {
          once({ ok: false, message: '视频无法播放 duration=' + v.duration });
          return;
        }
        const target = dur >= 10 ? 10 : Math.max(1, Math.floor(dur));
        try { v.currentTime = target; } catch (e) { once({ ok: false, message: String(e) }); return; }
        v.dispatchEvent(new Event('timeupdate'));
        v.dispatchEvent(new Event('pause'));
        once({ ok: true, progress: Math.floor(v.currentTime), duration: dur });
      };
      if (v.readyState >= 1) { finish(); return; }
      v.addEventListener('loadedmetadata', finish, { once: true });
      v.addEventListener('error', function () { once({ ok: false, message: '播放器加载失败' }); }, { once: true });
      setTimeout(function () {
        if (v.readyState >= 1) finish();
        else once({ ok: false, message: '等待视频元数据超时 readyState=' + v.readyState });
      }, 10000);
    `)
    assert.equal(progressInfo.ok, true, `未能通过播放器上报进度: ${progressInfo.message}`)
    await sleep(800)
    await openProfile(driver)
    await driver.wait(
      until.elementLocated(By.xpath(`//a[contains(@class,'history-item')][contains(., '${videoTitle}')]`)),
      12000
    )
    await driver.wait(
      until.elementLocated(
        By.xpath(
          `//a[contains(@class,'history-item')][contains(., '${videoTitle}')][contains(., '看到 ${progressInfo.progress} 秒')]`
        )
      ),
      12000
    )
    console.log('OK  个人中心播放历史出现该视频，进度', progressInfo.progress, '秒')
    await shot(driver, '02-2b-history')

    const historyLink = await driver.findElement(
      By.xpath(`//a[contains(@class,'history-item')][contains(., '${videoTitle}')]`)
    )
    await historyLink.click()
    await driver.wait(until.urlContains(`/video/${videoId}`), 12000)
    await driver.wait(until.elementLocated(By.css('video.player')), 12000)
    await driver.wait(
      async () => {
        const t = await driver.executeScript(
          'const v = document.querySelector("video.player"); return v && Number.isFinite(v.currentTime) ? v.currentTime : 0'
        )
        return Math.abs(Number(t) - Number(progressInfo.progress)) < 1.5
      },
      12000,
      `从历史进入详情应续播到约 ${progressInfo.progress} 秒`
    )
    console.log('OK  从播放历史进入详情已续播')

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
    await clickSearchTab(driver, '直播')
    await waitDisplayed(
      driver,
      By.xpath(`//a[contains(@class,'live-card')][contains(., '${liveTitle}')]`)
    )
    console.log('OK  按标题搜索到直播间')
    await shot(driver, '02-4-search-live')

    /* ---------- 9. 搜索作者昵称 → 用户结果 ---------- */
    const nickname = `用户_${authorName}`
    await doSearch(driver, nickname)
    await clickSearchTab(driver, '用户')
    await waitDisplayed(
      driver,
      By.xpath(`//div[contains(@class,'user-item')][contains(., '${nickname}')]`)
    )
    const userItem = await driver.findElement(
      By.xpath(`//div[contains(@class,'user-item')][contains(., '${nickname}')]`)
    )
    assert.ok((await userItem.getText()).includes(nickname), '用户结果应显示昵称')
    console.log('OK  按昵称搜索到用户')
    await shot(driver, '02-5-search-user')

    /* ---------- 10. 登录态搜索不存在关键词 → 三个 Tab 均为空 ---------- */
    await doSearch(driver, garbage)
    await waitVisibleEmpty(driver, '没有找到相关视频')
    await clickSearchTab(driver, '直播')
    await waitVisibleEmpty(driver, '没有找到相关直播')
    await clickSearchTab(driver, '用户')
    await waitVisibleEmpty(driver, '没有找到相关用户')
    console.log('OK  登录态搜索空结果：视频/直播/用户均为空')
    await shot(driver, '02-6-search-empty')

    console.log('\n全部通过 ✅  TASK-E2E-02 浏览+搜索')
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
