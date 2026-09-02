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

export async function apiRegister(username, password) {
  const res = await fetch(`${API_BASE}/user/account/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password, confirmedPassword: password }),
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
  await driver.executeScript('arguments[0].click()', btn)
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

/** 点顶栏文字导航。失败即视为「点不进去」，不要改成 driver.get 绕过。 */
export async function clickNav(driver, label) {
  const xpath = `//nav[contains(@class,'nav-links')]//a[normalize-space()='${label}']`
  const el = await driver.wait(
    until.elementLocated(By.xpath(xpath)),
    WAIT_MS,
    `顶栏没有「${label}」。未登录时没有创作中心；或导航被挡住`
  )
  await driver.executeScript('arguments[0].scrollIntoView({block:"center"}); arguments[0].click();', el)
}

export async function openHomeViaNav(driver) {
  await clickNav(driver, '首页')
  await driver.wait(
    async () => {
      const url = await driver.getCurrentUrl()
      const path = new URL(url).pathname
      return path === '/' || path === ''
    },
    WAIT_MS,
    '点击首页后应回到 /'
  )
}

export async function openLiveViaNav(driver) {
  await clickNav(driver, '直播')
  await driver.wait(
    async () => {
      const path = new URL(await driver.getCurrentUrl()).pathname
      return path === '/live'
    },
    WAIT_MS,
    '点击直播后应进入 /live 列表（不是某个直播间）'
  )
}

/** 用顶栏搜索框，覆盖 UC-12；不要改成 driver.get('/search') 绕过。 */
export async function searchViaTopNav(driver, keyword) {
  // 停播/上传成功等 toast 会挡住顶栏搜索框
  await driver.wait(
    async () => (await driver.findElements(By.css('.el-message, .el-message-box'))).length === 0,
    WAIT_MS
  ).catch(() => {})
  const input = await driver.wait(
    until.elementLocated(By.css('.top-nav .search-form input')),
    WAIT_MS,
    '顶栏没有搜索框（登录/注册页没有顶栏）'
  )
  await driver.executeScript(
    `const el = arguments[0];
     el.scrollIntoView({block:'center'});
     el.focus();
     el.click();`,
    input
  )
  await setVueInputValue(driver, input, keyword)
  const btn = await driver.findElement(By.css('.top-nav .search-form .search-btn'))
  await driver.executeScript('arguments[0].click()', btn)
  await driver.wait(
    async () => {
      const url = await driver.getCurrentUrl()
      try {
        const parsed = new URL(url)
        return parsed.pathname.startsWith('/search') && parsed.searchParams.get('keyword') === keyword
      } catch {
        return false
      }
    },
    WAIT_MS,
    `顶栏搜索后应进入 /search?keyword=${keyword}`
  )
}

export async function openStudioViaNav(driver) {
  await clickNav(driver, '创作中心')
  await driver.wait(until.urlContains('/studio'), WAIT_MS, '点击创作中心后应进入 /studio')
  await driver.wait(until.elementLocated(By.css('.sidebar-title')), WAIT_MS)
}

export async function openStudioSidebar(driver, label) {
  const btn = await driver.wait(
    until.elementLocated(By.xpath(`//aside[contains(@class,'studio-sidebar')]//button[contains(., '${label}')]`)),
    WAIT_MS
  )
  await driver.executeScript('arguments[0].click()', btn)
}

/**
 * 从顶栏进创作中心上传。fileInputs[0] 视频，[1] 封面。
 * 返回视频 id（来自跳转的 /studio/edit/:id）。
 */
export async function uploadStudioVideo(driver, { title, videoPath, coverPath, visibility = 'public' }) {
  await openStudioViaNav(driver)
  await driver.wait(until.urlContains('/studio/upload'), WAIT_MS)
  await driver.wait(until.elementLocated(By.css('input[placeholder="请输入视频标题"]')), WAIT_MS)
  const fileInputs = await driver.findElements(By.css('input[type="file"]'))
  if (fileInputs.length < 1) {
    throw new Error('上传页没有文件选择框')
  }
  await fileInputs[0].sendKeys(videoPath)
  if (coverPath && fileInputs[1]) {
    await fileInputs[1].sendKeys(coverPath)
  }
  const titleInput = await driver.findElement(By.css('input[placeholder="请输入视频标题"]'))
  await setVueInputValue(driver, titleInput, title)
  if (visibility === 'private') {
    const privateRadio = await driver.wait(
      until.elementLocated(By.xpath("//label[contains(., '仅自己可见')]")),
      WAIT_MS
    )
    await privateRadio.click()
  }
  const submit = await driver.findElement(By.xpath("//button[contains(., '提交上传')]"))
  await driver.executeScript('arguments[0].click()', submit)
  await waitMessageContains(driver, '上传成功')
  await driver.wait(until.urlContains('/studio/edit'), WAIT_MS)
  const url = await driver.getCurrentUrl()
  const match = url.match(/\/studio\/edit\/(\d+)/)
  return match ? match[1] : null
}

export async function clickVideoCard(driver, videoId) {
  const card = await driver.wait(
    until.elementLocated(By.css(`a.video-card[href="/video/${videoId}"]`)),
    WAIT_MS,
    `首页/列表没有视频卡片 /video/${videoId}`
  )
  await driver.executeScript('arguments[0].scrollIntoView({block:"center"}); arguments[0].click();', card)
  await driver.wait(until.urlContains(`/video/${videoId}`), WAIT_MS, `点击卡片后应进入 /video/${videoId}`)
}

export async function assertVideoCardCover(driver, videoId) {
  const result = await driver.executeScript(
    `const id = arguments[0];
     const card = document.querySelector('a.video-card[href="/video/' + id + '"]');
     if (!card) return { ok: false, message: '没有卡片' };
     const img = card.querySelector('img.cover');
     if (!img) return { ok: false, message: '卡片没有封面图（coverUrl 为空或 404）' };
     if (!img.complete || img.naturalWidth === 0) {
       return { ok: false, message: '封面未加载 src=' + img.src };
     }
     return { ok: true, src: img.src };`,
    String(videoId)
  )
  if (!result?.ok) {
    throw new Error(`视频 ${videoId} 封面不可用: ${result?.message || '未知'}`)
  }
}

export async function assertVideoPlayerMedia(driver) {
  const result = await driver.executeAsyncScript(
    `const done = arguments[arguments.length - 1];
     const v = document.querySelector('video.player');
     if (!v || !v.getAttribute('src')) {
       done({ ok: false, message: '详情页没有 video.player 或 src' });
       return;
     }
     const src = v.currentSrc || v.src;
     fetch(src, { method: 'GET', headers: { Range: 'bytes=0-1' } })
       .then(function (r) {
         done({ ok: r.ok || r.status === 206, status: r.status, src: src });
       })
       .catch(function (e) {
         done({ ok: false, message: String(e), src: src });
       });`
  )
  if (!result?.ok) {
    throw new Error(
      `播放地址不可用（${result?.status || ''} ${result?.message || ''} src=${result?.src || ''}）`
    )
  }
}

export async function waitMessageContains(driver, text, timeoutMs = WAIT_MS) {
  await driver.wait(
    async () => (await driver.getPageSource()).includes(text),
    timeoutMs,
    `页面未出现预期文案: ${text}`
  )
}

/**
 * 待审列表找到该视频并点「通过」。
 * Element Plus 操作列 fixed 后，标题和按钮不在同一行 DOM 里，不能用「标题所在 tr 里的按钮」。
 */
export async function approvePendingVideo(driver, videoId, title) {
  await driver.wait(until.elementLocated(By.xpath("//h1[contains(., '待审视频')]")), WAIT_MS)
  const href = `/admin/preview/${videoId}`
  try {
    await driver.wait(until.elementLocated(By.css(`a[href="${href}"]`)), 15000)
  } catch (err) {
    const src = await driver.getPageSource()
    let hint = '待审表里没有该视频'
    if (src.includes('需要管理员权限')) hint = '管理员 JWT 未生效，待审接口拒绝访问'
    else if (src.includes('暂无待审视频')) hint = '待审列表为空'
    throw new Error(`${hint}（${title || videoId}）: ${err.message}`)
  }
  const clicked = await driver.executeScript(
    `const href = arguments[0];
     const link = document.querySelector('a[href="' + href + '"]');
     if (!link) return false;
     const tr = link.closest('tr');
     if (!tr || !tr.parentElement) return false;
     const idx = Array.prototype.indexOf.call(tr.parentElement.children, tr);
     const fixedRows = document.querySelectorAll('.el-table__fixed-right tbody tr, .el-table__fixed-body-wrapper tbody tr');
     const row = (fixedRows && fixedRows[idx]) || tr;
     const btn = Array.from(row.querySelectorAll('button')).find(function (b) {
       return (b.textContent || '').indexOf('通过') >= 0;
     });
     if (!btn) return false;
     btn.scrollIntoView({ block: 'center' });
     btn.click();
     return true;`,
    href
  )
  if (!clicked) {
    throw new Error(`待审表找到了视频 ${videoId}，但没点到「通过」`)
  }
}

/** 写入 Element Plus / Vue 绑定的 input，避免 Selenium 改 DOM 但 v-model 仍是空。 */
export async function setVueInputValue(driver, element, value) {
  await driver.executeScript(
    `const el = arguments[0];
     const val = arguments[1];
     const proto = el.tagName === 'TEXTAREA'
       ? window.HTMLTextAreaElement.prototype
       : window.HTMLInputElement.prototype;
     const desc = Object.getOwnPropertyDescriptor(proto, 'value');
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
