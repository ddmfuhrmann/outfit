package github.io.ddmfuhrmann.outfit.shared;

import github.io.ddmfuhrmann.outfit.shared.application.dto.LoginRequest;
import github.io.ddmfuhrmann.outfit.shared.application.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class CityControllerIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void listCitiesReturnsPaged() {
        String token = login();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var response = rest.exchange("/shared/cities?page=0&size=20", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("content");
    }

    @Test
    void getCityNotFoundReturns404() {
        String token = login();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var response = rest.exchange("/shared/cities/99999", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getCityByIdReturns200() {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO city (ibge_city_code, ibge_state_code, city_name, state_name, state_abbr, created_at, updated_at)" +
                " VALUES (4314902, 43, 'Porto Alegre', 'Rio Grande do Sul', 'RS', now(), now()) RETURNING id",
                Long.class);
        String token = login();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var response = rest.exchange("/shared/cities/" + id, HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String login() {
        return rest.postForObject("/auth/login", new LoginRequest("admin", "admin"), LoginResponse.class).token();
    }
}
