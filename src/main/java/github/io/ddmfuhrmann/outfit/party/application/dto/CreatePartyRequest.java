package github.io.ddmfuhrmann.outfit.party.application.dto;

import github.io.ddmfuhrmann.outfit.party.domain.model.PersonType;

import java.math.BigDecimal;
import java.util.List;

public record CreatePartyRequest(
        PersonType personType,
        String cnpj,
        String cpf,
        String legalName,
        String name,
        boolean customer,
        boolean supplier,
        boolean salesperson,
        BigDecimal commissionPercent,
        List<AddAddressRequest> addresses,
        List<AddContactRequest> contacts
) {}
