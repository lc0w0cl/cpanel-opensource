import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export type ObjectValues<T> = T[keyof T];

/**
 * 根据环境处理图片URL
 * 开发环境：需要加上 http://localhost:8080 前缀
 * 生产环境：前后端部署在一起，直接使用相对路径
 * @param logoPath 图片路径，如 "/uploads/xxx.png"
 * @returns 完整的图片URL
 */
export function getImageUrl(logoPath: string): string {
  // 如果路径为空或不是以 /uploads/ 开头，直接返回原路径
  if (!logoPath || !logoPath.startsWith('/uploads/')) {
    return logoPath;
  }

  // 获取运行时配置
  const config = useRuntimeConfig();

  // 开发环境需要加上完整的后端地址前缀
  if (config.public.isDevelopment) {
    return `${config.public.apiBaseUrl}${logoPath}`;
  }

  // 生产环境直接使用相对路径
  return logoPath;
}