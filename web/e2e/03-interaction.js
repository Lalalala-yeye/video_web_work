/**
 * TASK-E2E-03 互动：评论、点赞、关注、订阅动态。
 * 前置：后端 8081 + 前端 `npm run dev`（8787）+ 本机 Chrome。
 * 管理员账号默认 demo_admin / 123456（可用 E2E_ADMIN_USER / E2E_ADMIN_PASSWORD 覆盖）。
 * 覆盖 UC-10 评论、UC-11 赞踩、UC-09 关注订阅。上传走顶栏创作中心。
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
  cleanupUserVideos,
  injectSession,
  apiLogin,
  waitMessageContains,
  setVueInputValue,
  approvePendingVideo,
  uploadStudioVideo,
  clickNav,
  openStudioSidebar,
} from './helpers.js'

const ADMIN_USER = process.env.E2E_ADMIN_USER || 'demo_admin'
const ADMIN_PASS = process.env.E2E_ADMIN_PASSWORD || '123456'
const __dirname = path.dirname(fileURLToPath(import.meta.url))
const FIXTURE_VIDEO = path.join(__dirname, 'fixtures', 'test-video.mp4')

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function shot(driver, name) {
  try {
    const img = await driver.takeScreenshot()
    const dir = path.join(__dirname, 'artifacts')
    fs.mkdirSync(dir, { recursive: true })
    fs.writeFileSync(path.join(dir, `${name}.png`), img, 'base64')
    console.log('📷 已保存截图 artifacts/' + name + '.png')
  } catch (err) {
    console.warn('截图保存失败', name, err.message)
  }
}

async function clickXpath(driver, xpath, timeoutMs = 12000) {
  const el = await driver.wait(until.elementLocated(By.xpath(xpath)), timeoutMs)
  await driver.wait(until.elementIsVisible(el), timeoutMs)
  await el.click()
  return el
}

async function run() {
  assert.ok(fs.existsSync(FIXTURE_VIDEO), `缺少测试视频: ${FIXTURE_VIDEO}`)
  console.log('启动 Chrome…  目标:', BASE_URL)
  const driver = await createDriver()
  const password = '123456'
  const authorName = uniqueUsername('e2e3a')
  const viewerName = uniqueUsername('e2e3b')
  const videoTitle = `E2E互动_${Date.now()}`

  try {
    /* ---------- 1. 作者注册并登录 ---------- */
    await register(driver, authorName, password)
    await driver.wait(until.urlContains('/login'), 12000)
    await login(driver, authorName, password)
    await waitLoggedIn(driver)
    console.log('OK  作者登录', authorName)

    /* ---------- 2. 从顶栏进创作中心上传公开视频（进入待审） ---------- */
    const videoId = await uploadStudioVideo(driver, {
      title: videoTitle,
      videoPath: FIXTURE_VIDEO,
      visibility: 'public',
    })
    assert.ok(videoId, '上传后应拿到视频 id')
    console.log('OK  上传公开视频（待审）', videoTitle, `id=${videoId}`)
    await sleep(800)

    /* ---------- 3. 侧栏点「修改视频」再预览详情（不直达 URL） ---------- */
    await openStudioSidebar(driver, '修改视频')
    const firstItem = await driver.wait(until.elementLocated(By.css('.video-item')), 12000)
    await firstItem.click()
    const previewLink = await driver.wait(
      until.elementLocated(By.css('a[href^="/video/"]')),
      12000
    )
    await previewLink.click()
    await driver.wait(until.elementLocated(By.css('.video-title')), 12000)
    console.log('OK  打开视频详情页')

    /* ---------- 4. 视频点赞 → 取消 → 点踩 ---------- */
    const likeBtn = await driver.wait(
      until.elementLocated(By.xpath("//div[contains(@class,'video-actions')]//button[.//img[@alt='赞']]")),
      12000
    )
    await likeBtn.click()
    await waitMessageContains(driver, '点赞成功')
    console.log('OK  视频点赞：出现"点赞成功"反馈')
    await shot(driver, '03-1-video-like')
    await sleep(600)

    await likeBtn.click()
    await waitMessageContains(driver, '已取消')
    console.log('OK  取消点赞：出现"已取消"反馈')
    await sleep(600)

    const dislikeBtn = await driver.findElement(
      By.xpath("//div[contains(@class,'video-actions')]//button[.//img[@alt='踩']]")
    )
    await dislikeBtn.click()
    await waitMessageContains(driver, '已点踩')
    console.log('OK  视频点踩：出现"已点踩"反馈')
    await sleep(600)

    /* ---------- 5. 发表评论 ---------- */
    const commentBox = await driver.findElement(By.css('textarea[placeholder="发表你的看法..."]'))
    await commentBox.sendKeys('E2E 自动化评论 e2e3')
    await driver.findElement(By.xpath("//button[contains(., '发表评论')]")).click()
    await waitMessageContains(driver, '评论成功')
    await driver.wait(
      until.elementLocated(By.xpath("//*[contains(text(), 'E2E 自动化评论')]")),
      12000
    )
    console.log('OK  发表评论：出现"评论成功"反馈且评论上屏')
    await sleep(600)

    /* ---------- 6. 评论点赞 ---------- */
    const commentLike = await driver.wait(
      until.elementLocated(By.xpath("//div[contains(@class,'comment-item')]//button[.//img[@alt='赞']]")),
      12000
    )
    await commentLike.click()
    await waitMessageContains(driver, '评论已点赞')
    console.log('OK  评论点赞：出现"评论已点赞"反馈')
    await shot(driver, '03-2-comment-like')

    /* ---------- 7. 管理员过审（订阅动态只聚合已发布内容） ---------- */
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
    console.log('OK  管理员通过审核', videoTitle)

    /* ---------- 8. 用户B 关注作者 ---------- */
    const authorLogin = await apiLogin(authorName, password)
    assert.equal(authorLogin.code, 200, `读取作者 id 失败: ${authorLogin.message}`)
    const targetAuthorId = authorLogin.data.user.id

    await register(driver, viewerName, password)
    await driver.wait(until.urlContains('/login'), 12000)
    await login(driver, viewerName, password)
    await waitLoggedIn(driver)
    console.log('OK  用户B登录', viewerName)

    await driver.get(`${BASE_URL}/user/${targetAuthorId}`)
    const followBtn = await driver.wait(until.elementLocated(By.css('.follow-btn')), 12000)
    const textBefore = await followBtn.getText()
    assert.ok(textBefore.includes('关注') && !textBefore.includes('已关注'), '初始应为未关注')
    await followBtn.click()
    await waitMessageContains(driver, '关注成功')
    console.log('OK  关注：出现"关注成功"反馈')
    const textAfter = await driver.findElement(By.css('.follow-btn')).getText()
    assert.ok(textAfter.includes('已关注'), '按钮状态应切换为"已关注"')
    await shot(driver, '03-3-following')
    await sleep(600)

    await clickNav(driver, '关注')
    await driver.wait(until.elementLocated(By.xpath("//h1[contains(., '关注动态')]")), 12000)
    await driver.wait(
      until.elementLocated(By.xpath(`//a[contains(@class,'video-card')][contains(., '${videoTitle}')]`)),
      12000
    )
    console.log('OK  关注动态出现该作者已发布视频')
    await shot(driver, '03-4-subscribe-feed')

    await driver.get(`${BASE_URL}/user/${targetAuthorId}`)
    await driver.wait(until.elementLocated(By.css('.follow-btn')), 12000)
    await driver.findElement(By.css('.follow-btn')).click()
    await waitMessageContains(driver, '已取消关注')
    console.log('OK  取消关注：出现"已取消关注"反馈')

    await clickNav(driver, '关注')
    await driver.wait(until.elementLocated(By.xpath("//h1[contains(., '关注动态')]")), 12000)
    await driver.wait(
      async () => {
        const src = await driver.getPageSource()
        return src.includes('还没有关注任何人') && !src.includes(videoTitle)
      },
      12000,
      '取消关注后动态页不应再出现该视频'
    )
    console.log('OK  取消关注后动态不再出现该视频')

    console.log('\n全部通过 ✅  TASK-E2E-03 互动（评论/点赞/关注/订阅动态）')
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
