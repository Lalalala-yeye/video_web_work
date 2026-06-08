import { useState } from 'react';
import { useParams } from 'react-router';
import { Play, Pause, Volume2, Maximize, ThumbsUp, Star, Share2, Eye } from 'lucide-react';
import { TopNavigation } from '../components/TopNavigation';
import { Avatar } from '../components/ui/Avatar';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { CommentItem, CommentItemProps } from '../components/CommentItem';

// 模拟数据
const mockComments: CommentItemProps[] = [
  {
    id: 'comment-1',
    author: { name: '用户A', avatar: undefined },
    content: '这个视频太棒了！讲解得非常清晰，学到了很多东西。',
    likes: 42,
    createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
    replies: [
      {
        id: 'reply-1',
        author: { name: 'UP主', avatar: undefined },
        content: '谢谢支持！',
        likes: 5,
        createdAt: new Date(Date.now() - 1 * 60 * 60 * 1000).toISOString(),
      },
    ],
  },
  {
    id: 'comment-2',
    author: { name: '用户B', avatar: undefined },
    content: '期待下一期内容！',
    likes: 18,
    createdAt: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString(),
  },
];

export function VideoDetail() {
  const { id } = useParams();
  const [isPlaying, setIsPlaying] = useState(false);
  const [comment, setComment] = useState('');

  const handleSubmitComment = (e: React.FormEvent) => {
    e.preventDefault();
    console.log('发表评论:', comment);
    setComment('');
  };

  return (
    <div className="min-h-screen bg-[#F5F7FA]">
      <TopNavigation
        isLoggedIn={true}
        userInfo={{ name: '测试用户', avatar: undefined }}
        onSearch={(query) => console.log('搜索:', query)}
      />

      <main className="max-w-[1280px] mx-auto px-4 pt-20 pb-8">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* 左侧：播放器和视频信息 */}
          <div className="lg:col-span-2">
            {/* 播放器 16:9 */}
            <div className="relative aspect-video bg-black rounded-lg overflow-hidden mb-4">
              <img
                src="https://images.unsplash.com/photo-1611162616305-c69b3fa7fbe0?w=800&h=450&fit=crop"
                alt="视频封面"
                className="w-full h-full object-cover"
              />
              <div className="absolute inset-0 flex items-center justify-center">
                <button
                  onClick={() => setIsPlaying(!isPlaying)}
                  className="w-16 h-16 rounded-full bg-white/90 hover:bg-white flex items-center justify-center transition-colors"
                >
                  {isPlaying ? (
                    <Pause size={32} className="text-[#409EFF]" />
                  ) : (
                    <Play size={32} className="text-[#409EFF] fill-current ml-1" />
                  )}
                </button>
              </div>
              {/* 播放控制条 */}
              <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/80 to-transparent p-4">
                <div className="flex items-center gap-4 text-white">
                  <button>
                    {isPlaying ? <Pause size={20} /> : <Play size={20} />}
                  </button>
                  <div className="flex-1 h-1 bg-white/30 rounded-full overflow-hidden">
                    <div className="w-1/3 h-full bg-[#409EFF]" />
                  </div>
                  <span className="text-sm">05:24 / 15:42</span>
                  <button>
                    <Volume2 size={20} />
                  </button>
                  <button>
                    <Maximize size={20} />
                  </button>
                </div>
              </div>
            </div>

            {/* 视频标题和信息 */}
            <div className="bg-white rounded-lg p-6 mb-4">
              <h1 className="text-xl font-bold text-[#303133] mb-4">
                {`精彩视频标题 - ${id}`}
              </h1>

              <div className="flex items-center justify-between mb-6">
                <div className="flex items-center gap-3">
                  <Avatar size={48} name="UP主名称" />
                  <div>
                    <h3 className="text-base font-medium text-[#303133]">UP主名称</h3>
                    <p className="text-sm text-[#909399]">10.5万 粉丝</p>
                  </div>
                </div>
                <Button variant="primary" size="medium">
                  <Star size={16} className="mr-1" />
                  订阅
                </Button>
              </div>

              <div className="flex items-center gap-6 py-4 border-t border-[#EBEEF5]">
                <div className="flex items-center gap-2 text-[#606266]">
                  <Eye size={18} />
                  <span className="text-sm">12.5万 播放</span>
                </div>
                <button className="flex items-center gap-2 text-[#606266] hover:text-[#409EFF] transition-colors">
                  <ThumbsUp size={18} />
                  <span className="text-sm">3.2万</span>
                </button>
                <button className="flex items-center gap-2 text-[#606266] hover:text-[#409EFF] transition-colors">
                  <Share2 size={18} />
                  <span className="text-sm">分享</span>
                </button>
              </div>

              <div className="pt-4 border-t border-[#EBEEF5]">
                <p className="text-sm text-[#606266] leading-6">
                  这是视频的详细介绍内容，可以包含多行文字。介绍视频的主要内容、亮点和相关信息。
                  欢迎大家观看、点赞、投币、收藏！
                </p>
              </div>
            </div>

            {/* 评论区 */}
            <div className="bg-white rounded-lg p-6">
              <h2 className="text-lg font-bold text-[#303133] mb-4">
                评论 {mockComments.length}
              </h2>

              {/* 评论输入 */}
              <form onSubmit={handleSubmitComment} className="mb-6">
                <div className="flex gap-3">
                  <Avatar size={40} name="测试用户" />
                  <div className="flex-1">
                    <Input
                      placeholder="发一条友善的评论"
                      value={comment}
                      onChange={(e) => setComment(e.target.value)}
                      className="mb-2"
                    />
                    <div className="flex justify-end">
                      <Button type="submit" variant="primary" size="medium">
                        发表评论
                      </Button>
                    </div>
                  </div>
                </div>
              </form>

              {/* 评论列表 */}
              <div className="divide-y divide-[#EBEEF5]">
                {mockComments.map((comment) => (
                  <CommentItem key={comment.id} {...comment} />
                ))}
              </div>
            </div>
          </div>

          {/* 右侧：推荐视频 */}
          <div className="lg:col-span-1">
            <h3 className="text-base font-bold text-[#303133] mb-4">推荐视频</h3>
            <div className="space-y-3">
              {Array.from({ length: 5 }).map((_, i) => (
                <div
                  key={i}
                  className="bg-white rounded-lg overflow-hidden hover:shadow-md transition-shadow cursor-pointer"
                >
                  <div className="flex gap-3 p-3">
                    <div className="w-40 h-24 flex-shrink-0 bg-[#F5F7FA] rounded overflow-hidden">
                      <img
                        src={`https://images.unsplash.com/photo-${1500000000000 + i * 10000000}?w=160&h=96&fit=crop`}
                        alt=""
                        className="w-full h-full object-cover"
                      />
                    </div>
                    <div className="flex-1 min-w-0">
                      <h4 className="text-sm font-medium text-[#303133] line-clamp-2 mb-2">
                        推荐视频标题 {i + 1}
                      </h4>
                      <p className="text-xs text-[#909399] mb-1">UP主名称</p>
                      <p className="text-xs text-[#C0C4CC]">8.5万 播放</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
