import { useState } from 'react';
import { TopNavigation } from '../components/TopNavigation';
import { VideoCard, VideoCardProps } from '../components/VideoCard';
import { Button } from '../components/ui/Button';

// 模拟数据
const mockVideos: VideoCardProps[] = Array.from({ length: 12 }, (_, i) => ({
  id: `video-${i + 1}`,
  title: `这是一个非常有趣的视频标题，涵盖了很多精彩内容 ${i + 1}`,
  cover: `https://images.unsplash.com/photo-${1500000000000 + i * 10000000}?w=400&h=225&fit=crop`,
  author: {
    name: `UP主${i + 1}`,
    avatar: undefined,
  },
  views: Math.floor(Math.random() * 100000) + 1000,
  publishedAt: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString(),
}));

export function Home() {
  const [currentPage, setCurrentPage] = useState(1);
  const videosPerPage = 12;

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
          <h1 className="text-2xl font-bold text-[#303133]">推荐视频</h1>
          <p className="text-sm text-[#909399] mt-1">为你精选的优质内容</p>
        </div>

        {/* 视频网格 */}
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
          {mockVideos.map((video) => (
            <VideoCard key={video.id} {...video} />
          ))}
        </div>

        {/* 分页 */}
        <div className="mt-8 flex items-center justify-center gap-2">
          <Button
            variant="secondary"
            size="medium"
            disabled={currentPage === 1}
            onClick={() => setCurrentPage(currentPage - 1)}
          >
            上一页
          </Button>
          <div className="flex items-center gap-2">
            {[1, 2, 3, 4, 5].map((page) => (
              <button
                key={page}
                onClick={() => setCurrentPage(page)}
                className={`w-9 h-9 rounded ${
                  currentPage === page
                    ? 'bg-[#409EFF] text-white'
                    : 'bg-white text-[#606266] hover:text-[#409EFF] border border-[#DCDFE6]'
                } transition-colors`}
              >
                {page}
              </button>
            ))}
          </div>
          <Button
            variant="secondary"
            size="medium"
            disabled={currentPage === 5}
            onClick={() => setCurrentPage(currentPage + 1)}
          >
            下一页
          </Button>
        </div>
      </main>
    </div>
  );
}
