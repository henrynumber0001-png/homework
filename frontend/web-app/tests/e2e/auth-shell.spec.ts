import { expect, test } from '@playwright/test'

test('email login enters home and keeps the global navigation', async ({
  page,
}) => {
  await page.route('**/api/app/**', async (route) => {
    const url = new URL(route.request().url())
    let data: unknown = null

    if (url.pathname.endsWith('/auth/login/email')) {
      data = 'test-token'
    } else if (url.pathname.endsWith('/user/info')) {
      data = {
        accountNo: 'HW000001',
        displayName: 'Henry',
        avatar: null,
      }
    } else if (url.pathname.endsWith('/membership/center')) {
      data = {
        displayName: 'Henry',
        avatarUrl: null,
        membershipType: null,
        expiredTime: null,
        memberStatus: 0,
        baseFreezeExpireTime: null,
      }
    } else if (url.pathname.endsWith('/messages/unread-summary')) {
      data = {
        commentsAndMentions: 0,
        interactions: 0,
        system: 0,
        privateMessages: 0,
        total: 0,
      }
    } else if (url.pathname.endsWith('/home-page')) {
      data = {
        interviewQuestionBankVOList: [],
        certificateQuestionBankVOList: [],
        hotPostList: [],
      }
    } else if (url.pathname.endsWith('/learning-activity/heartbeat')) {
      data = null
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, message: 'success', data }),
    })
  })

  await page.goto('/login')
  await page.getByLabel('邮箱').fill('henry@example.com')
  await page.getByLabel('密码').fill('password')
  await page.getByRole('button', { name: '登录' }).click()

  await expect(page).toHaveURL(/\/home$/)
  await expect(page.getByText('Henry，欢迎回来')).toBeVisible()
  await expect(page.getByRole('link', { name: '首页' }).first()).toBeVisible()
  await expect(
    page.getByRole('link', { name: '面试题库' }).first(),
  ).toBeVisible()
})
