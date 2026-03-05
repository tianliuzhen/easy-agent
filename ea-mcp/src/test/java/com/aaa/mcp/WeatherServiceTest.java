package com.aaa.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;

class WeatherServiceTest {


    public static void main(String[] args) {
        // stdioTest();

        // sseHttpTest();

        streamableHTTP();
    }



    /**
     * stdio 是一种用于本地进程间通信的传输方式。
     * 在这种模式下，MCP客户端（比如你的Claude Desktop应用）会像一个指挥官一样，
     * 直接启动MCP服务器作为一个子进程
     */
    public static void stdioTest() {
        var stdioParams = ServerParameters.builder("java")
                .args("-jar", "F:\\WorkSpace\\MyGithub\\easy-agent\\mcp\\target\\mcp-0.0.1-SNAPSHOT.jar")
                .build();

        var stdioTransport = new StdioClientTransport(stdioParams, McpJsonMapper.createDefault());

        var mcpClient = McpClient.sync(stdioTransport).build();

        mcpClient.initialize();

        McpSchema.ListToolsResult toolsList = mcpClient.listTools();
        System.out.println();
        System.out.println("toolsList = " + toolsList);
        System.out.println();
        McpSchema.CallToolResult weather = mcpClient.callTool(
                new McpSchema.CallToolRequest("getWeather",
                        Map.of("city", "北京")));
        System.out.println("weather = " + weather);

        mcpClient.closeGracefully();
    }

    /**
     * 已废弃
     * <p>
     * MCP早期设计的远程通信方案，它组合使用了两种机制：
     * 客户端                            服务器
     * |                                  |
     * |----HTTP POST (发送消息) -------->|
     * |                                  |
     * |<---HTTP响应 (可选) --------------|
     * |                                  |
     * |  +------------------------+      |
     * |--| 独立的SSE(SseEmitter)长连接         |----->|
     * |  | (只用来接收服务器推送)   |      |
     * |  +------------------------+      |
     * |       ↓                          |
     * |<---SSE推送 (消息1) ---------------|
     * |<---SSE推送 (消息2) ---------------|
     *
     * sseEndpoint 发送和接受可以是不相同的，配置文件是 mcp/messages ，底层默认是：/sse
     */
    public static void sseHttpTest() {
        var sseClientTransport = HttpClientSseClientTransport
                .builder("http://localhost:8083/")
                // .sseEndpoint("/sse") // 默认/sse
                .build();

        var mcpClient = McpClient.sync(sseClientTransport).build();

        mcpClient.initialize();

        McpSchema.ListToolsResult toolsList = mcpClient.listTools();
        System.out.println();
        System.out.println("toolsList = " + toolsList);
        System.out.println();
        McpSchema.CallToolResult weather = mcpClient.callTool(
                new McpSchema.CallToolRequest("getWeather",
                        Map.of("city", "北京")));
        System.out.println("weather = " + weather);

        mcpClient.closeGracefully();
    }

    /**
     * endpoint 发送和接受是相同的，必须要指定 api/mcp  和配置文件相同
     *
     * claude mcp add --transport http notion http://localhost:8083/api/mcp
     */
    private static void streamableHTTP() {
        var sseClientTransport = HttpClientStreamableHttpTransport
                .builder("http://localhost:8083/")
                .endpoint("api/mcp") // 默认：/mcp
                .build();

        var mcpClient = McpClient.sync(sseClientTransport).build();

        mcpClient.initialize();

        McpSchema.ListToolsResult toolsList = mcpClient.listTools();
        System.out.println();
        System.out.println("toolsList = " + toolsList);
        System.out.println();
        McpSchema.CallToolResult weather = mcpClient.callTool(
                new McpSchema.CallToolRequest("getWeather",
                        Map.of("city", "北京")));
        System.out.println("weather = " + weather);

        mcpClient.closeGracefully();
    }
}
