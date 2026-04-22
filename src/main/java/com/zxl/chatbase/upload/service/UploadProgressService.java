package com.zxl.chatbase.upload.service;

import com.zxl.chatbase.upload.entity.UploadProgress;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UploadProgressService {

    private final Map<String, UploadProgress> progressMap = new ConcurrentHashMap<>();

    public UploadProgress createProgress(String taskId, int totalCount) {
        UploadProgress progress = UploadProgress.create(taskId, totalCount);
        progressMap.put(taskId, progress);
        return progress;
    }

    public UploadProgress getProgress(String taskId) {
        return progressMap.get(taskId);
    }

    public void updateProgress(String taskId, String fileName, boolean success, String message, String difyFileId) {
        UploadProgress progress = progressMap.get(taskId);
        if (progress == null) return;

        progress.setCompletedCount(progress.getCompletedCount() + 1);
        progress.setCurrentFile(fileName);
        progress.setUpdateTime(java.time.LocalDateTime.now());

        if (success) {
            progress.setSuccessCount(progress.getSuccessCount() + 1);
        } else {
            progress.setFailedCount(progress.getFailedCount() + 1);
        }

        UploadProgress.FileProgress fileProgress = new UploadProgress.FileProgress();
        fileProgress.setFileName(fileName);
        fileProgress.setStatus(success ? "success" : "failed");
        fileProgress.setMessage(message);
        fileProgress.setDifyFileId(difyFileId);
        progress.getFileProgresses().add(fileProgress);

        if (progress.getCompletedCount() >= progress.getTotalCount()) {
            progress.setStatus("completed");
            progress.setCurrentFile(null);
        }
    }

    public void markFailed(String taskId, String error) {
        UploadProgress progress = progressMap.get(taskId);
        if (progress == null) return;

        progress.setStatus("failed");
        progress.setUpdateTime(java.time.LocalDateTime.now());
    }

    public void removeProgress(String taskId) {
        progressMap.remove(taskId);
    }

    public void cleanOldProgress() {
        long now = System.currentTimeMillis();
        progressMap.entrySet().removeIf(entry -> {
            UploadProgress progress = entry.getValue();
            if (progress.isCompleted()) {
                long updateTime = progress.getUpdateTime() != null 
                    ? progress.getUpdateTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    : now;
                return (now - updateTime) > 3600000;
            }
            return false;
        });
    }
}