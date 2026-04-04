package com.aaa.easyagent.core.service;

import com.aaa.easyagent.core.domain.DO.EaMcpConfigDO;
import com.aaa.easyagent.core.domain.request.McpServerConfigRequest;
import com.aaa.easyagent.core.domain.result.McpServerConfigResult;
import com.aaa.easyagent.core.domain.result.McpToolInfoResult;

import java.util.List;

/**
 * MCP Server 配置管理服务接口
 *
 * @author liuzhen.tian
 * @version 1.0 McpServerConfigService.java  2026/4/3
 */
public interface McpServerConfigService {

    /**
     * 新增 MCP Server 配置
     *
     * @param request 配置请求
     * @return 配置 ID
     */
    Long createConfig(McpServerConfigRequest request);

    /**
     * 更新 MCP Server 配置
     *
     * @param id      配置 ID
     * @param request 配置请求
     * @return 是否成功
     */
    boolean updateConfig(Long id, McpServerConfigRequest request);

    /**
     * 删除 MCP Server 配置
     *
     * @param id 配置 ID
     * @return 是否成功
     */
    boolean deleteConfig(Long id);

    /**
     * 根据 ID 查询配置
     *
     * @param id 配置 ID
     * @return 配置信息
     */
    McpServerConfigResult getConfigById(Long id);

    /**
     * 查询所有配置列表
     *
     * @return 配置列表
     */
    List<McpServerConfigResult> listAllConfigs();

    /**
     * 根据服务器名称查询配置
     *
     * @param serverName 服务器名称
     * @return 配置列表
     */
    List<McpServerConfigResult> listConfigsByServerName(String serverName);

    /**
     * 从 MCP Server 获取工具列表
     *
     * @param id 配置 ID
     * @return 可用工具列表
     */
    List<McpToolInfoResult> fetchToolsFromServer(Long id);

    /**
     * 直接测试 MCP Server 连接并获取工具列表（不保存配置）
     *
     * @param request 配置请求
     * @return 可用工具列表
     */
    List<McpToolInfoResult> fetchToolsFromServer(McpServerConfigRequest request);

    /**
     * 获取指定 Server 的工具列表
     *
     * @param id 配置 ID
     * @return 工具列表
     */
    List<McpToolInfoResult> listTools(Long id);

    /**
     * 同步 MCP Server 的工具列表到本地配置
     *
     * @param id 配置 ID
     * @return 同步的工具数量
     */
    int syncTools(Long id);

    /**
     * 根据 serverName 和 toolName 查询 MCP 配置
     * 优先精确匹配 serverName + toolName，如未找到则仅匹配 serverName
     *
     * @param serverName 服务器名称
     * @param toolName   工具名称
     * @return MCP 配置，未找到返回 null
     */
    EaMcpConfigDO getMcpConfig(String serverName, String toolName);
}
