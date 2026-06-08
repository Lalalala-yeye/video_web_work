import { TopNavigation } from '../components/TopNavigation';
import { LiveCard, LiveCardProps } from '../components/LiveCard';

// 模拟数据
const mockLiveStreams: LiveCardProps[] = Array.from({ length: 12 }, (_, i) => ({
  id: `live-${i + 1}`,
  title: `正在直播：${['游戏解说', '学习分享', '音乐演奏', '技术教程'][i % 4]} - 第${i + 1}期`,
  cover: `https://images.unsplash.com/photo-${1600000000000 + i * 10000000}?w=400&h=225&fit=crop`,
  streamer: {
    name: `主播${i + 1}`,
    avatar: undefined,
  },
  viewers: Math.floor(Math.random() * 50000) + 500,
  isLive: i < 8, // 前8个是直播中，后4个是离线
}));

export function LiveList() {
  return (
    <div className="min-h-screen bg-[#F5F7FA]">
      <TopNavigation
        isLoggedIn={true}
        userInfo={{ name: '测试用户', avatar: undefined }}
        onSearch={(query) => console.log('搜索:', query)}
      />

      <main className="max-w-[1280px] mx-auto px-4 pt-20 pb-8">
        {/* 页面标题 */}
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-[#303133]">直播</h1>
          <p className="text-sm text-[#909399] mt-1">发现精彩的直播内容</p>
        </div>

        {/* 分类标签 */}
        <div className="flex items-center gap-3 mb-6 overflow-x-auto pb-2">
          {['全部', '游戏', '学习', '音乐', '科技', '生活', '娱乐'].map((category, i) => (
            <button
              key={category}
              className={`px-4 py-2 rounded-full whitespace-nowrap transition-colors ${
                i === 0
                  ? 'bg-[#409EFF] text-white'
                  : 'bg-white text-[#606266] hover:bg-[#ecf5ff] hover:text-[#409EFF] border border-[#DCDFE6]'
              }`}
            >
              {category}
            </button>
          ))}
        </div>

        {/* 直播网格 */}
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          {mockLiveStreams.map((stream) => (
            <LiveCard key={stream.id} {...stream} />
          ))}
        </div>
      </main>
    </div>
  );
}
