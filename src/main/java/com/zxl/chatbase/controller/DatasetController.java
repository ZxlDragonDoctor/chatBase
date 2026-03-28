package com.zxl.chatbase.controller;

import com.zxl.chatbase.dify.server.DifyService;
import lombok.Data;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 简单的 Dify 知识库管理接口
 */
@RestController
@RequestMapping("/api/dify/datasets")
public class DatasetController {

    private final DifyService difyService;

    public DatasetController(DifyService difyService) {
        this.difyService = difyService;
    }

    /**
     * 创建一个新的知识库，并返回 dataset_id
     */
    @PostMapping
    public Map<String, Object> createDataset(@RequestBody CreateDatasetRequest request) {
        String datasetId = difyService.createDataset(request.getName(), request.getDescription());
        Map<String, Object> result = new HashMap<>();
        result.put("datasetId", datasetId);
        return result;
    }

    @Data
    public static class CreateDatasetRequest {
        private String name;
        private String description;
    }
}

