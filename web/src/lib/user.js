import { getAuth } from './auth';
/** @deprecated 使用 getAuth().userId；保留以兼容旧调用 */
export function getOrCreateUserId() {
    return getAuth().userId;
}
