package github.io.ddmfuhrmann.outfit.query.application.usecase;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import github.io.ddmfuhrmann.outfit.party.domain.event.PartySnapshot;
import github.io.ddmfuhrmann.outfit.query.application.dto.AddressDocument;
import github.io.ddmfuhrmann.outfit.query.application.dto.ContactDocument;
import github.io.ddmfuhrmann.outfit.query.application.dto.PartyDocument;
import github.io.ddmfuhrmann.outfit.query.application.exception.IndexingException;
import github.io.ddmfuhrmann.outfit.shared.application.usecase.GetCityUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class IndexPartyUseCase {

    private static final String INDEX = "parties";

    private final ElasticsearchClient esClient;
    private final GetCityUseCase getCity;

    public IndexPartyUseCase(ElasticsearchClient esClient, GetCityUseCase getCity) {
        this.esClient = esClient;
        this.getCity = getCity;
    }

    public void execute(PartySnapshot snapshot) {
        var doc = new PartyDocument(
                snapshot.id(),
                snapshot.personType(),
                snapshot.cnpj(),
                snapshot.cpf(),
                snapshot.legalName(),
                snapshot.name(),
                snapshot.customer(),
                snapshot.supplier(),
                snapshot.salesperson(),
                snapshot.commissionPercent(),
                snapshot.active(),
                snapshot.createdAt(),
                snapshot.updatedAt(),
                snapshot.addresses().stream()
                        .map(a -> {
                            var city = a.cityIbgeCode() != null ? getCity.execute(a.cityIbgeCode()) : null;
                            return new AddressDocument(
                                    a.id(),
                                    a.street(),
                                    a.neighborhood(),
                                    a.zipCode(),
                                    a.number(),
                                    a.complement(),
                                    a.cityIbgeCode(),
                                    city != null ? city.cityName() : null,
                                    city != null ? city.stateAbbr() : null);
                        })
                        .toList(),
                snapshot.contacts().stream()
                        .map(c -> new ContactDocument(
                                c.id(),
                                c.classification(),
                                c.description()))
                        .toList());
        try {
            esClient.index(i -> i
                    .index(INDEX)
                    .id(doc.id().toString())
                    .document(doc)
                    .refresh(Refresh.True));
            log.info("Indexed party {}", snapshot.id());
        } catch (IOException e) {
            throw new IndexingException("Failed to index party " + snapshot.id(), e);
        }
    }
}
