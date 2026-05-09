package github.io.ddmfuhrmann.outfit.shared;

import github.io.ddmfuhrmann.outfit.shared.application.dto.*;
import github.io.ddmfuhrmann.outfit.shared.domain.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    @Test
    void getCompanyReturns200() {
        String token = loginAdmin();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var response = rest.exchange("/shared/company", HttpMethod.GET,
                new HttpEntity<>(headers), CompanyResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().cnpj()).isEqualTo("00000000000000");
        assertThat(response.getBody().companyName()).isEqualTo("Outfit Retail");
    }

    @Test
    void updateCompanyWithAdminTokenReturns200() {
        String token = loginAdmin();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        var request = new UpdateCompanyRequest("00000000000000", "Outfit Retail", "Nova Razão Social", null, null, null);
        var response = rest.exchange("/shared/company", HttpMethod.PUT,
                new HttpEntity<>(request, headers), CompanyResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().tradeName()).isEqualTo("Nova Razão Social");
    }

    @Test
    void updateCompanyWithUserTokenReturns403() {
        String adminToken = loginAdmin();
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        adminHeaders.setContentType(MediaType.APPLICATION_JSON);
        rest.exchange("/shared/users", HttpMethod.POST,
                new HttpEntity<>(new CreateUserRequest("companyuser", "pass1234", "Company User", UserRole.USER), adminHeaders),
                UserResponse.class);

        String userToken = login("companyuser", "pass1234");
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userToken);
        userHeaders.setContentType(MediaType.APPLICATION_JSON);
        var response = rest.exchange("/shared/company", HttpMethod.PUT,
                new HttpEntity<>(new UpdateCompanyRequest("00000000000000", "Outfit Retail", "Tentativa", null, null, null), userHeaders),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private String loginAdmin() {
        return login("admin", "admin");
    }

    private String login(String username, String password) {
        return rest.postForObject("/auth/login", new LoginRequest(username, password), LoginResponse.class).token();
    }
}
