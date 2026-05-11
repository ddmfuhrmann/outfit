package github.io.ddmfuhrmann.outfit.query;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import github.io.ddmfuhrmann.outfit.shared.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticsearchIndexInitializerIT extends AbstractIT {

    @Autowired
    ElasticsearchClient esClient;

    private static final List<String> ALL_INDICES = List.of("products", "parties", "brands", "colors", "categories", "sizes");

    @Test
    void allSixIndicesExistAfterStartup() throws IOException {
        for (String name : ALL_INDICES) {
            assertThat(esClient.indices().exists(r -> r.index(name)).value())
                    .as("Index '%s' should exist", name)
                    .isTrue();
        }
    }

    @Test
    void productsIndex_descriptionField_isSearchAsYouType() throws IOException {
        var mapping = esClient.indices().getMapping(r -> r.index("products"));
        var prop = mapping.result().get("products").mappings().properties().get("description");
        assertThat(prop._kind()).isEqualTo(Property.Kind.SearchAsYouType);
    }

    @Test
    void partiesIndex_nameAndLegalNameFields_areSearchAsYouType() throws IOException {
        var mapping = esClient.indices().getMapping(r -> r.index("parties"));
        var properties = mapping.result().get("parties").mappings().properties();
        assertThat(properties.get("name")._kind()).isEqualTo(Property.Kind.SearchAsYouType);
        assertThat(properties.get("legalName")._kind()).isEqualTo(Property.Kind.SearchAsYouType);
    }
}
