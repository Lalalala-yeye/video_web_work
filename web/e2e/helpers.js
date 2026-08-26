import { Builder, By, Key, until } from 'selenium-webdriver'
import chrome from 'selenium-webdriver/chrome.js'

/** 前端地址。本机先 `npm run dev`（8787），后端 8081 也要开。 */
export const BASE_URL = process.env.E2E_BASE_URL || 'http://127.0.0.1:8787'

const WAIT_MS = 12000

export function uniqueUsername(prefix = 'e2e1') {
  return `${prefix}_${Date.now()}`
}

export async function createDriver() {
  const options = new chrome.Options()
  if (process.env.E2E_HEADLESS === '1') {
    options.addArguments('--headless=new')
  }
  options.addArguments('--window-size=1280,900', '--disable-gpu', '--no-sandbox')
  return new Builder().forBrowser('chrome').setChromeOptions(options).build()
}

export async function fillByPlaceholder(driver, placeholder, text) {
  const input = await driver.wait(
    until.elementLocated(By.css(`input[placeholder="${placeholder}"]`)),
    WAIT_MS
  )
  await input.click()
  await input.sendKeys(Key.CONTROL, 'a', Key.BACK_SPACE)
  await input.sendKeys(text)
  return input
}

export async function clickAuthSubmit(driver) {
  const btn = await driver.wait(
    until.elementLocated(By.css('.auth-card button[type="submit"]')),
    WAIT_MS
  )
  await btn.click()
}

export async function register(driver, username, password, confirmedPassword = password) {
  await driver.get(`${BASE_URL}/register`)
  await driver.wait(until.elementLocated(By.css('.auth-card')), WAIT_MS)
  await fillByPlaceholder(driver, '请输入账号', username)
  await fillByPlaceholder(driver, '至少6个字符', password)
  await fillByPlaceholder(driver, '请再次输入密码', confirmedPassword)
  await clickAuthSubmit(driver)
}

export async function login(driver, username, password) {
  await driver.get(`${BASE_URL}/login`)
  await driver.wait(until.elementLocated(By.css('.auth-card')), WAIT_MS)
  await fillByPlaceholder(driver, '请输入账号', username)
  await fillByPlaceholder(driver, '请输入密码', password)
  await clickAuthSubmit(driver)
}

export async function waitLoggedIn(driver) {
  await driver.wait(async () => {
    const url = await driver.getCurrentUrl()
    return !url.includes('/login')
  }, WAIT_MS)
  await driver.wait(until.elementLocated(By.css('.user-name')), WAIT_MS)
}

export async function logout(driver) {
  const trigger = await driver.wait(until.elementLocated(By.css('.user-trigger')), WAIT_MS)
  await trigger.click()
  const item = await driver.wait(
    until.elementLocated(By.xpath("//button[contains(., '退出当前账号')]")),
    WAIT_MS
  )
  await item.click()
  await driver.wait(until.elementLocated(By.xpath("//a[contains(., '登录')]")), WAIT_MS)
}

export async function waitMessageContains(driver, text) {
  await driver.wait(until.elementLocated(By.css('.el-message, .el-form-item__error')), WAIT_MS)
  const body = await driver.getPageSource()
  if (!body.includes(text)) {
    throw new Error(`页面未出现预期文案: ${text}`)
  }
}
