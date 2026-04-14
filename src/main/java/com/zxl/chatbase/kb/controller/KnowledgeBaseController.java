package com.zxl.chatbase.kb.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zxl.chatbase.kb.entity.KbCategory;
import com.zxl.chatbase.kb.entity.KbDocument;
import com.zxl.chatbase.kb.entity.KbKnowledgeBase;
import com.zxl.chatbase.kb.service.IKbCategoryService;
import com.zxl.chatbase.kb.service.IKbDocumentService;
import com.zxl.chatbase.kb.service.IKbKnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kb")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final IKbKnowledgeBaseService knowledgeBaseService;
    private final IKbDocumentService documentService;
    private final IKbCategoryService categoryService;

    @GetMapping("/category/tree")
    public List<KbCategory> categoryTree() {
        return categoryService.treeList();
    }

    @GetMapping("/category/page")
    public Page<KbCategory> categoryPage(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return categoryService.pageList(name, pageNum, pageSize);
    }

    @PostMapping("/category")
    public boolean createCategory(@RequestBody KbCategory category) {
        return categoryService.createCategory(category);
    }

    @PutMapping("/category")
    public boolean updateCategory(@RequestBody KbCategory category) {
        return categoryService.updateCategory(category);
    }

    @DeleteMapping("/category/{id}")
    public boolean deleteCategory(@PathVariable Long id) {
        return categoryService.deleteCategory(id);
    }

    @GetMapping("/page")
    public Page<KbKnowledgeBase> page(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return knowledgeBaseService.pageList(categoryId, name, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    public KbKnowledgeBase getById(@PathVariable Long id) {
        return knowledgeBaseService.getById(id);
    }

    @PostMapping
    public boolean create(@RequestBody KbKnowledgeBase knowledgeBase) {
        return knowledgeBaseService.createKnowledgeBase(knowledgeBase);
    }

    @PutMapping
    public boolean update(@RequestBody KbKnowledgeBase knowledgeBase) {
        return knowledgeBaseService.updateKnowledgeBase(knowledgeBase);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return knowledgeBaseService.deleteKnowledgeBase(id);
    }

    @PostMapping("/{id}/sync")
    public Map<String, Object> sync(@PathVariable Long id) {
        boolean success = knowledgeBaseService.syncDocumentsToDify(id);
        return Map.of("success", success, "message", success ? "同步成功" : "同步失败");
    }

    @GetMapping("/{kbId}/document/page")
    public Page<KbDocument> documentPage(
            @PathVariable Long kbId,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return documentService.pageList(kbId, title, pageNum, pageSize);
    }

    @PostMapping("/document")
    public boolean createDocument(@RequestBody KbDocument document) {
        return documentService.createDocument(document);
    }

    @PutMapping("/document")
    public boolean updateDocument(@RequestBody KbDocument document) {
        return documentService.updateDocument(document);
    }

    @DeleteMapping("/document/{id}")
    public boolean deleteDocument(@PathVariable Long id) {
        return documentService.deleteDocument(id);
    }

    @PostMapping("/document/{id}/sync")
    public Map<String, Object> syncDocument(@PathVariable Long id) {
        boolean success = documentService.syncToDify(id);
        return Map.of("success", success, "message", success ? "同步成功" : "同步失败");
    }
}
