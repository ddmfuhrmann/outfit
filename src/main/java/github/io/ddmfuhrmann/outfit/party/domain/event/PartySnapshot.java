package github.io.ddmfuhrmann.outfit.party.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PartySnapshot(
        Long id,
        String personType,
        String cnpj,
        String cpf,
        String legalName,
        String name,
        boolean customer,
        boolean supplier,
        boolean salesperson,
        BigDecimal commissionPercent,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        List<PartyAddressSnapshot> addresses,
        List<PartyContactSnapshot> contacts) {}
