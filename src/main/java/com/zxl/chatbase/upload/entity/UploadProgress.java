package com.zxl.chatbase.upload.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class UploadProgress {

    private String taskId;

    private int totalCount;

    private int completedCount;

    private int successCount;

    private int failedCount;

    private String currentFile;

    private String status;

    private List<FileProgress> fileProgresses;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Data
    public static class FileProgress {
        private String fileName;
        private String status;
        private String message;
        private String difyFileId;
    }

    public static UploadProgress create(String taskId, int totalCount) {
        UploadProgress progress = new UploadProgress();
        progress.setTaskId(taskId);
        progress.setTotalCount(totalCount);
        progress.setCompletedCount(0);
        progress.setSuccessCount(0);
        progress.setFailedCount(0);
        progress.setStatus("pending");
        progress.setFileProgresses(new ArrayList<>());
        progress.setCreateTime(LocalDateTime.now());
        progress.setUpdateTime(LocalDateTime.now());
        return progress;
    }

    public double getProgressPercent() {
        if (totalCount == 0) return 0;
        return (completedCount * 100.0) / totalCount;
    }

    public boolean isCompleted() {
        return "completed".equals(status) || "failed".equals(status);
    }
}