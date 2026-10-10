package com.zxl.chatbase.opencode.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class OpencodeNodeRpcClient {
    private final OpencodeNodeRegistry registry;
    private final OpencodeNodeService nodeService;
    private final OpencodeNodeProperties properties;
    private final ObjectMapper objectMapper;

    public ResponseEntity<String> invoke(Long nodeId, OpencodeNodeCommandMapper.MappedCommand command,
                                         Consumer<String> progressListener) {
        nodeService.ensureCanInvoke(nodeId, command.getCommand());
        OpencodeNodeSession session = registry.getOnlineSession(nodeId);
        if (session == null) throw new IllegalStateException("本机节点“" + nodeService.getNodeName(nodeId) + "”当前离线");
        JsonNode frame = session.invoke(command.getCommand(), command.getParams(), properties.getRequestTimeoutSeconds(), progressListener);
        int status = frame.path("httpStatus").asInt(200);
        JsonNode body = frame.path("data");
        try {
            String bodyText = body.isMissingNode() || body.isNull() ? "{}" : objectMapper.writeValueAsString(body);
            return ResponseEntity.status(status).body(bodyText);
        } catch (Exception e) {
            throw new IllegalStateException("无法读取本机 OpenCode 响应", e);
        }
    }
}
