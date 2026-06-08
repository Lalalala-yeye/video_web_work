import { Eye } from 'lucide-react';
import { Link } from 'react-router';
import { Card } from './ui/Card';
import { Tag } from './ui/Tag';
import { Avatar } from './ui/Avatar';

export interface LiveCardProps {
  id: string;
  title: string;
  cover: string;
  streamer: {
    name: string;
    avatar?: string;
  };
  viewers: number;
  isLive: boolean;
}

export function LiveCard({ id, title, cover, streamer, viewers, isLive }: LiveCardProps) {
  const formatViewers = (num: number) => {
    if (num >= 10000) {
      return `${(num / 10000).toFixed(1)}万`;
    }
    return num.toString();
  };

  return (
    <Link to={`/live/${id}`}>
      <Card hoverable className="overflow-hidden">
        {/* 封面 16:9 */}
        <div className="relative aspect-video bg-[#F5F7FA] overflow-hidden group">
          <img
            src={cover}
            alt={title}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          />
          <div className="absolute inset-0 bg-black/0 group-hover:bg-black/10 transition-colors" />

          {/* 直播中标签 */}
          {isLive && (
            <div className="absolute top-2 left-2">
              <Tag type="live" size="small">
                <span className="inline-block w-1.5 h-1.5 rounded-full bg-white mr-1 animate-pulse" />
                直播中
              </Tag>
            </div>
          )}

          {/* 观看人数 */}
          <div className="absolute bottom-2 right-2 px-2 py-1 bg-black/60 rounded text-white text-xs flex items-center gap-1">
            <Eye size={12} />
            <span>{formatViewers(viewers)}</span>
          </div>
        </div>

        {/* 信息 */}
        <div className="p-3">
          <h3 className="text-[#303133] text-sm font-medium line-clamp-2 leading-5 min-h-[2.5rem] mb-2">
            {title}
          </h3>
          <div className="flex items-center gap-2">
            <Avatar size={24} src={streamer.avatar} name={streamer.name} />
            <span className="text-xs text-[#909399] truncate">{streamer.name}</span>
          </div>
        </div>
      </Card>
    </Link>
  );
}
