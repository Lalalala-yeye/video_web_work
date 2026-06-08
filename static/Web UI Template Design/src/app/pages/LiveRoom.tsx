import { useState } from 'react';
import { useParams } from 'react-router';
import { Volume2, Maximize, Eye, Heart, Gift, Send } from 'lucide-react';
import { TopNavigation } from '../components/TopNavigation';
import { Avatar } from '../components/ui/Avatar';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Tag } from '../components/ui/Tag';

// 模拟聊天消息
const mockMessages = Array.from({ length: 20 }, (_, i) => ({
  id: `msg-${i}`,
  user: `用户${i + 1}`,
  message: ['欢迎来到直播间！', '主播加油！', '这个内容太棒了', '学到了很多', '给主播点赞'][
    i % 5
  ],
  timestamp: new Date(Date.now() - (20 - i) * 60000).toISOString(),
}));

export function LiveRoom() {
  const { id } = useParams();
  const [chatMessage, setChatMessage] = useState('');
  const [messages, setMessages] = useState(mockMessages);

  const handleSendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (chatMessage.trim()) {
      setMessages([
        ...messages,
        {
          id: `msg-${Date.now()}`,
          user: '我',
          message: chatMessage,
          timestamp: new Date().toISOString(),
        },
      ]);
      setChatMessage('');
    }
  };

  return (
    <div className="min-h-screen bg-[#F5F7FA]">
      <TopNavigation
        isLoggedIn={true}
        userInfo={{ name: '测试用户', avatar: undefined }}
        onSearch={(query) => console.log('搜索:', query)}
      />

      <main className="max-w-[1280px] mx-auto px-4 pt-20 pb-8">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
          {/* 左侧：播放器和直播信息 */}
          <div className="lg:col-span-2">
            {/* 播放器 16:9 */}
            <div className="relative aspect-video bg-black rounded-lg overflow-hidden mb-4">
              <img
                src="https://images.unsplash.com/photo-1611162616305-c69b3fa7fbe0?w=800&h=450&fit=crop"
                alt="直播画面"
                className="w-full h-full object-cover"
              />

              {/* 直播中标签 */}
              <div className="absolute top-4 left-4">
                <Tag type="live" size="medium">
                  <span className="inline-block w-2 h-2 rounded-full bg-white mr-1.5 animate-pulse" />
                  直播中
                </Tag>
              </div>

              {/* 观看人数 */}
              <div className="absolute top-4 right-4 px-3 py-1.5 bg-black/60 rounded text-white text-sm flex items-center gap-2">
                <Eye size={16} />
                <span>8.5万人观看</span>
              </div>

              {/* 播放控制 */}
              <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/80 to-transparent p-4">
                <div className="flex items-center justify-end gap-4 text-white">
                  <button>
                    <Volume2 size={20} />
                  </button>
                  <button>
                    <Maximize size={20} />
                  </button>
                </div>
              </div>
            </div>

            {/* 直播信息 */}
            <div className="bg-white rounded-lg p-6">
              <div className="flex items-start justify-between mb-4">
                <div className="flex-1">
                  <h1 className="text-xl font-bold text-[#303133] mb-2">
                    {`精彩直播间 - ${id}`}
                  </h1>
                  <p className="text-sm text-[#909399]">正在分享有趣的内容</p>
                </div>
                <Button variant="primary" size="medium">
                  关注主播
                </Button>
              </div>

              <div className="flex items-center gap-3 pt-4 border-t border-[#EBEEF5]">
                <Avatar size={48} name="主播名称" />
                <div>
                  <h3 className="text-base font-medium text-[#303133]">主播名称</h3>
                  <p className="text-sm text-[#909399]">15.2万 粉丝</p>
                </div>
              </div>
            </div>
          </div>

          {/* 右侧：聊天互动区 */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg overflow-hidden h-[600px] flex flex-col">
              {/* 聊天头部 */}
              <div className="px-4 py-3 border-b border-[#EBEEF5]">
                <h3 className="text-base font-bold text-[#303133]">聊天室</h3>
                <p className="text-xs text-[#909399]">8.5万人在线</p>
              </div>

              {/* 消息列表 */}
              <div className="flex-1 overflow-y-auto p-4 space-y-3">
                {messages.map((msg) => (
                  <div key={msg.id} className="text-sm">
                    <span className="text-[#409EFF] font-medium">{msg.user}：</span>
                    <span className="text-[#606266]">{msg.message}</span>
                  </div>
                ))}
              </div>

              {/* 互动按钮 */}
              <div className="px-4 py-3 border-t border-[#EBEEF5] flex items-center gap-2">
                <button className="flex-1 flex items-center justify-center gap-2 py-2 text-[#606266] hover:text-[#F56C6C] hover:bg-[#FEF0F0] rounded transition-colors">
                  <Heart size={18} />
                  <span className="text-sm">点赞</span>
                </button>
                <button className="flex-1 flex items-center justify-center gap-2 py-2 text-[#606266] hover:text-[#E6A23C] hover:bg-[#FDF6EC] rounded transition-colors">
                  <Gift size={18} />
                  <span className="text-sm">礼物</span>
                </button>
              </div>

              {/* 消息输入 */}
              <form onSubmit={handleSendMessage} className="p-4 border-t border-[#EBEEF5]">
                <div className="flex gap-2">
                  <Input
                    placeholder="说点什么..."
                    value={chatMessage}
                    onChange={(e) => setChatMessage(e.target.value)}
                    className="flex-1"
                  />
                  <Button type="submit" variant="primary" size="medium">
                    <Send size={16} />
                  </Button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
