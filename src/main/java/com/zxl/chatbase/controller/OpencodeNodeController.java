package com.zxl.chatbase.controller;

import com.zxl.chatbase.opencode.node.OpencodeNodeService;
import com.zxl.chatbase.opencode.node.dto.OpencodeNodeVO;
import com.zxl.chatbase.opencode.node.dto.PairingCodeVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/opencode/nodes")
@RequiredArgsConstructor
public class OpencodeNodeController {
    private final OpencodeNodeService nodeService;

    @PostMapping("/pairing-codes")
    public PairingCodeVO createPairingCode(@RequestAttribute("currentUser") String adminId) {
        return nodeService.createPairingCode(adminId);
    }

    @GetMapping
    public List<OpencodeNodeVO> listNodes() {
        return nodeService.listNodes();
    }

    @PostMapping("/{id}/approve")
    public void approve(@PathVariable Long id) {
        nodeService.approveNode(id);
    }

    @PostMapping("/{id}/reject")
    public void reject(@PathVariable Long id) {
        nodeService.rejectNode(id);
    }

    @PutMapping("/{id}/name")
    public void rename(@PathVariable Long id, @RequestBody RenameRequest request) {
        nodeService.renameNode(id, request.getName());
    }

    @DeleteMapping("/{id}")
    public void revoke(@PathVariable Long id) {
        nodeService.revokeNode(id);
    }

    @Data
    public static class RenameRequest {
        private String name;
    }
}
