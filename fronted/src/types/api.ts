/** 统一 API 响应包装 */
export interface ApiResult<T = any> {
  code: number;
  message: string;
  data: T;
}

/** 分页响应 */
export interface PageResult<T> {
  records: T[];
  total: number;
  page: number;
  pageSize: number;
}
