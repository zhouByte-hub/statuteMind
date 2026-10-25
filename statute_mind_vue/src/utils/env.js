/** 是否偏好减少动效（模块加载时取一次，与原型行为一致） */
export const prefersReduced =
  typeof matchMedia !== 'undefined' && matchMedia('(prefers-reduced-motion: reduce)').matches
