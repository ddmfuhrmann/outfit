package github.io.ddmfuhrmann.outfit.shared;

import github.io.ddmfuhrmann.outfit.shared.application.dto.*;
import github.io.ddmfuhrmann.outfit.shared.domain.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    @Test
    void createUserWithAdminTokenReturns201() {
        String token = loginAdmin();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        var request = new CreateUserRequest("testuser", "password123", "Test User", UserRole.USER);
        var response = rest.exchange("/shared/users", HttpMethod.POST,
                new HttpEntity<>(request, headers), UserResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().login()).isEqualTo("testuser");
    }

    @Test
    void createUserWithUserTokenReturns403() {
        String adminToken = loginAdmin();
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        adminHeaders.setContentType(MediaType.APPLICATION_JSON);
        rest.exchange("/shared/users", HttpMethod.POST,
                new HttpEntity<>(new CreateUserRequest("regularuser", "pass1234", "Regular", UserRole.USER), adminHeaders),
                UserResponse.class);

        String userToken = login("regularuser", "pass1234");
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userToken);
        userHeaders.setContentType(MediaType.APPLICATION_JSON);
        var response = rest.exchange("/shared/users", HttpMethod.POST,
                new HttpEntity<>(new CreateUserRequest("another", "pass1234", "Another", UserRole.USER), userHeaders),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void listUsersWithAdminTokenReturnsPaged() {
        String token = loginAdmin();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var response = rest.exchange("/shared/users?page=0&size=20", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("content", "totalElements");
    }

    @Test
    void updateUserReturns200() {
        String token = loginAdmin();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        var created = rest.exchange("/shared/users", HttpMethod.POST,
                new HttpEntity<>(new CreateUserRequest("updateme", "pass1234", "Original Name", UserRole.USER), headers),
                UserResponse.class).getBody();
        assertThat(created).isNotNull();

        var updated = rest.exchange("/shared/users/" + created.login(), HttpMethod.PUT,
                new HttpEntity<>(new UpdateUserRequest("Updated Name", null), headers),
                UserResponse.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).isNotNull();
        assertThat(updated.getBody().name()).isEqualTo("Updated Name");
    }

    @Test
    void deactivateUserSetsActiveFalse() {
        String token = loginAdmin();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        var created = rest.exchange("/shared/users", HttpMethod.POST,
                new HttpEntity<>(new CreateUserRequest("todeactivate", "pass1234", "ToDeactivate", UserRole.USER), headers),
                UserResponse.class).getBody();
        assertThat(created).isNotNull();

        var deleteResponse = rest.exchange("/shared/users/" + created.login(), HttpMethod.DELETE,
                new HttpEntity<>(headers), Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var getResponse = rest.exchange("/shared/users/" + created.login(), HttpMethod.GET,
                new HttpEntity<>(headers), UserResponse.class);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().active()).isFalse();
    }

    @Test
    void deactivateAlreadyInactiveUserReturns422() {
        String token = loginAdmin();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        var created = rest.exchange("/shared/users", HttpMethod.POST,
                new HttpEntity<>(new CreateUserRequest("twicedeactivate", "pass1234", "Twice", UserRole.USER), headers),
                UserResponse.class).getBody();
        assertThat(created).isNotNull();

        rest.exchange("/shared/users/" + created.login(), HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

        var secondDeactivate = rest.exchange("/shared/users/" + created.login(), HttpMethod.DELETE,
                new HttpEntity<>(headers), String.class);
        assertThat(secondDeactivate.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void createUserWithBlankNameReturns400() {
        String token = loginAdmin();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        var response = rest.exchange("/shared/users", HttpMethod.POST,
                new HttpEntity<>(new CreateUserRequest("blankname", "pass1234", "  ", UserRole.USER), headers),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private String loginAdmin() {
        return login("admin", "admin");
    }

    private String login(String username, String password) {
        return rest.postForObject("/auth/login", new LoginRequest(username, password), LoginResponse.class).token();
    }
}
