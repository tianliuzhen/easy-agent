package com.aaa.easyagent.web.biz.mcp;

import com.aaa.easyagent.core.domain.base.BaseResult;
import com.aaa.easyagent.core.domain.request.McpBindRequest;
import com.aaa.easyagent.core.domain.request.McpServerConfigRequest;
import com.aaa.easyagent.core.domain.request.McpUnbindRequest;
import com.aaa.easyagent.core.domain.result.McpServerConfigResult;
import com.aaa.easyagent.core.domain.result.McpToolInfoResult;
import com.aaa.easyagent.core.service.McpServerConfigService;
import com.aaa.easyagent.core.service.McpToolIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MCP绑定关系管理控制器
 * 负责MCP与Agent的绑定/解绑操作
 *
 * @author liuzhen.tian
 * @version 1.0 McpRelationController.java  2026/4/4
 */
@Slf4j
@RestController
@RequestMapping("eaAgent/mcp/")
@RequiredArgsConstructor
public class McpRelationController {

    private final McpToolIntegrationService mcpToolIntegrationService;
    private final McpServerConfigService mcpServerConfigService;

    /**
     * 根据用户ID获取MCP配置列表
     *
     * @return MCP配置结果列表
     */
    @GetMapping("getMcpConfigByUserId/")
    public BaseResult getMcpConfigByUserId() {
        try {
            // TODO: 从上下文获取当前用户ID，暂时返回所有配置
            List<McpServerConfigResult> results = mcpToolIntegrationService.getAllMcpConfigs();
            return BaseResult.buildSuc(results);
        } catch (Exception e) {
            log.error("获取用户MCP配置列表失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 获取官方MCP配置列表（user_id = 0）
     *
     * @return 官方MCP配置结果列表
     */
    @GetMapping("getOfficialMcpConfigs/")
    public BaseResult getOfficialMcpConfigs() {
        try {
            List<McpServerConfigResult> results = mcpToolIntegrationService.getOfficialMcpConfigs();
            return BaseResult.buildSuc(results);
        } catch (Exception e) {
            log.error("获取官方MCP配置列表失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 根据Agent ID获取已绑定的MCP列表
     *
     * @param agentId Agent标识
     * @return 已绑定的MCP配置列表
     */
    @GetMapping("listBoundMcpByAgentId/{agentId}")
    public BaseResult listBoundMcpByAgentId(@PathVariable Long agentId) {
        try {
            List<McpServerConfigResult> results = mcpToolIntegrationService.getBoundMcpConfigsByAgentId(agentId);
            return BaseResult.buildSuc(results);
        } catch (Exception e) {
            log.error("获取Agent绑定的MCP列表失败: agentId={}", agentId, e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 绑定MCP到Agent
     *
     * @param request 绑定请求
     * @return 绑定结果
     */
    @PostMapping("bind")
    public BaseResult bindMcp(@RequestBody McpBindRequest request) {
        try {
            Long agentId = Long.valueOf(request.getAgentId());
            Long mcpConfigId = request.getMcpConfigId();
            String bindingConfig = request.getBindingConfig();

            boolean success = mcpToolIntegrationService.bindMcpToolToAgent(agentId, mcpConfigId, bindingConfig);
            return BaseResult.buildSuc(success);
        } catch (Exception e) {
            log.error("绑定MCP到Agent失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 从Agent解绑MCP
     *
     * @param request 解绑请求
     * @return 解绑结果
     */
    @PostMapping("unbind")
    public BaseResult unbindMcp(@RequestBody McpUnbindRequest request) {
        try {
            Long agentId = Long.valueOf(request.getAgentId());
            Long mcpConfigId = request.getMcpConfigId();

            boolean success = mcpToolIntegrationService.unbindMcpToolFromAgent(agentId, mcpConfigId);
            return BaseResult.buildSuc(success);
        } catch (Exception e) {
            log.error("从Agent解绑MCP失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    // ==================== MCP Server 配置管理接口 ====================

    /**
     * 获取所有 MCP Server 配置列表
     *
     * @return MCP Server 配置列表
     */
    @GetMapping("server")
    public BaseResult listAllConfigs() {
        try {
            List<McpServerConfigResult> results = mcpServerConfigService.listAllConfigs();
            return BaseResult.buildSuc(results);
        } catch (Exception e) {
            log.error("获取MCP Server配置列表失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询 MCP Server 配置
     *
     * @param id 配置 ID
     * @return 配置信息
     */
    @GetMapping("server/{id}")
    public BaseResult getConfigById(@PathVariable Long id) {
        try {
            McpServerConfigResult result = mcpServerConfigService.getConfigById(id);
            return BaseResult.buildSuc(result);
        } catch (Exception e) {
            log.error("获取MCP Server配置失败: id={}", id, e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 根据服务器名称查询配置
     *
     * @param serverName 服务器名称
     * @return 配置列表
     */
    @GetMapping("server/name/{serverName}")
    public BaseResult listConfigsByServerName(@PathVariable String serverName) {
        try {
            List<McpServerConfigResult> results = mcpServerConfigService.listConfigsByServerName(serverName);
            return BaseResult.buildSuc(results);
        } catch (Exception e) {
            log.error("根据名称获取MCP Server配置失败: serverName={}", serverName, e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 新增 MCP Server 配置
     *
     * @param request 配置请求
     * @return 配置 ID
     */
    @PostMapping("server")
    public BaseResult createConfig(@RequestBody McpServerConfigRequest request) {
        try {
            Long id = mcpServerConfigService.createConfig(request);
            return BaseResult.buildSuc(id);
        } catch (Exception e) {
            log.error("创建MCP Server配置失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 更新 MCP Server 配置
     *
     * @param id      配置 ID
     * @param request 配置请求
     * @return 是否成功
     */
    @PutMapping("server/{id}")
    public BaseResult updateConfig(@PathVariable Long id, @RequestBody McpServerConfigRequest request) {
        try {
            boolean success = mcpServerConfigService.updateConfig(id, request);
            return BaseResult.buildSuc(success);
        } catch (Exception e) {
            log.error("更新MCP Server配置失败: id={}", id, e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 删除 MCP Server 配置
     *
     * @param id 配置 ID
     * @return 是否成功
     */
    @DeleteMapping("server/{id}")
    public BaseResult deleteConfig(@PathVariable Long id) {
        try {
            boolean success = mcpServerConfigService.deleteConfig(id);
            return BaseResult.buildSuc(success);
        } catch (Exception e) {
            log.error("删除MCP Server配置失败: id={}", id, e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 从 MCP Server 获取工具列表
     *
     * @param id 配置 ID
     * @return 可用工具列表
     */
    @PostMapping("server/{id}/tools/fetch")
    public BaseResult fetchToolsFromServer(@PathVariable Long id) {
        try {
            List<McpToolInfoResult> tools = mcpServerConfigService.fetchToolsFromServer(id);
            return BaseResult.buildSuc(tools);
        } catch (Exception e) {
            log.error("获取MCP Server工具列表失败: id={}", id, e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 直接获取 MCP Server 工具列表（不保存配置）
     *
     * @param request 配置请求
     * @return 可用工具列表
     */
    @PostMapping("server/tools/fetch")
    public BaseResult fetchToolsFromServerDirect(@RequestBody McpServerConfigRequest request) {
        try {
            List<McpToolInfoResult> tools = mcpServerConfigService.fetchToolsFromServer(request);
            return BaseResult.buildSuc(tools);
        } catch (Exception e) {
            log.error("直接获取MCP Server工具列表失败", e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 获取指定 Server 的工具列表
     *
     * @param id 配置 ID
     * @return 工具列表
     */
    @GetMapping("server/{id}/tools")
    public BaseResult listTools(@PathVariable Long id) {
        try {
            List<McpToolInfoResult> tools = mcpServerConfigService.listTools(id);
            return BaseResult.buildSuc(tools);
        } catch (Exception e) {
            log.error("获取MCP Server工具列表失败: id={}", id, e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 同步 MCP Server 的工具列表到本地配置
     *
     * @param id 配置 ID
     * @return 同步的工具数量
     */
    @PostMapping("server/{id}/sync")
    public BaseResult syncTools(@PathVariable Long id) {
        try {
            int count = mcpServerConfigService.syncTools(id);
            return BaseResult.buildSuc(count);
        } catch (Exception e) {
            log.error("同步MCP Server工具列表失败: id={}", id, e);
            return BaseResult.buildFail(e.getMessage());
        }
    }
}
