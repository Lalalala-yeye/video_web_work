/**
 * TASK-E2E-04 创作中心和直播。
 * 前置：后端 8081 + 前端 `npm run dev`（8787）+ 本机 Chrome。
 * 覆盖：未登录进创作中心、上传校验、私密上传、修改视频、创建直播间、
 *       屏幕分享未完善提示、OBS 开播（不要求真推流）、直播列表与进入直播间。
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
  waitMessageContains,
} from './helpers.js'

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

async function run() {
  assert.ok(fs.existsSync(FIXTURE_VIDEO), `缺少测试视频: ${FIXTURE_VIDEO}`)
  console.log('启动 Chrome…  目标:', BASE_URL)
  const driver = await createDriver()
  const password = '123456'
  const username = uniqueUsername('e2e4')
  const liveTitle = `E2E直播_${Date.now()}`
  const videoTitle = `E2E创作_${Date.now()}`

  try {
    /* ---------- 1. 未登录访问创作中心 → 请先登录 ---------- */
    await driver.get(`${BASE_URL}/studio`)
    await waitMessageContains(driver, '请先登录')
    await driver.wait(until.urlContains('/login'), 12000)
    console.log('OK  未登录访问创作中心会跳转登录')

    /* ---------- 2. 注册并登录 ---------- */
    await register(driver, username, password)
    await driver.wait(until.urlContains('/login'), 12000)
    await login(driver, username, password)
    await waitLoggedIn(driver)
    console.log('OK  登录', username)

    /* ---------- 3. 创作中心侧栏 ---------- */
    await driver.get(`${BASE_URL}/studio/upload`)
    await driver.wait(until.elementLocated(By.css('.page-title')), 12000)
    const sidebar = await driver.wait(until.elementLocated(By.css('.sidebar-title')), 12000)
    assert.equal((await sidebar.getText()).trim(), '创作中心')
    await driver.wait(until.elementLocated(By.xpath("//button[contains(., '我的直播')]")), 12000)
    await driver.wait(until.elementLocated(By.xpath("//h1[contains(., '上传视频')]")), 12000)
    console.log('OK  创作中心侧栏（上传 / 修改 / 直播）')

    /* ---------- 4. 上传空提交校验 ---------- */
    await clickXpath(driver, "//button[contains(., '提交上传')]")
    await waitToast(driver, '请填写标题')
    console.log('OK  空标题提示「请填写标题」')

    const titleInput = await driver.wait(
      until.elementLocated(By.css('input[placeholder="请输入视频标题"]')),
      12000
    )
    await titleInput.sendKeys(videoTitle)
    await clickXpath(driver, "//button[contains(., '提交上传')]")
    await waitToast(driver, '请选择视频文件')
    console.log('OK  未选文件提示「请选择视频文件」')

    /* ---------- 5. 仅自己可见上传 ---------- */
    const fileInputs = await driver.findElements(By.css('input[type="file"]'))
    assert.ok(fileInputs.length >= 1, '上传页应有文件选择框')
    await fileInputs[0].sendKeys(FIXTURE_VIDEO)
    const privateRadio = await driver.wait(
      until.elementLocated(By.xpath("//label[contains(., '仅自己可见')]")),
      12000
    )
    await privateRadio.click()
    await clickXpath(driver, "//button[contains(., '提交上传')]")
    await waitToast(driver, '上传成功')
    await driver.wait(until.urlContains('/studio/edit'), 12000)
    console.log('OK  私密视频上传成功')
    await shot(driver, '04-1-upload')
    await sleep(800)

    /* ---------- 6. 修改视频（后端成功文案是「更新成功…」） ---------- */
    await driver.wait(until.elementLocated(By.xpath("//h1[contains(., '修改视频')]")), 12000)
    const firstItem = await driver.wait(until.elementLocated(By.css('.video-item')), 12000)
    await firstItem.click()
    const desc = await driver.wait(until.elementLocated(By.css('textarea.el-textarea__inner')), 12000)
    await desc.sendKeys('E2E 修改简介')
    await clickXpath(driver, "//button[contains(., '保存修改')]")
    await waitToast(driver, '更新成功', 12000)
    console.log('OK  修改视频并保存成功')
    await shot(driver, '04-2-edit')

    /* ---------- 7. 我的直播：空标题 / 创建 ---------- */
    await driver.get(`${BASE_URL}/studio/live`)
    await driver.wait(until.elementLocated(By.xpath("//h1[contains(., '我的直播')]")), 12000)
    await clickXpath(driver, "//button[normalize-space()='创建']")
    await waitToast(driver, '请填写直播间标题')
    console.log('OK  空直播标题提示')

    const liveInput = await driver.wait(
      until.elementLocated(By.css('input[placeholder="直播间标题"]')),
      12000
    )
    await liveInput.sendKeys(liveTitle)
    await clickXpath(driver, "//button[normalize-space()='创建']")
    await waitToast(driver, '创建成功')
    await driver.wait(until.elementLocated(By.xpath(`//*[contains(text(), '${liveTitle}')]`)), 12000)
    console.log('OK  创建直播间', liveTitle)
    await shot(driver, '04-3-live-create')

    /* ---------- 8. 屏幕分享开播 → 功能未完善（不要求真推流） ---------- */
    const shareBtn = await driver.wait(
      until.elementLocated(
        By.xpath(`//div[contains(@class,'el-table')]//tr[contains(., '${liveTitle}')]//button[contains(., '屏幕分享开播')]`)
      ),
      12000
    )
    await shareBtn.click()
    await driver.wait(until.elementLocated(By.css('.el-message-box')), 12000)
    await waitToast(driver, '浏览器屏幕分享功能尚未完善')
    await clickXpath(driver, "//button[contains(., '知道了')]")
    console.log('OK  屏幕分享开播提示功能未完善')

    /* ---------- 9. OBS 开播（只调开播接口，不启 OBS） ---------- */
    const obsBtn = await driver.wait(
      until.elementLocated(
        By.xpath(`//div[contains(@class,'el-table')]//tr[contains(., '${liveTitle}')]//button[contains(., 'OBS 开播')]`)
      ),
      12000
    )
    await obsBtn.click()
    await waitToast(driver, '已开播，请在 OBS 开始推流')
    await driver.wait(
      until.elementLocated(
        By.xpath(`//div[contains(@class,'el-table')]//tr[contains(., '${liveTitle}')]//*[contains(., '直播中')]`)
      ),
      12000
    )
    console.log('OK  OBS 开播成功（未做真实推流）')
    await shot(driver, '04-4-obs-start')

    /* ---------- 10. 公开直播列表 + 进入直播间 ---------- */
    await driver.get(`${BASE_URL}/live`)
    await driver.wait(until.elementLocated(By.xpath("//*[contains(., '发现正在进行的精彩直播')]")), 12000)
    const liveCard = await driver.wait(
      until.elementLocated(By.xpath(`//a[contains(@class,'live-card') and contains(., '${liveTitle}')]`)),
      12000
    )
    await liveCard.click()
    await driver.wait(until.urlMatches(/\/live\/\d+/), 12000)
    await driver.wait(until.elementLocated(By.xpath(`//*[contains(., '${liveTitle}')]`)), 12000)
    console.log('OK  进入直播间')
    await shot(driver, '04-5-live-room')

    /* ---------- 11. 停播 ---------- */
    await driver.get(`${BASE_URL}/studio/live`)
    const stopBtn = await driver.wait(
      until.elementLocated(
        By.xpath(`//div[contains(@class,'el-table')]//tr[contains(., '${liveTitle}')]//button[contains(., '停播')]`)
      ),
      12000
    )
    await stopBtn.click()
    await waitToast(driver, '已停播')
    console.log('OK  停播')

    console.log('\n全部通过 ✅  TASK-E2E-04 创作中心和直播')
  } finally {
    await driver.quit()
  }
}

run().catch((e) => {
  console.error('FAILED:', e.message)
  if (e.stack) console.error(e.stack)
  process.exit(1)
})
