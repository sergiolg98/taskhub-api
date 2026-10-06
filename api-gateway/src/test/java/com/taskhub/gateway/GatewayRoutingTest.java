package com.taskhub.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

// Two fake downstream services (plain JDK HTTP servers) that echo who they are, the path and the headers they received.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    private static final String P = "spring.cloud.gateway.server.webflux.";
    private static final HttpServer auth;
    private static final HttpServer task;
    private static final int deadPort;

    // Static initializer: the fakes must exist before Spring reads the dynamic properties.
    static {
        try {
            auth = fake("auth");
            task = fake("task");
            try (var s = new ServerSocket(0)) {
                deadPort = s.getLocalPort();     // nothing listens here: a service that is down
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @AfterAll
    static void stopFakes() {
        auth.stop(0);
        task.stop(0);
    }

    @DynamicPropertySource
    static void routes(DynamicPropertyRegistry r) {
        r.add(P + "routes[0].id", () -> "auth-service");
        r.add(P + "routes[0].uri", () -> "http://localhost:" + auth.getAddress().getPort());
        r.add(P + "routes[0].predicates[0]", () -> "Path=/auth/**,/users/**,/admin/users/**");
        r.add(P + "routes[1].id", () -> "task-service");
        r.add(P + "routes[1].uri", () -> "http://localhost:" + task.getAddress().getPort());
        r.add(P + "routes[1].predicates[0]", () -> "Path=/tasks/**,/admin/tasks/**");
        r.add(P + "routes[2].id", () -> "dead-service");
        r.add(P + "routes[2].uri", () -> "http://localhost:" + deadPort);
        r.add(P + "routes[2].predicates[0]", () -> "Path=/dead/**");
        r.add(P + "routes[3].id", () -> "gone-service");
        r.add(P + "routes[3].uri", () -> "http://no-such-service.invalid:8081");   // a name that does not resolve
        r.add(P + "routes[3].predicates[0]", () -> "Path=/gone/**");
        r.add(P + "globalcors.cors-configurations.[/**].allowed-origins", () -> "http://localhost:3000");
        r.add(P + "globalcors.cors-configurations.[/**].allowed-methods", () -> "GET,POST");
        r.add(P + "globalcors.cors-configurations.[/**].allowed-headers", () -> "Authorization,Content-Type");
        r.add(P + "globalcors.cors-configurations.[/**].exposed-headers", () -> "X-Request-Id");
        r.add("taskhub.gateway.public-paths", () -> "/auth/**");
    }

    @Autowired
    WebTestClient client;

    @Test
    void routesEachPathToItsService() {
        for (var c : new String[][]{{"/auth/login", "auth"}, {"/users/2", "auth"}, {"/admin/users", "auth"},
                {"/tasks", "task"}, {"/tasks/7", "task"}, {"/admin/tasks", "task"}}) {
            client.get().uri(c[0]).header(HttpHeaders.AUTHORIZATION, "Bearer x").exchange()
                    .expectStatus().isOk()
                    .expectBody(String.class).value(b -> assertThat(b).startsWith(c[1] + " GET " + c[0]));
        }
    }

    @Test
    void authorizationReachesTheServiceUntouched() {
        client.get().uri("/tasks").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi").exchange()
                .expectBody(String.class).value(b -> assertThat(b).contains("authorization=Bearer abc.def.ghi"));
    }

    @Test
    void publicPathNeedsNoTokenButEverythingElseDoes() {
        client.post().uri("/auth/login").exchange().expectStatus().isOk();
        client.get().uri("/tasks").exchange().expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.status").isEqualTo(401).jsonPath("$.message").isEqualTo("Unauthorized");
        client.get().uri("/admin/tasks").header(HttpHeaders.AUTHORIZATION, "Basic abc").exchange()
                .expectStatus().isUnauthorized();
        client.get().uri("/users/2").header(HttpHeaders.AUTHORIZATION, "Bearer ").exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void addsARequestIdToTheServiceAndTheClient() {
        var body = client.get().uri("/tasks").header(HttpHeaders.AUTHORIZATION, "Bearer x").exchange()
                .expectHeader().exists("X-Request-Id")
                .expectBody(String.class).returnResult();
        String id = body.getResponseHeaders().getFirst("X-Request-Id");
        assertThat(body.getResponseBody()).contains("x-request-id=" + id);
    }

    @Test
    void keepsAHarmlessClientRequestIdAndReplacesAnUnsafeOne() {
        client.get().uri("/tasks").header(HttpHeaders.AUTHORIZATION, "Bearer x").header("X-Request-Id", "abc-123")
                .exchange().expectHeader().valueEquals("X-Request-Id", "abc-123");
        var result = client.get().uri("/tasks").header(HttpHeaders.AUTHORIZATION, "Bearer x")
                .header("X-Request-Id", "bad id\twith spaces").exchange().returnResult(String.class);
        assertThat(result.getResponseHeaders().getFirst("X-Request-Id")).matches("[0-9a-f-]{36}");
    }

    @Test
    void corsPreflightIsAnsweredByTheGatewayItself() {
        client.options().uri("/tasks")
                .header("Origin", "http://localhost:3000").header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization").exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:3000");
        client.options().uri("/tasks")
                .header("Origin", "http://evil.example").header("Access-Control-Request-Method", "POST").exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void aServiceThatIsDownIs503InTheSharedErrorFormat() {
        client.get().uri("/dead/x").header(HttpHeaders.AUTHORIZATION, "Bearer x").exchange()
                .expectStatus().isEqualTo(503)
                .expectBody().jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.message").isEqualTo("Service Unavailable")
                .jsonPath("$.timestamp").exists();
    }

    @Test
    void aServiceNameThatNoLongerResolvesIs503NotA500() {
        // A stopped container disappears from Docker's DNS: the lookup fails instead of the connection being refused.
        client.get().uri("/gone/x").header(HttpHeaders.AUTHORIZATION, "Bearer x").exchange()
                .expectStatus().isEqualTo(503)
                .expectBody().jsonPath("$.status").isEqualTo(503).jsonPath("$.message").isEqualTo("Service Unavailable");
    }

    @Test
    void unknownRouteIs404InTheSharedErrorFormat() {
        client.get().uri("/nope").header(HttpHeaders.AUTHORIZATION, "Bearer x").exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.status").isEqualTo(404).jsonPath("$.message").isEqualTo("Not Found");
    }

    private static HttpServer fake(String name) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            String body = name + " " + exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath()
                    + " authorization=" + exchange.getRequestHeaders().getFirst("Authorization")
                    + " x-request-id=" + exchange.getRequestHeaders().getFirst("X-Request-Id");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return server;
    }
}
