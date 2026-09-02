/**
 * 人1：账号 + 顶栏导航（UC-01～03，并点进 UC-04/06/08/09 入口）。
 * 15 个用例由 e2e:ci 串联覆盖：01 注册登录资料；02 浏览播放搜索直播；
 * 03 评论赞踩关注；04 创作中心直播；05 私信通知举报复审。
 */
import assert from 'node:assert/strict'
import { By, until } from 'selenium-webdriver'
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
  setVueInputValue,
  clickNav,
  openStudioViaNav,
  openLiveViaNav,
  openHomeViaNav,
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

    await openStudioViaNav(driver)
    await driver.wait(until.elementLocated(By.xpath("//h1[contains(., '上传视频')]")), 12000)
    console.log('OK  UC-06 顶栏点进创作中心')

    await openLiveViaNav(driver)
    await driver.wait(until.elementLocated(By.css('.page-container')), 12000)
    console.log('OK  UC-08 顶栏点进直播')

    await clickNav(driver, '关注')
    await driver.wait(until.urlContains('/subscribe'), 12000)
    console.log('OK  UC-09 顶栏点进关注')

    await openHomeViaNav(driver)
    console.log('OK  UC-04 顶栏回到首页')

    await openProfile(driver)
    console.log('OK  个人中心')

    const nickInput = await driver.wait(
      until.elementLocated(By.css('.edit-form .el-form-item .el-input__wrapper input, .edit-form .el-form-item input.el-input__inner')),
      12000
    )
    await driver.wait(
      async () => {
        const v = await nickInput.getAttribute('value')
        return Boolean(v && v.trim())
      },
      12000,
      '昵称应已从 /user/personal/info 加载，不能对着空表单点保存'
    )
    const newNick = `E2E资料_${Date.now().toString().slice(-4)}`
    await nickInput.click()
    await setVueInputValue(driver, nickInput, newNick)
    const saveBtn = await driver.findElement(By.xpath("//button[contains(., '保存资料')]"))
    await driver.executeScript('arguments[0].click()', saveBtn)
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
