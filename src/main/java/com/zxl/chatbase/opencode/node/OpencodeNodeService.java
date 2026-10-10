package com.zxl.chatbase.opencode.node;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zxl.chatbase.opencode.node.dto.NodeHelloRequest;
import com.zxl.chatbase.opencode.node.dto.NodePairRequest;
import com.zxl.chatbase.opencode.node.dto.OpencodeNodeVO;
import com.zxl.chatbase.opencode.node.dto.PairingCodeVO;
import com.zxl.chatbase.opencode.node.entity.OpencodeNode;
import com.zxl.chatbase.opencode.node.mapper.OpencodeNodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpencodeNodeService {
    private static final String PAIR_KEY_PREFIX = "opencode:node:pair:";
    private static final String APPROVED = "APPROVED";
    private static final String PENDING = "PENDING";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DefaultRedisScript<String> GET_AND_DELETE = new DefaultRedisScript<>(
            "local v=redis.call('GET',KEYS[1]); if v then redis.call('DEL',KEYS[1]); end; return v", String.class);

    private final OpencodeNodeMapper mapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final OpencodeNodeProperties properties;
    private final OpencodeNodeRegistry registry;

    public PairingCodeVO createPairingCode(String adminId) {
        ensureEnabled();
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        int ttl = Math.max(60, properties.getPairingTtlSeconds());
        redis.opsForValue().set(pairKey(code), adminId, java.time.Duration.ofSeconds(ttl));
        return new PairingCodeVO(code, LocalDateTime.now().plusSeconds(ttl));
    }

    public NodePairRequest enrollNode(NodeHelloRequest hello) {
        ensureEnabled();
        validateHello(hello);
        String pairingCode = hello.getPairingCode();
        String adminId = redis.execute(GET_AND_DELETE, Collections.singletonList(pairKey(pairingCode)));
        if (!StringUtils.hasText(adminId)) throw new IllegalArgumentException("配对码无效、已使用或已过期");

        String nodeToken = randomSecret(32);
        OpencodeNode node = new OpencodeNode();
        node.setNodeKey(hello.getNodeKey());
        node.setName(hello.getName().trim());
        node.setPlatform(trimOrNull(hello.getPlatform(), 32));
        node.setAgentVersion(trimOrNull(hello.getAgentVersion(), 40));
        node.setTokenHash(sha256(nodeToken));
        node.setCapabilities(writeCapabilities(hello.getCapabilities()));
        node.setApprovedCapabilities("[]");
        node.setStatus(PENDING);
        node.setCreatedBy(adminId);
        node.setCreateTime(LocalDateTime.now());
        node.setUpdateTime(LocalDateTime.now());
        OpencodeNode existing = mapper.selectOne(new LambdaQueryWrapper<OpencodeNode>()
                .eq(OpencodeNode::getNodeKey, hello.getNodeKey()).last("LIMIT 1"));
        if (existing != null) {
            if (!(PENDING.equals(existing.getStatus()) || "REJECTED".equals(existing.getStatus())
                    || "REVOKED".equals(existing.getStatus()))) {
                throw new IllegalArgumentException("该节点标识已登记；请先撤销旧节点或重置本机 Agent 身份");
            }
            node.setId(existing.getId());
            node.setCreateTime(existing.getCreateTime());
            mapper.updateById(node);
            node.setLastSeenAt(null);
        } else {
            try {
                mapper.insert(node);
            } catch (Exception e) {
                log.warn("OpenCode 节点配对登记失败: nodeKey={}", hello.getNodeKey());
                throw new IllegalArgumentException("该节点标识已登记，请在本机 Agent 配置中重置节点身份");
            }
        }
        NodePairRequest result = new NodePairRequest();
        result.setNodeId(node.getId());
        result.setNodeToken(nodeToken);
        result.setApproval(PENDING);
        log.info("OpenCode 节点等待管理员批准: nodeId={}, createdBy={}", node.getId(), adminId);
        return result;
    }

    public OpencodeNode authenticate(Long nodeId, String nodeToken, List<String> declaredCapabilities,
                                     String name, String platform, String agentVersion) {
        ensureEnabled();
        if (nodeId == null || !StringUtils.hasText(nodeToken)) throw new IllegalArgumentException("节点凭据无效");
        OpencodeNode node = mapper.selectById(nodeId);
        if (node == null || "REVOKED".equals(node.getStatus()) || "REJECTED".equals(node.getStatus())
                || !constantTimeEquals(node.getTokenHash(), sha256(nodeToken))) {
            throw new IllegalArgumentException("节点凭据无效或节点已撤销");
        }
        validateCapabilities(declaredCapabilities);
        Set<String> declared = new HashSet<>(declaredCapabilities);
        Set<String> approved = new HashSet<>(readCapabilities(node.getApprovedCapabilities()));
        approved.retainAll(declared);
        node.setCapabilities(writeCapabilities(declaredCapabilities));
        node.setApprovedCapabilities(writeCapabilities(new ArrayList<>(approved)));
        if (StringUtils.hasText(name)) node.setName(trimOrNull(name, 100));
        node.setPlatform(trimOrNull(platform, 32));
        node.setAgentVersion(trimOrNull(agentVersion, 40));
        node.setLastSeenAt(LocalDateTime.now());
        node.setUpdateTime(LocalDateTime.now());
        mapper.updateById(node);
        return node;
    }

    public List<OpencodeNodeVO> listNodes() {
        List<OpencodeNode> nodes = mapper.selectList(new LambdaQueryWrapper<OpencodeNode>().orderByDesc(OpencodeNode::getId));
        List<OpencodeNodeVO> result = new ArrayList<>(nodes.size());
        for (OpencodeNode node : nodes) result.add(toVO(node));
        return result;
    }

    public void approveNode(Long id) {
        OpencodeNode node = requireNode(id);
        if (!PENDING.equals(node.getStatus()) && !APPROVED.equals(node.getStatus())) {
            throw new IllegalArgumentException("该节点当前状态不能批准");
        }
        List<String> capabilities = readCapabilities(node.getCapabilities());
        validateCapabilities(capabilities);
        node.setApprovedCapabilities(writeCapabilities(capabilities));
        node.setStatus(APPROVED);
        node.setUpdateTime(LocalDateTime.now());
        mapper.updateById(node);
    }

    public void rejectNode(Long id) {
        OpencodeNode node = requireNode(id);
        if (!PENDING.equals(node.getStatus())) throw new IllegalArgumentException("只能拒绝待批准的节点");
        node.setStatus("REJECTED");
        node.setUpdateTime(LocalDateTime.now());
        mapper.updateById(node);
        registry.disconnect(id, 4003, "node rejected");
    }

    public void renameNode(Long id, String name) {
        if (!StringUtils.hasText(name) || name.trim().length() > 100) throw new IllegalArgumentException("节点名称长度需为 1 到 100 个字符");
        OpencodeNode node = requireNode(id);
        node.setName(name.trim());
        node.setUpdateTime(LocalDateTime.now());
        mapper.updateById(node);
    }

    public void revokeNode(Long id) {
        OpencodeNode node = requireNode(id);
        node.setStatus("REVOKED");
        node.setTokenHash(sha256(randomSecret(32)));
        node.setUpdateTime(LocalDateTime.now());
        mapper.updateById(node);
        registry.disconnect(id, 4003, "node revoked");
    }

    public OpencodeNode requireBindableNode(Long id) {
        OpencodeNode node = requireNode(id);
        if (!APPROVED.equals(node.getStatus())) throw new IllegalArgumentException("只能绑定已批准的节点");
        return node;
    }

    public void ensureCanInvoke(Long nodeId, String command) {
        ensureEnabled();
        if (!OpencodeNodeCommand.isAllowed(command)) throw new IllegalArgumentException("该 OpenCode 命令未开放");
        OpencodeNode node = requireNode(nodeId);
        if (!APPROVED.equals(node.getStatus())) {
            throw new IllegalStateException("本机节点“" + node.getName() + "”未获批准或已撤销，无法执行");
        }
        if (!readCapabilities(node.getApprovedCapabilities()).contains(command)) {
            throw new IllegalStateException("本机节点“" + node.getName() + "”的执行能力尚未获批准");
        }
        if (!isOnline(nodeId)) throw new IllegalStateException("本机节点“" + node.getName() + "”当前离线");
    }

    public String getNodeName(Long nodeId) {
        return requireNode(nodeId).getName();
    }

    public boolean isOnline(Long nodeId) {
        OpencodeNode node = mapper.selectById(nodeId);
        if (node == null || node.getLastSeenAt() == null || !registry.isOnline(nodeId)) return false;
        return node.getLastSeenAt().plusSeconds(Math.max(10, properties.getOfflineAfterSeconds())).isAfter(LocalDateTime.now());
    }

    public String getNodeStatus(Long nodeId) {
        OpencodeNode node = requireNode(nodeId);
        if ("REVOKED".equals(node.getStatus())) return "已撤销";
        if ("REJECTED".equals(node.getStatus())) return "已拒绝";
        if (PENDING.equals(node.getStatus())) return "等待批准";
        return isOnline(nodeId) ? "在线" : "离线";
    }

    public void markSeen(Long nodeId) {
        mapper.update(null, new LambdaUpdateWrapper<OpencodeNode>()
                .eq(OpencodeNode::getId, nodeId)
                .set(OpencodeNode::getLastSeenAt, LocalDateTime.now()));
    }

    public OpencodeNode requireNode(Long id) {
        OpencodeNode node = mapper.selectById(id);
        if (node == null) throw new IllegalArgumentException("节点不存在");
        return node;
    }

    private OpencodeNodeVO toVO(OpencodeNode node) {
        OpencodeNodeVO vo = new OpencodeNodeVO();
        vo.setId(node.getId());
        vo.setName(node.getName());
        vo.setPlatform(node.getPlatform());
        vo.setAgentVersion(node.getAgentVersion());
        vo.setStatus(node.getStatus());
        vo.setOnline(isOnline(node.getId()));
        vo.setLastSeenAt(node.getLastSeenAt());
        vo.setCapabilities(readCapabilities(node.getCapabilities()));
        vo.setApprovedCapabilities(readCapabilities(node.getApprovedCapabilities()));
        vo.setCapabilitiesPending(!new HashSet<>(vo.getCapabilities()).equals(new HashSet<>(vo.getApprovedCapabilities()))
                && APPROVED.equals(node.getStatus()));
        return vo;
    }

    private void validateHello(NodeHelloRequest hello) {
        if (hello == null || !"1".equals(hello.getProtocolVersion())) throw new IllegalArgumentException("节点协议版本不兼容");
        if (!StringUtils.hasText(hello.getPairingCode()) || hello.getPairingCode().length() > 100) throw new IllegalArgumentException("配对码无效");
        if (!StringUtils.hasText(hello.getNodeKey()) || !hello.getNodeKey().matches("[A-Za-z0-9_-]{16,64}")) throw new IllegalArgumentException("节点标识无效");
        if (!StringUtils.hasText(hello.getName()) || hello.getName().trim().length() > 100) throw new IllegalArgumentException("节点名称无效");
        validateCapabilities(hello.getCapabilities());
    }

    private void validateCapabilities(List<String> capabilities) {
        if (capabilities == null || capabilities.isEmpty() || capabilities.size() > OpencodeNodeCommand.values().length) {
            throw new IllegalArgumentException("节点能力列表无效");
        }
        Set<String> unique = new HashSet<>(capabilities);
        if (unique.size() != capabilities.size() || !OpencodeNodeCommand.valuesSet().containsAll(unique)) {
            throw new IllegalArgumentException("节点声明了不支持的能力");
        }
    }

    private String pairKey(String code) { return PAIR_KEY_PREFIX + sha256(code); }

    private String writeCapabilities(List<String> values) {
        try { return objectMapper.writeValueAsString(values); }
        catch (Exception e) { throw new IllegalStateException("无法保存节点能力", e); }
    }

    private List<String> readCapabilities(String json) {
        if (!StringUtils.hasText(json)) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<List<String>>() {}); }
        catch (Exception e) { log.warn("OpenCode 节点能力数据格式错误"); return List.of(); }
    }

    private String trimOrNull(String value, int maxLength) {
        if (!StringUtils.hasText(value)) return null;
        String trimmed = value.trim();
        return trimmed.substring(0, Math.min(trimmed.length(), maxLength));
    }

    private String randomSecret(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception e) { throw new IllegalStateException("当前环境不支持 SHA-256", e); }
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) return false;
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.US_ASCII), right.getBytes(StandardCharsets.US_ASCII));
    }

    private void ensureEnabled() {
        if (!properties.isEnabled()) throw new IllegalStateException("OpenCode 节点通道未启用");
    }
}
