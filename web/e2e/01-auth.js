/**
 * 人1：账号相关 E2E（UC-01～03：注册、登录/登出、改资料与个人主页）。
 * 前置：后端 8081 + 前端 `npm run dev`（8787）+ 本机已装 Chrome。
 */
import assert from 'node:assert/strict'
import { By, Key, until } from 'selenium-webdriver'
import {
  BASE_URL,
  createDriver,
  uniqueUsername,
  register,
  login,
  waitLoggedIn,
  openProfile,
  logout,
  waitMessageContains,
} from './helpers.js'

async function run() {
  console.log('启动 Chrome…  目标:', BASE_URL)
  const driver = await createDriver()
  const username = uniqueUsername('e2e1')
  const password = '123456'

  try {
    await driver.get(BASE_URL)
    const home = await driver.wait(until.elementLocated(By.css('.logo-text, .page-container')), 12000)
    assert.ok(await home.isDisplayed(), '首页应能打开')
    console.log('OK  打开首页')

    await register(driver, username, password, '654321')
    await waitMessageContains(driver, '两次输入的密码不一致')
    console.log('OK  注册密码不一致')

    await register(driver, username, password)
    await driver.wait(until.urlContains('/login'), 12000)
    await driver.wait(until.elementLocated(By.xpath("//h1[contains(., '欢迎回来')]")), 12000)
    console.log('OK  注册成功并跳转登录页')

    await login(driver, username, 'wrong_pass')
    await waitMessageContains(driver, '账号或密码不正确')
    console.log('OK  错误密码提示')

    await login(driver, username, password)
    await waitLoggedIn(driver)
    const name = await driver.findElement(By.css('.user-name')).getText()
    assert.ok(name.length > 0, '登录后顶栏应显示昵称')
    console.log('OK  登录成功')

    await openProfile(driver)
    console.log('OK  个人中心')

    const nickInput = await driver.wait(
      until.elementLocated(By.css('.edit-form .el-form-item input')),
      12000
    )
    await nickInput.click()
    await nickInput.sendKeys(Key.CONTROL, 'a', Key.BACK_SPACE)
    const newNick = `E2E资料_${Date.now().toString().slice(-4)}`
    await nickInput.sendKeys(newNick)
    await driver.findElement(By.xpath("//button[contains(., '保存资料')]")).click()
    await waitMessageContains(driver, '资料已更新')
    console.log('OK  保存昵称', newNick)

    await driver.findElement(By.xpath("//a[contains(., '预览我的主页')]")).click()
    await driver.wait(until.urlContains('/user/'), 12000)
    await driver.wait(until.elementLocated(By.xpath(`//*[contains(., '${newNick}')]`)), 12000)
    console.log('OK  预览个人主页')

    await logout(driver)
    await driver.get(`${BASE_URL}/admin`)
    await driver.wait(until.urlContains('/login'), 12000)
    console.log('OK  未登录访问后台会跳转登录')

    console.log('\n01-auth 全部通过  用户名:', username)
  } finally {
    if (driver) {
      await driver.quit()
    }
  }
}

run().catch((err) => {
  console.error('\n01-auth 失败:', err?.message || err)
  if (err?.stack) {
    console.error(err.stack)
  }
  process.exitCode = 1
})
