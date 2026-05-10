package github.io.ddmfuhrmann.outfit.shared;

import github.io.ddmfuhrmann.outfit.shared.application.dto.LoginRequest;
import github.io.ddmfuhrmann.outfit.shared.application.dto.LoginResponse;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIT {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static final ElasticsearchContainer ELASTIC =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.13.4")
                    .withEnv("xpack.security.enabled", "false");

    static {
        POSTGRES.start();
        ELASTIC.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("elasticsearch.uris", () -> "http://" + ELASTIC.getHttpHostAddress());
    }

    private static String cachedToken;

    protected HttpHeaders authHeaders(TestRestTemplate rest) {
        if (cachedToken == null) {
            cachedToken = rest.postForObject("/auth/login",
                    new LoginRequest("admin", "admin"), LoginResponse.class).token();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(cachedToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
