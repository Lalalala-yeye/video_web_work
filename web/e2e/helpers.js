import { Builder, By, until } from 'selenium-webdriver'
import chrome from 'selenium-webdriver/chrome.js'

/** 前端地址。本机先 `npm run dev`（8787），后端 8081 也要开。 */
export const BASE_URL = process.env.E2E_BASE_URL || 'http://127.0.0.1:8787'
export const API_BASE = process.env.E2E_API || 'http://127.0.0.1:8081'

const WAIT_MS = 12000

export function uniqueUsername(prefix = 'e2e1') {
  return `${prefix}_${Date.now()}`
}

export async function createDriver() {
  const options = new chrome.Options()
  if (process.env.E2E_HEADLESS === '1') {
    options.addArguments('--headless=new')
  }
  // CI 容器 /dev/shm 很小，不加这条 Chrome 容易静默崩
  options.addArguments('--window-size=1280,900', '--disable-gpu', '--no-sandbox', '--disable-dev-shm-usage')
  if (process.env.CHROME_BIN) {
    options.setChromeBinaryPath(process.env.CHROME_BIN)
  }
  options.setPageLoadStrategy('eager')
  const driver = await new Builder().forBrowser('chrome').setChromeOptions(options).build()
  await driver.manage().setTimeouts({ implicit: 0, pageLoad: 20000, script: 20000 })
  return driver
}

export async function apiLogin(username, password) {
  const res = await fetch(`${API_BASE}/user/account/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  return res.json()
}

/** 多账号切换。登录页本身由 01-auth 覆盖；「退出当前账号」会切到列表里下一个，不能当登出。 */
export async function injectSession(driver, username, password) {
  const body = await apiLogin(username, password)
  if (body.code !== 200 || !body.data?.token || !body.data?.user) {
    throw new Error(`切换账号失败 ${username}: ${body.message || body.code}`)
  }
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
  await driver.wait(
    until.elementLocated(By.css('.user-name')),
    20000,
    `注入登录后顶栏应显示用户名: ${username}`
  )
}

export async function fillByPlaceholder(driver, placeholder, text) {
  const input = await driver.wait(
    until.elementLocated(By.css(`input[placeholder="${placeholder}"]`)),
    WAIT_MS
  )
  await input.click()
  await setVueInputValue(driver, input, text)
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

/** 登录后走顶栏进个人中心，避免 driver.get 整页刷新在 CI 里丢掉 sessionStorage。 */
export async function openProfile(driver) {
  const trigger = await driver.wait(until.elementLocated(By.css('.user-trigger')), WAIT_MS)
  await trigger.click()
  const link = await driver.wait(
    until.elementLocated(By.css('a.menu-item[href="/profile"]')),
    WAIT_MS
  )
  await link.click()
  try {
    await driver.wait(until.urlContains('/profile'), WAIT_MS)
    await driver.wait(until.elementLocated(By.css('.edit-form')), WAIT_MS)
  } catch (err) {
    const url = await driver.getCurrentUrl()
    throw new Error(`未能打开个人中心（当前 ${url}）: ${err.message}`)
  }
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

export async function waitMessageContains(driver, text, timeoutMs = WAIT_MS) {
  await driver.wait(
    async () => (await driver.getPageSource()).includes(text),
    timeoutMs,
    `页面未出现预期文案: ${text}`
  )
}

/** 写入 Element Plus / Vue 绑定的 input，避免 Selenium 改 DOM 但 v-model 仍是空。 */
export async function setVueInputValue(driver, element, value) {
  await driver.executeScript(
    `const el = arguments[0];
     const val = arguments[1];
     const desc = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value');
     if (desc && desc.set) {
       desc.set.call(el, val);
     } else {
       el.value = val;
     }
     el.dispatchEvent(new Event('input', { bubbles: true }));
     el.dispatchEvent(new Event('change', { bubbles: true }));`,
    element,
    value
  )
}

/**
 * 测完后删掉该账号下的全部视频（库记录 + uploads 文件）。
 * 失败不抛错，避免掩盖用例本身的错误。
 */
export async function cleanupUserVideos(username, password) {
  if (!username || !password) return
  try {
    const loginRes = await fetch(`${API_BASE}/user/account/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    })
    const loginBody = await loginRes.json()
    if (loginBody.code !== 200 || !loginBody.data?.token) {
      return
    }
    const token = loginBody.data.token
    const headers = { Authorization: token }
    const listRes = await fetch(`${API_BASE}/video/my/list?page=1&size=50`, { headers })
    const listBody = await listRes.json()
    const records = listBody.data?.records || []
    for (const video of records) {
      const delRes = await fetch(`${API_BASE}/video/delete`, {
        method: 'POST',
        headers: {
          Authorization: token,
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams({ id: String(video.id) }).toString(),
      })
      const delBody = await delRes.json().catch(() => ({}))
      if (delBody.code === 200) {
        console.log('已删除测试视频', video.id, video.title)
      } else {
        console.warn('删除测试视频失败', video.id, delBody.message || delRes.status)
      }
    }
  } catch (err) {
    console.warn('清理测试视频时出错:', err?.message || err)
  }
}
