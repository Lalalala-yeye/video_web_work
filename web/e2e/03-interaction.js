/**
 * TASK-E2E-03 互动：评论、点赞、关注功能的点击成功反馈。
 * 前置：后端 8081 + 前端 `npm run dev`（8787）+ 本机 Chrome。
 * 覆盖：视频点赞/取消/点踩、发表评论、评论点赞、关注/取消关注。
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
const __dirname = path.dirname(fileURLToPath(import.meta.url))
const FIXTURE_VIDEO = path.join(__dirname, 'fixtures', 'test-video.mp4')

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

/** 轮询等待页面出现目标文案（旧提示未消失也不会误判，比 waitMessageContains 稳） */
async function waitToast(driver, text, timeoutMs = 8000) {
  await driver.wait(
    async () => (await driver.getPageSource()).includes(text),
    timeoutMs,
    `页面未出现预期文案: ${text}`
  )
}

/** 通过后端接口登录拿用户 id（仅用于测试数据准备，不经过页面） */
async function apiGetUserId(username, password) {
  const res = await fetch(`${API}/user/account/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  const body = await res.json()
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

async function run() {
  console.log('启动 Chrome…  目标:', BASE_URL)
  const driver = await createDriver()
  const password = '123456'
  const authorName = uniqueUsername('e2e3a') // 作者：上传视频并互动
  const viewerName = uniqueUsername('e2e3b') // 用户：关注作者

  try {
    /* ---------- 1. 作者注册并登录 ---------- */
    await register(driver, authorName, password)
    await driver.wait(until.urlContains('/login'), 12000)
    await login(driver, authorName, password)
    await waitLoggedIn(driver)
    console.log('OK  作者登录', authorName)

    /* ---------- 2. 上传一个"仅自己可见"的视频（无需管理员审核即可预览） ---------- */
    await driver.get(`${BASE_URL}/studio/upload`)
    await driver.wait(until.elementLocated(By.css('.page-title')), 12000)
    const fileInputs = await driver.findElements(By.css('input[type="file"]'))
    assert.ok(fileInputs.length >= 1, '上传页应有文件选择框')
    await fileInputs[0].sendKeys(FIXTURE_VIDEO) // 第一个 input 是视频
    const privateRadio = await driver.wait(
      until.elementLocated(By.xpath("//label[contains(., '仅自己可见')]")),
      12000
    )
    await privateRadio.click()
    const titleInput = await driver.wait(
      until.elementLocated(By.css('input[placeholder="请输入视频标题"]')),
      12000
    )
    await titleInput.sendKeys('E2E 互动测试视频')
    const submitBtn = await driver.wait(
      until.elementLocated(By.xpath("//button[contains(., '提交上传')]")),
      12000
    )
    await submitBtn.click()
    await waitToast(driver,'上传成功')
    console.log('OK  上传视频成功（仅自己可见）')
    await sleep(800)

    /* ---------- 3. 从创作中心进入视频详情 ---------- */
    await driver.get(`${BASE_URL}/studio/edit`)
    const firstItem = await driver.wait(until.elementLocated(By.css('.video-item')), 12000)
    await firstItem.click()
    const previewLink = await driver.wait(
      until.elementLocated(By.css('a[href^="/video/"]')),
      12000
    )
    await previewLink.click()
    await driver.wait(until.elementLocated(By.css('.video-title')), 12000)
    console.log('OK  打开视频详情页')

    /* ---------- 4. 视频点赞 → 取消 → 点踩（本轮新增的反馈） ---------- */
    const likeBtn = await driver.wait(
      until.elementLocated(By.xpath("//div[contains(@class,'video-actions')]//button[.//img[@alt='赞']]")),
      12000
    )
    await likeBtn.click()
    await waitToast(driver,'点赞成功')
    console.log('OK  视频点赞：出现"点赞成功"反馈')
    await shot(driver, '03-1-video-like')
    await sleep(600)

    await likeBtn.click()
    await waitToast(driver,'已取消')
    console.log('OK  取消点赞：出现"已取消"反馈')
    await sleep(600)

    const dislikeBtn = await driver.findElement(
      By.xpath("//div[contains(@class,'video-actions')]//button[.//img[@alt='踩']]")
    )
    await dislikeBtn.click()
    await waitToast(driver,'已点踩')
    console.log('OK  视频点踩：出现"已点踩"反馈')
    await sleep(600)

    /* ---------- 5. 发表评论 ---------- */
    const commentBox = await driver.findElement(By.css('textarea[placeholder="发表你的看法..."]'))
    await commentBox.sendKeys('E2E 自动化评论 e2e3')
    await driver.findElement(By.xpath("//button[contains(., '发表评论')]")).click()
    await waitToast(driver,'评论成功')
    await driver.wait(
      until.elementLocated(By.xpath("//*[contains(text(), 'E2E 自动化评论')]")),
      12000
    )
    console.log('OK  发表评论：出现"评论成功"反馈且评论上屏')
    await sleep(600)

    /* ---------- 6. 评论点赞（本轮新增的反馈） ---------- */
    const commentLike = await driver.wait(
      until.elementLocated(By.xpath("//div[contains(@class,'comment-item')]//button[.//img[@alt='赞']]")),
      12000
    )
    await commentLike.click()
    await waitToast(driver,'评论已点赞')
    console.log('OK  评论点赞：出现"评论已点赞"反馈')
    await shot(driver, '03-2-comment-like')

    /* ---------- 7. 换用户B，关注作者 ---------- */
    const authorId = await apiGetUserId(authorName, password)
    await logout(driver)
    await register(driver, viewerName, password)
    await driver.wait(until.urlContains('/login'), 12000)
    await login(driver, viewerName, password)
    await waitLoggedIn(driver)
    console.log('OK  用户B登录', viewerName)

    await driver.get(`${BASE_URL}/user/${authorId}`)
    const followBtn = await driver.wait(until.elementLocated(By.css('.follow-btn')), 12000)
    const textBefore = await followBtn.getText()
    assert.ok(textBefore.includes('关注') && !textBefore.includes('已关注'), '初始应为未关注')
    await followBtn.click()
    await waitToast(driver,'关注成功')
    console.log('OK  关注：出现"关注成功"反馈')
    const textAfter = await driver.findElement(By.css('.follow-btn')).getText()
    assert.ok(textAfter.includes('已关注'), '按钮状态应切换为"已关注"')
    await shot(driver, '03-3-following')
    await sleep(600)

    await driver.findElement(By.css('.follow-btn')).click()
    await waitToast(driver,'已取消关注')
    console.log('OK  取消关注：出现"已取消关注"反馈')

    console.log('\n全部通过 ✅  TASK-E2E-03 互动（评论/点赞/关注 点击成功反馈）')
  } finally {
    await driver.quit()
  }
}

run().catch((e) => {
  console.error('FAILED:', e.message)
  process.exit(1)
})
