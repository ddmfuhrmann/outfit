package github.io.ddmfuhrmann.outfit.query;

import github.io.ddmfuhrmann.outfit.catalog.application.dto.BrandRequest;
import github.io.ddmfuhrmann.outfit.catalog.application.dto.BrandResponse;
import github.io.ddmfuhrmann.outfit.query.application.dto.RefDocument;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import github.io.ddmfuhrmann.outfit.shared.application.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class SearchBrandsUseCaseIT extends AbstractIT {

    @Autowired
    TestRestTemplate rest;

    @Test
    void searchByPrefix_returnsMatchingBrand() {
        var headers = authHeaders(rest);
        var resp = rest.exchange("/catalog/brands", HttpMethod.POST,
                new HttpEntity<>(new BrandRequest("Adidas"), headers), BrandResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long brandId = resp.getBody().id();

        var searchResp = rest.exchange("/catalog/brands?q=Adi", HttpMethod.GET,
                new HttpEntity<>(authHeaders(rest)),
                new ParameterizedTypeReference<PageResponse<RefDocument>>() {});

        assertThat(searchResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(searchResp.getBody()).isNotNull();
        assertThat(searchResp.getBody().content())
                .extracting(RefDocument::id)
                .contains(brandId);
    }
}
