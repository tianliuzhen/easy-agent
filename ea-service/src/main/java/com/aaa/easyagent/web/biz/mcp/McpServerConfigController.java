package com.aaa.easyagent.web.biz.mcp;

import com.aaa.easyagent.core.domain.base.BaseResult;
import com.aaa.easyagent.core.domain.request.McpServerConfigRequest;
import com.aaa.easyagent.core.domain.result.McpServerConfigResult;
import com.aaa.easyagent.core.domain.result.McpToolInfoResult;
import com.aaa.easyagent.core.service.McpServerConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MCP Server 配置管理控制器
 *
 * @author liuzhen.tian
 * @version 1.0 McpServerConfigController.java  2026/4/3
 */
@Slf4j
@RestController
@RequestMapping("eaAgent/mcp/")
@RequiredArgsConstructor
public class McpServerConfigController {

    private final McpServerConfigService mcpServerConfigService;

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
            log.error("创建 MCP Server 配置失败", e);
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
            log.error("更新 MCP Server 配置失败: id={}", id, e);
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
            log.error("删除 MCP Server 配置失败: id={}", id, e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 根据 ID 查询配置
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
            log.error("查询 MCP Server 配置失败: id={}", id, e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 查询所有配置列表
     *
     * @return 配置列表
     */
    @GetMapping("server")
    public BaseResult listAllConfigs() {
        try {
            List<McpServerConfigResult> results = mcpServerConfigService.listAllConfigs();
            return BaseResult.buildSuc(results);
        } catch (Exception e) {
            log.error("查询 MCP Server 配置列表失败", e);
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
            log.error("查询 MCP Server 配置失败: serverName={}", serverName, e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 测试 MCP Server 连接
     *
     * @param id 配置 ID
     * @return 可用工具列表
     */
    @PostMapping("server/{id}/test")
    public BaseResult testConnection(@PathVariable Long id) {
        try {
            List<McpToolInfoResult> tools = mcpServerConfigService.testConnection(id);
            return BaseResult.buildSuc(tools);
        } catch (Exception e) {
            log.error("测试 MCP Server 连接失败: id={}", id, e);
            return BaseResult.buildFail(e.getMessage());
        }
    }

    /**
     * 直接测试 MCP Server 连接（不保存配置）
     *
     * @param request 配置请求
     * @return 可用工具列表
     */
    @PostMapping("server/test")
    public BaseResult testConnectionDirect(@RequestBody McpServerConfigRequest request) {
        try {
            List<McpToolInfoResult> tools = mcpServerConfigService.testConnection(request);
            return BaseResult.buildSuc(tools);
        } catch (Exception e) {
            log.error("测试 MCP Server 连接失败", e);
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
            log.error("获取 MCP Server 工具列表失败: id={}", id, e);
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
            log.error("同步 MCP Server 工具列表失败: id={}", id, e);
            return BaseResult.buildFail(e.getMessage());
        }
    }
}
