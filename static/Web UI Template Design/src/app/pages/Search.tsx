import { useState } from 'react';
import { useSearchParams } from 'react-router';
import { TopNavigation } from '../components/TopNavigation';
import { VideoCard, VideoCardProps } from '../components/VideoCard';
import { LiveCard, LiveCardProps } from '../components/LiveCard';
import { Avatar } from '../components/ui/Avatar';
import { Card } from '../components/ui/Card';
import { Empty } from '../components/ui/Empty';

// 模拟搜索结果
const mockVideoResults: VideoCardProps[] = Array.from({ length: 6 }, (_, i) => ({
  id: `search-video-${i + 1}`,
  title: `搜索结果视频 ${i + 1}：与关键词相关的精彩内容`,
  cover: `https://images.unsplash.com/photo-${1500000000000 + i * 10000000}?w=400&h=225&fit=crop`,
  author: { name: `UP主${i + 1}`, avatar: undefined },
  views: Math.floor(Math.random() * 50000) + 1000,
  publishedAt: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString(),
}));

const mockLiveResults: LiveCardProps[] = Array.from({ length: 4 }, (_, i) => ({
  id: `search-live-${i + 1}`,
  title: `直播搜索结果 ${i + 1}：相关的直播内容`,
  cover: `https://images.unsplash.com/photo-${1600000000000 + i * 10000000}?w=400&h=225&fit=crop`,
  streamer: { name: `主播${i + 1}`, avatar: undefined },
  viewers: Math.floor(Math.random() * 10000) + 500,
  isLive: i < 2,
}));

const mockUserResults = Array.from({ length: 5 }, (_, i) => ({
  id: `user-${i + 1}`,
  name: `用户${i + 1}`,
  avatar: undefined,
  followers: Math.floor(Math.random() * 100000) + 1000,
  videos: Math.floor(Math.random() * 200) + 10,
}));

export function Search() {
  const [searchParams] = useSearchParams();
  const query = searchParams.get('q') || '';
  const [activeTab, setActiveTab] = useState<'video' | 'live' | 'user'>('video');

  const tabs = [
    { key: 'video' as const, label: '视频', count: mockVideoResults.length },
    { key: 'live' as const, label: '直播', count: mockLiveResults.length },
    { key: 'user' as const, label: '用户', count: mockUserResults.length },
  ];

  return (
    <div className="min-h-screen bg-[#F5F7FA]">
      <TopNavigation
        isLoggedIn={true}
        userInfo={{ name: '测试用户', avatar: undefined }}
        onSearch={(q) => console.log('搜索:', q)}
      />

      <main className="max-w-[1280px] mx-auto px-4 pt-20 pb-8">
        {/* 搜索信息 */}
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-[#303133]">
            {query ? `"${query}" 的搜索结果` : '搜索结果'}
          </h1>
          <p className="text-sm text-[#909399] mt-1">
            找到 {mockVideoResults.length + mockLiveResults.length + mockUserResults.length} 条相关内容
          </p>
        </div>

        {/* 标签页 */}
        <div className="flex items-center gap-1 mb-6 border-b border-[#EBEEF5]">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`px-6 py-3 text-base font-medium transition-colors relative ${
                activeTab === tab.key
                  ? 'text-[#409EFF]'
                  : 'text-[#606266] hover:text-[#409EFF]'
              }`}
            >
              {tab.label}
              <span className="ml-1 text-sm text-[#909399]">({tab.count})</span>
              {activeTab === tab.key && (
                <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-[#409EFF]" />
              )}
            </button>
          ))}
        </div>

        {/* 内容区域 */}
        <div>
          {/* 视频结果 */}
          {activeTab === 'video' && (
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
              {mockVideoResults.length > 0 ? (
                mockVideoResults.map((video) => <VideoCard key={video.id} {...video} />)
              ) : (
                <div className="col-span-full">
                  <Empty image="noSearch" description="没有找到相关视频" />
                </div>
              )}
            </div>
          )}

          {/* 直播结果 */}
          {activeTab === 'live' && (
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
              {mockLiveResults.length > 0 ? (
                mockLiveResults.map((live) => <LiveCard key={live.id} {...live} />)
              ) : (
                <div className="col-span-full">
                  <Empty image="noSearch" description="没有找到相关直播" />
                </div>
              )}
            </div>
          )}

          {/* 用户结果 */}
          {activeTab === 'user' && (
            <div className="space-y-3">
              {mockUserResults.length > 0 ? (
                mockUserResults.map((user) => (
                  <Card key={user.id} className="p-4 hover:shadow-md transition-shadow cursor-pointer">
                    <div className="flex items-center gap-4">
                      <Avatar size={64} name={user.name} src={user.avatar} />
                      <div className="flex-1">
                        <h3 className="text-base font-medium text-[#303133] mb-1">{user.name}</h3>
                        <div className="flex items-center gap-4 text-sm text-[#909399]">
                          <span>{(user.followers / 10000).toFixed(1)}万 粉丝</span>
                          <span>{user.videos} 个视频</span>
                        </div>
                      </div>
                      <button className="px-6 py-2 bg-[#409EFF] text-white rounded hover:bg-[#66b1ff] transition-colors">
                        关注
                      </button>
                    </div>
                  </Card>
                ))
              ) : (
                <Empty image="noSearch" description="没有找到相关用户" />
              )}
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
