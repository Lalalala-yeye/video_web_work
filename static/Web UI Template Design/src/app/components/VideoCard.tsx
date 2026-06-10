import { Play, Eye } from 'lucide-react';
import { Link } from 'react-router';
import { Card } from './ui/Card';
import { Avatar } from './ui/Avatar';

export interface VideoCardProps {
  id: string;
  title: string;
  cover: string;
  author: {
    name: string;
    avatar?: string;
  };
  views: number;
  publishedAt: string;
}

export function VideoCard({ id, title, cover, author, views, publishedAt }: VideoCardProps) {
  const formatViews = (num: number) => {
    if (num >= 10000) {
      return `${(num / 10000).toFixed(1)}万`;
    }
    return num.toString();
  };

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));

    if (days === 0) return '今天';
    if (days === 1) return '昨天';
    if (days < 7) return `${days}天前`;
    if (days < 30) return `${Math.floor(days / 7)}周前`;
    if (days < 365) return `${Math.floor(days / 30)}月前`;
    return `${Math.floor(days / 365)}年前`;
  };

  return (
    <Link to={`/video/${id}`}>
      <Card hoverable className="overflow-hidden">
        {/* 封面 16:9 */}
        <div className="relative aspect-video bg-[#F5F7FA] overflow-hidden group">
          <img
            src={cover}
            alt={title}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          />
          <div className="absolute inset-0 bg-black/0 group-hover:bg-black/20 transition-colors flex items-center justify-center">
            <div className="w-12 h-12 rounded-full bg-white/0 group-hover:bg-white/90 flex items-center justify-center transition-all">
              <Play size={24} className="text-white group-hover:text-[#409EFF] fill-current" />
            </div>
          </div>
          {/* 播放次数 */}
          <div className="absolute bottom-2 right-2 px-2 py-1 bg-black/60 rounded text-white text-xs flex items-center gap-1">
            <Eye size={12} />
            <span>{formatViews(views)}</span>
          </div>
        </div>

        {/* 信息 */}
        <div className="p-3">
          <h3 className="text-[#303133] text-sm font-medium line-clamp-2 leading-5 min-h-[2.5rem] mb-2">
            {title}
          </h3>
          <div className="flex items-center gap-2">
            <Avatar size={24} src={author.avatar} name={author.name} />
            <div className="flex-1 flex items-center justify-between text-xs text-[#909399]">
              <span className="truncate">{author.name}</span>
              <span className="flex-shrink-0">{formatDate(publishedAt)}</span>
            </div>
          </div>
        </div>
      </Card>
    </Link>
  );
}
