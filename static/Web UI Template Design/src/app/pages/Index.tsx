import { Link } from 'react-router';
import {
  Home as HomeIcon,
  Video,
  Radio,
  Search,
  User,
  Upload,
  Rss,
  Palette,
  FileQuestion,
  LogIn,
  UserPlus
} from 'lucide-react';
import { Card } from '../components/ui/Card';

const pages = [
  {
    category: '核心页面',
    items: [
      { path: '/home', label: 'Home 视频首页', icon: HomeIcon, description: '视频卡片网格，4列布局，分页功能' },
      { path: '/video/demo-1', label: 'VideoDetail 视频播放页', icon: Video, description: '播放器、视频信息、评论区' },
      { path: '/live', label: 'LiveList 直播列表', icon: Radio, description: '直播卡片网格，直播中标签' },
      { path: '/live/demo-1', label: 'LiveRoom 直播间', icon: Radio, description: '直播画面、聊天互动区' },
    ],
  },
  {
    category: '功能页面',
    items: [
      { path: '/subscribe', label: 'Subscribe 订阅动态', icon: Rss, description: '关注UP主的最新内容' },
      { path: '/search?q=test', label: 'Search 搜索结果', icon: Search, description: '搜索框、Tab切换、结果列表' },
      { path: '/upload', label: 'Upload 视频上传', icon: Upload, description: '拖拽上传、表单、进度条' },
      { path: '/profile', label: 'Profile 个人中心', icon: User, description: '用户信息、播放历史、订阅' },
    ],
  },
  {
    category: '认证页面',
    items: [
      { path: '/login', label: 'Login 登录页', icon: LogIn, description: '账号密码登录表单' },
      { path: '/register', label: 'Register 注册页', icon: UserPlus, description: '账号注册表单' },
    ],
  },
  {
    category: '设计系统',
    items: [
      { path: '/design-system', label: 'DesignSystem 设计规范', icon: Palette, description: '颜色、字体、组件展示' },
      { path: '/empty-state', label: 'EmptyState 空状态页', icon: FileQuestion, description: '各种空状态示例' },
    ],
  },
];

export function Index() {
  return (
    <div className="min-h-screen bg-[#F5F7FA] py-12">
      <div className="max-w-[1280px] mx-auto px-4">
        {/* 头部 */}
        <div className="text-center mb-12">
          <div className="inline-flex items-center justify-center w-20 h-20 bg-[#409EFF] rounded-2xl mb-4">
            <span className="text-white font-bold text-4xl">D</span>
          </div>
          <h1 className="text-4xl font-bold text-[#303133] mb-3">doinb 视频平台</h1>
          <p className="text-lg text-[#909399] mb-2">完整的 UI 设计系统与原型</p>
          <p className="text-sm text-[#C0C4CC]">Desktop 1440px · Element Plus 风格 · 简体中文</p>
        </div>

        {/* 页面导航 */}
        {pages.map((section) => (
          <div key={section.category} className="mb-8">
            <h2 className="text-xl font-bold text-[#303133] mb-4">{section.category}</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              {section.items.map((page) => {
                const Icon = page.icon;
                return (
                  <Link key={page.path} to={page.path}>
                    <Card hoverable className="p-5 h-full">
                      <div className="flex items-start gap-3">
                        <div className="w-10 h-10 rounded-lg bg-[#ecf5ff] flex items-center justify-center flex-shrink-0">
                          <Icon size={20} className="text-[#409EFF]" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <h3 className="text-base font-medium text-[#303133] mb-1">
                            {page.label}
                          </h3>
                          <p className="text-sm text-[#909399] leading-5">
                            {page.description}
                          </p>
                        </div>
                      </div>
                    </Card>
                  </Link>
                );
              })}
            </div>
          </div>
        ))}

        {/* 设计规范说明 */}
        <Card className="p-8 mt-12">
          <h2 className="text-xl font-bold text-[#303133] mb-4">设计规范</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <div>
              <h3 className="text-sm font-medium text-[#606266] mb-2">主色调</h3>
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded bg-[#409EFF]" />
                <span className="text-sm text-[#303133]">#409EFF</span>
              </div>
            </div>
            <div>
              <h3 className="text-sm font-medium text-[#606266] mb-2">背景色</h3>
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded bg-[#F5F7FA] border border-[#DCDFE6]" />
                <span className="text-sm text-[#303133]">#F5F7FA</span>
              </div>
            </div>
            <div>
              <h3 className="text-sm font-medium text-[#606266] mb-2">圆角</h3>
              <p className="text-sm text-[#303133]">4px - 8px</p>
            </div>
            <div>
              <h3 className="text-sm font-medium text-[#606266] mb-2">栅格</h3>
              <p className="text-sm text-[#303133]">8px 间距系统</p>
            </div>
          </div>
        </Card>

        {/* 页脚 */}
        <div className="text-center mt-12 text-sm text-[#909399]">
          <p>共 {pages.reduce((acc, section) => acc + section.items.length, 0)} 个页面 · Element Plus 风格 · React + Tailwind CSS</p>
        </div>
      </div>
    </div>
  );
}
