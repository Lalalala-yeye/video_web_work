import { HTMLAttributes } from 'react';

interface TagProps extends HTMLAttributes<HTMLSpanElement> {
  type?: 'success' | 'warning' | 'danger' | 'info' | 'live';
  size?: 'small' | 'medium';
}

export function Tag({ type = 'info', size = 'medium', className = '', children, ...props }: TagProps) {
  const typeStyles = {
    success: 'bg-[#f0f9ff] text-[#67C23A] border-[#c2e7b0]',
    warning: 'bg-[#fdf6ec] text-[#E6A23C] border-[#f5dab1]',
    danger: 'bg-[#fef0f0] text-[#F56C6C] border-[#fbc4c4]',
    info: 'bg-[#f4f4f5] text-[#909399] border-[#d3d4d6]',
    live: 'bg-[#F56C6C] text-white border-[#F56C6C]'
  };

  const sizeStyles = {
    small: 'px-2 py-0.5 text-xs',
    medium: 'px-2 py-1 text-sm'
  };

  return (
    <span
      className={`inline-flex items-center rounded border ${typeStyles[type]} ${sizeStyles[size]} ${className}`}
      {...props}
    >
      {children}
    </span>
  );
}
