package com.aaa.easyagent.core.mapper;

import com.aaa.easyagent.core.domain.DO.EaMcpConfigDO;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface EaMcpConfigDAO extends Mapper<EaMcpConfigDO> {

    /**
     * 根据服务器名称查询 MCP 配置
     *
     * @param serverName 服务器名称
     * @return MCP 配置列表
     */
    List<EaMcpConfigDO> selectByServerName(@Param("serverName") String serverName);

    /**
     * 根据服务器名称和工具名称查询 MCP 配置
     *
     * @param serverName 服务器名称
     * @param toolName   工具名称
     * @return MCP 配置列表
     */
    List<EaMcpConfigDO> selectByServerNameAndToolName(@Param("serverName") String serverName,
                                                       @Param("toolName") String toolName);
}