package github.io.ddmfuhrmann.outfit.query.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PartyDocument(
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
        List<AddressDocument> addresses,
        List<ContactDocument> contacts) {}
