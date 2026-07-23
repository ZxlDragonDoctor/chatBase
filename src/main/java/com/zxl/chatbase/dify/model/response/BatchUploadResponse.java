package com.zxl.chatbase.dify.model.response;

import lombok.Data;
import java.util.List;

@Data
public class BatchUploadResponse {

    private int totalCount;

    private int successCount;

    private int failedCount;

    private List<FileUploadResult> results;

    @Data
    public static class FileUploadResult {
        private String fileName;
        private boolean success;
        private String message;
        private String difyFileId;

        public static FileUploadResult success(String fileName, String difyFileId) {
            FileUploadResult result = new FileUploadResult();
            result.setFileName(fileName);
            result.setSuccess(true);
            result.setMessage("上传成功");
            result.setDifyFileId(difyFileId);
            return result;
        }

        public static FileUploadResult fail(String fileName, String message) {
            FileUploadResult result = new FileUploadResult();
            result.setFileName(fileName);
            result.setSuccess(false);
            result.setMessage(message);
            return result;
        }
    }
}