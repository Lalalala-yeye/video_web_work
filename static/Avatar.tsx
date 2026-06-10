import { ImgHTMLAttributes } from 'react';

interface AvatarProps extends ImgHTMLAttributes<HTMLImageElement> {
  size?: number;
  name?: string;
}

export function Avatar({ size = 40, name = '', src, className = '', ...props }: AvatarProps) {
  const getInitials = (name: string) => {
    if (!name) return '?';
    const parts = name.split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name[0].toUpperCase();
  };

  return (
    <div
      className={`inline-flex items-center justify-center rounded-full overflow-hidden bg-[#ecf5ff] text-[#409EFF] font-medium ${className}`}
      style={{ width: size, height: size, fontSize: size * 0.4 }}
    >
      {src ? (
        <img src={src} alt={name} className="w-full h-full object-cover" {...props} />
      ) : (
        <span>{getInitials(name)}</span>
      )}
    </div>
  );
}
