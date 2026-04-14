package com.zxl.chatbase.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zxl.chatbase.kb.entity.KbFile;
import com.zxl.chatbase.kb.mapper.KbFileMapper;
import com.zxl.chatbase.kb.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final KbFileMapper kbFileMapper;

    @Value("${chat.file.upload-path:./uploads}")
    private String uploadPath;

    @Value("${chat.file.access-path:/uploads}")
    private String accessPath;

    @Override
    @Transactional
    public String uploadFile(MultipartFile file, String source, String sourceId, String uploadUserId, String uploadGroupId) {
        String originalFilename = file.getOriginalFilename();
        String fileExt = getFileExt(originalFilename);
        String fileName = UUID.randomUUID().toString() + "." + fileExt;
        
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        
        File destFile = new File(uploadDir, fileName);
        try {
            file.transferTo(destFile);
        } catch (IOException e) {
            log.error("文件保存失败: {}", originalFilename, e);
            throw new RuntimeException("文件保存失败: " + e.getMessage());
        }
        
        KbFile kbFile = new KbFile();
        kbFile.setFileName(originalFilename);
        kbFile.setFilePath(accessPath + "/" + fileName);
        kbFile.setFileSize(file.getSize());
        kbFile.setFileType(file.getContentType());
        kbFile.setFileExt(fileExt);
        kbFile.setBucket("local");
        kbFile.setSource(source);
        kbFile.setSourceId(sourceId);
        kbFile.setUploadUserId(uploadUserId);
        kbFile.setUploadGroupId(uploadGroupId);
        kbFile.setStatus(true);
        kbFile.setCreateTime(LocalDateTime.now());
        
        kbFileMapper.insert(kbFile);
        log.info("文件上传成功: id={}, name={}, size={}", kbFile.getId(), originalFilename, file.getSize());
        
        return kbFile.getId().toString();
    }

    @Override
    public List<KbFile> listFiles(String source, String sourceId) {
        LambdaQueryWrapper<KbFile> wrapper = new LambdaQueryWrapper<KbFile>()
                .eq(KbFile::getSource, source)
                .eq(KbFile::getSourceId, sourceId)
                .eq(KbFile::getStatus, true)
                .orderByDesc(KbFile::getCreateTime);
        return kbFileMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void deleteFile(Long fileId) {
        KbFile file = kbFileMapper.selectById(fileId);
        if (file != null) {
            file.setStatus(false);
            kbFileMapper.updateById(file);
            
            File physicalFile = new File(uploadPath + "/" + getFileNameFromPath(file.getFilePath()));
            if (physicalFile.exists()) {
                physicalFile.delete();
            }
            log.info("文件删除: id={}, name={}", fileId, file.getFileName());
        }
    }

    private String getFileExt(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private String getFileNameFromPath(String filePath) {
        if (filePath == null || !filePath.contains("/")) {
            return filePath;
        }
        return filePath.substring(filePath.lastIndexOf("/") + 1);
    }
}