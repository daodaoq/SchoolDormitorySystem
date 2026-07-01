/**
 * 从 localStorage 读取 auth token。
 * Zustand v5 persist 格式为 { state: {...}, version: 0 }，v4 为扁平结构。
 *
 * @returns token 字符串，解析失败或未登录返回空字符串
 */
export function getToken(): string {
  try {
    const auth = localStorage.getItem('auth');
    if (!auth) return '';
    const parsed = JSON.parse(auth);
    return parsed?.state?.token || parsed?.token || '';
  } catch (err) {
    console.warn('[token] localStorage auth 数据损坏，已自动清除', err);
    localStorage.removeItem('auth');
    return '';
  }
}

/**
 * 从 localStorage 读取 auth 完整数据。
 * @returns 解析后的 auth 对象，失败或不存在返回 null
 */
export function getAuthData<T = Record<string, any>>(): T | null {
  try {
    const auth = localStorage.getItem('auth');
    if (!auth) return null;
    return JSON.parse(auth) as T;
  } catch (err) {
    console.warn('[token] localStorage auth 数据损坏，已自动清除', err);
    localStorage.removeItem('auth');
    return null;
  }
}
