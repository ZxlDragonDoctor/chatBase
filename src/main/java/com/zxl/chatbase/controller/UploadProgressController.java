package com.zxl.chatbase.controller;

import com.zxl.chatbase.upload.entity.UploadProgress;
import com.zxl.chatbase.upload.service.UploadProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadProgressController {

    private final UploadProgressService progressService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @GetMapping("/progress/{taskId}")
    public UploadProgress getProgress(@PathVariable String taskId) {
        return progressService.getProgress(taskId);
    }

    @GetMapping("/progress/{taskId}/sse")
    public SseEmitter subscribeProgress(@PathVariable String taskId) {
        SseEmitter emitter = new SseEmitter(180000L);
        
        emitter.onCompletion(() -> {});
        emitter.onTimeout(() -> emitter.complete());
        
        executor.execute(() -> {
            try {
                while (true) {
                    UploadProgress progress = progressService.getProgress(taskId);
                    if (progress == null) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data("任务不存在或已过期"));
                            emitter.complete();
                        } catch (IllegalStateException ignored) {}
                        break;
                    }

                    try {
                        emitter.send(SseEmitter.event()
                                .name("progress")
                                .data(progress, MediaType.APPLICATION_JSON));
                    } catch (IllegalStateException e) {
                        break;
                    }

                    if (progress.isCompleted()) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("complete")
                                    .data(progress, MediaType.APPLICATION_JSON));
                            emitter.complete();
                            progressService.removeProgress(taskId);
                        } catch (IllegalStateException ignored) {}
                        break;
                    }

                    Thread.sleep(500);
                }
            } catch (IOException | InterruptedException e) {
                try {
                    emitter.completeWithError(e);
                } catch (IllegalStateException ignored) {}
            }
        });
        
        return emitter;
    }
}