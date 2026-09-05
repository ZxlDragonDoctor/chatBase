package com.zxl.chatbase.opencode;

/**
 * opencode 流式处理回调
 * 用于在轮询过程中将中间结果实时推送给调用方
 */
@FunctionalInterface
public interface StreamingCallback {
    /**
     * 收到中间更新时调用
     * @param partialAnswer 当前累计的回复内容（含思考/工具/文本）
     */
    void onUpdate(String partialAnswer);
}
