package github.io.ddmfuhrmann.outfit.shared;

import github.io.ddmfuhrmann.outfit.shared.application.dto.LoginRequest;
import github.io.ddmfuhrmann.outfit.shared.application.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    @Test
    void loginSuccess() {
        var response = rest.postForEntity("/auth/login",
                new LoginRequest("admin", "admin"), LoginResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
        assertThat(response.getBody().expiresAt()).isNotNull();
    }

    @Test
    void loginFailure() {
        var response = rest.postForEntity("/auth/login",
                new LoginRequest("admin", "wrongpassword"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpointWithoutToken() {
        var response = rest.getForEntity("/shared/cities", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpointWithValidToken() {
        String token = login("admin", "admin");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var response = rest.exchange("/shared/cities", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    String login(String login, String password) {
        return rest.postForObject("/auth/login", new LoginRequest(login, password), LoginResponse.class).token();
    }
}
