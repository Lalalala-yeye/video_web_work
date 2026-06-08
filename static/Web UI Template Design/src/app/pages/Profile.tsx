import { useState } from 'react';
import { Edit, History, Heart, Star } from 'lucide-react';
import { TopNavigation } from '../components/TopNavigation';
import { Avatar } from '../components/ui/Avatar';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { VideoCard, VideoCardProps } from '../components/VideoCard';

// 模拟播放历史
const mockHistory: VideoCardProps[] = Array.from({ length: 8 }, (_, i) => ({
  id: `history-${i + 1}`,
  title: `播放历史视频 ${i + 1}：最近观看的内容`,
  cover: `https://images.unsplash.com/photo-${1500000000000 + i * 10000000}?w=400&h=225&fit=crop`,
  author: { name: `UP主${i + 1}`, avatar: undefined },
  views: Math.floor(Math.random() * 50000) + 1000,
  publishedAt: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
}));

export function Profile() {
  const [activeTab, setActiveTab] = useState<'history' | 'favorites' | 'subscriptions'>('history');

  const tabs = [
    { key: 'history' as const, label: '播放历史', icon: History },
    { key: 'favorites' as const, label: '我的收藏', icon: Heart },
    { key: 'subscriptions' as const, label: '我的订阅', icon: Star },
  ];

  return (
    <div className="min-h-screen bg-[#F5F7FA]">
      <TopNavigation
        isLoggedIn={true}
        userInfo={{ name: '测试用户', avatar: undefined }}
        onSearch={(query) => console.log('搜索:', query)}
      />

      <main className="max-w-[1280px] mx-auto px-4 pt-20 pb-8">
        {/* 用户信息卡片 */}
        <Card className="p-8 mb-6">
          <div className="flex items-start gap-6">
            <Avatar size={120} name="测试用户" />
            <div className="flex-1">
              <div className="flex items-center gap-4 mb-4">
                <h1 className="text-2xl font-bold text-[#303133]">测试用户</h1>
                <Button variant="secondary" size="medium">
                  <Edit size={16} className="mr-1" />
                  编辑资料
                </Button>
              </div>

              <div className="flex items-center gap-8 mb-4">
                <div>
                  <p className="text-2xl font-bold text-[#303133]">128</p>
                  <p className="text-sm text-[#909399]">关注</p>
                </div>
                <div>
                  <p className="text-2xl font-bold text-[#303133]">1.2万</p>
                  <p className="text-sm text-[#909399]">粉丝</p>
                </div>
                <div>
                  <p className="text-2xl font-bold text-[#303133]">45</p>
                  <p className="text-sm text-[#909399]">获赞</p>
                </div>
              </div>

              <p className="text-sm text-[#606266] leading-6">
                这是用户的个人简介，可以介绍自己的兴趣爱好、专业领域等。
              </p>
            </div>
          </div>
        </Card>

        {/* 标签页 */}
        <div className="flex items-center gap-1 mb-6 border-b border-[#EBEEF5]">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`flex items-center gap-2 px-6 py-3 text-base font-medium transition-colors relative ${
                  activeTab === tab.key
                    ? 'text-[#409EFF]'
                    : 'text-[#606266] hover:text-[#409EFF]'
                }`}
              >
                <Icon size={18} />
                {tab.label}
                {activeTab === tab.key && (
                  <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-[#409EFF]" />
                )}
              </button>
            );
          })}
        </div>

        {/* 内容区域 */}
        <div>
          {/* 播放历史 */}
          {activeTab === 'history' && (
            <div>
              <div className="flex items-center justify-between mb-4">
                <p className="text-sm text-[#909399]">共 {mockHistory.length} 个视频</p>
                <button className="text-sm text-[#409EFF] hover:text-[#66b1ff]">
                  清空历史
                </button>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                {mockHistory.map((video) => (
                  <VideoCard key={video.id} {...video} />
                ))}
              </div>
            </div>
          )}

          {/* 我的收藏 */}
          {activeTab === 'favorites' && (
            <div>
              <div className="flex items-center justify-between mb-4">
                <p className="text-sm text-[#909399]">共 32 个视频</p>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                {mockHistory.slice(0, 4).map((video) => (
                  <VideoCard key={video.id} {...video} />
                ))}
              </div>
            </div>
          )}

          {/* 我的订阅 */}
          {activeTab === 'subscriptions' && (
            <div>
              <div className="flex items-center justify-between mb-4">
                <p className="text-sm text-[#909399]">共 15 个UP主</p>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                {Array.from({ length: 8 }).map((_, i) => (
                  <Card key={i} className="p-4 text-center hover:shadow-md transition-shadow">
                    <Avatar size={80} name={`UP主${i + 1}`} className="mx-auto mb-3" />
                    <h3 className="text-base font-medium text-[#303133] mb-1">UP主{i + 1}</h3>
                    <p className="text-sm text-[#909399] mb-3">
                      {(Math.random() * 10).toFixed(1)}万 粉丝
                    </p>
                    <Button variant="secondary" size="small" className="w-full">
                      已关注
                    </Button>
                  </Card>
                ))}
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
