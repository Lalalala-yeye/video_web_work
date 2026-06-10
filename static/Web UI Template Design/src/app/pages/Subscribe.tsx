import { TopNavigation } from '../components/TopNavigation';
import { VideoCard, VideoCardProps } from '../components/VideoCard';
import { LiveCard, LiveCardProps } from '../components/LiveCard';
import { Tag } from '../components/ui/Tag';

// 模拟订阅动态数据
const mockFeed = [
  {
    type: 'video' as const,
    data: {
      id: 'video-1',
      title: '订阅UP主的最新视频：深入讲解前端开发技巧',
      cover: 'https://images.unsplash.com/photo-1587620962725-abab7fe55159?w=400&h=225&fit=crop',
      author: { name: 'UP主A', avatar: undefined },
      views: 25000,
      publishedAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
    } as VideoCardProps,
  },
  {
    type: 'live' as const,
    data: {
      id: 'live-1',
      title: '订阅主播正在直播：游戏实况解说',
      cover: 'https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=400&h=225&fit=crop',
      streamer: { name: '主播B', avatar: undefined },
      viewers: 8500,
      isLive: true,
    } as LiveCardProps,
  },
  {
    type: 'video' as const,
    data: {
      id: 'video-2',
      title: '订阅UP主分享：如何高效学习新技能',
      cover: 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=400&h=225&fit=crop',
      author: { name: 'UP主C', avatar: undefined },
      views: 42000,
      publishedAt: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString(),
    } as VideoCardProps,
  },
  {
    type: 'video' as const,
    data: {
      id: 'video-3',
      title: '精彩视频：音乐创作背后的故事',
      cover: 'https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=400&h=225&fit=crop',
      author: { name: 'UP主D', avatar: undefined },
      views: 18000,
      publishedAt: new Date(Date.now() - 12 * 60 * 60 * 1000).toISOString(),
    } as VideoCardProps,
  },
];

export function Subscribe() {
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
          <h1 className="text-2xl font-bold text-[#303133]">订阅动态</h1>
          <p className="text-sm text-[#909399] mt-1">关注UP主的最新内容</p>
        </div>

        {/* 筛选标签 */}
        <div className="flex items-center gap-3 mb-6">
          {['全部', '视频', '直播'].map((filter, i) => (
            <button
              key={filter}
              className={`px-4 py-2 rounded-full transition-colors ${
                i === 0
                  ? 'bg-[#409EFF] text-white'
                  : 'bg-white text-[#606266] hover:bg-[#ecf5ff] hover:text-[#409EFF] border border-[#DCDFE6]'
              }`}
            >
              {filter}
            </button>
          ))}
        </div>

        {/* 动态内容 */}
        <div className="space-y-6">
          {mockFeed.map((item, index) => (
            <div key={index}>
              {/* 时间标记 */}
              <div className="flex items-center gap-2 mb-3">
                <Tag type="info" size="small">
                  {index === 0 ? '最新' : index < 2 ? '今天' : '昨天'}
                </Tag>
              </div>

              {/* 内容卡片 */}
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                {item.type === 'video' ? (
                  <VideoCard {...item.data} />
                ) : (
                  <LiveCard {...item.data} />
                )}
              </div>
            </div>
          ))}
        </div>

        {/* 加载更多 */}
        <div className="mt-8 text-center">
          <button className="px-6 py-2 text-[#409EFF] hover:bg-[#ecf5ff] rounded transition-colors">
            加载更多
          </button>
        </div>
      </main>
    </div>
  );
}
