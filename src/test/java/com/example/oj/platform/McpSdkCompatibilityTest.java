package com.example.oj.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(McpSdkCompatibilityTest.Fixture.class)
class McpSdkCompatibilityTest {
    @LocalServerPort int port;

    @Test
    void realSdkDiscoversAndExecutesStructuredToolOnCurrentSpringVersion() {
        var transport = HttpClientStreamableHttpTransport.builder("http://127.0.0.1:" + port)
                .endpoint("/contract-mcp").openConnectionOnStartup(false).build();
        try (var client = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(5)).build()) {
            assertEquals("2025-03-26", client.initialize().protocolVersion());
            var definitions = client.listTools().tools();
            assertEquals(1, definitions.size());
            assertNotNull(definitions.get(0).outputSchema());
            var result = client.callTool(new McpSchema.CallToolRequest("contract_fixture", Map.of("text", "fixture")));
            assertFalse(Boolean.TRUE.equals(result.isError()));
            assertEquals("fixture", result.structuredContent().get("text"));
        }
    }

    @TestConfiguration
    static class Fixture {
        @Bean
        HttpServletStreamableServerTransportProvider contractTransport(ObjectMapper json) {
            return HttpServletStreamableServerTransportProvider.builder()
                    .objectMapper(json).mcpEndpoint("/contract-mcp").build();
        }
        @Bean
        ServletRegistrationBean<HttpServletStreamableServerTransportProvider> contractServlet(
                HttpServletStreamableServerTransportProvider transport) {
            var servlet = new ServletRegistrationBean<>(transport, "/contract-mcp");
            servlet.setAsyncSupported(true); return servlet;
        }
        @Bean(destroyMethod = "close")
        McpSyncServer contractServer(HttpServletStreamableServerTransportProvider transport) {
            String schema = "{\"type\":\"object\",\"properties\":{\"text\":{\"type\":\"string\"}},\"required\":[\"text\"],\"additionalProperties\":false}";
            var tool = McpSchema.Tool.builder().name("contract_fixture").description("Compatibility fixture only")
                    .inputSchema(schema).outputSchema(schema).build();
            return McpServer.sync(transport).serverInfo("contract-fixture", "0.1.0")
                    .capabilities(McpSchema.ServerCapabilities.builder().tools(false).build())
                    .tool(tool, (exchange, args) -> McpSchema.CallToolResult.builder()
                            .structuredContent(Map.of("text", args.get("text"))).isError(false).build()).build();
        }
    }
}
