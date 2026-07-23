package com.zxl.chatbase.kb.service;

import com.zxl.chatbase.kb.entity.KbFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    String uploadFile(MultipartFile file, String source, String sourceId, String uploadUserId, String uploadGroupId);

    List<KbFile> listFiles(String source, String sourceId);

    void deleteFile(Long fileId);

    void saveKbFile(KbFile kbFile);
}