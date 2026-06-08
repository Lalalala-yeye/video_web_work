import { ThumbsUp } from 'lucide-react';
import { Avatar } from './ui/Avatar';

export interface CommentItemProps {
  id: string;
  author: {
    name: string;
    avatar?: string;
  };
  content: string;
  likes: number;
  createdAt: string;
  replies?: CommentItemProps[];
}

export function CommentItem({ author, content, likes, createdAt, replies }: CommentItemProps) {
  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / (1000 * 60));
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));

    if (minutes < 1) return '刚刚';
    if (minutes < 60) return `${minutes}分钟前`;
    if (hours < 24) return `${hours}小时前`;
    if (days < 7) return `${days}天前`;
    return date.toLocaleDateString('zh-CN');
  };

  return (
    <div className="py-4">
      <div className="flex gap-3">
        <Avatar size={40} src={author.avatar} name={author.name} />
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-1">
            <span className="text-sm text-[#606266] font-medium">{author.name}</span>
            <span className="text-xs text-[#C0C4CC]">{formatDate(createdAt)}</span>
          </div>
          <p className="text-sm text-[#303133] leading-6 mb-2">{content}</p>
          <div className="flex items-center gap-4">
            <button className="flex items-center gap-1 text-xs text-[#909399] hover:text-[#409EFF] transition-colors">
              <ThumbsUp size={14} />
              <span>{likes > 0 ? likes : '点赞'}</span>
            </button>
            <button className="text-xs text-[#909399] hover:text-[#409EFF] transition-colors">
              回复
            </button>
          </div>

          {/* 回复列表 */}
          {replies && replies.length > 0 && (
            <div className="mt-3 pl-4 border-l-2 border-[#F2F6FC]">
              {replies.map((reply) => (
                <CommentItem key={reply.id} {...reply} />
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
