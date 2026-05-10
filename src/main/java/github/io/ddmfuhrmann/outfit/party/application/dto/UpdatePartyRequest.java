package github.io.ddmfuhrmann.outfit.party.application.dto;

import java.math.BigDecimal;

public record UpdatePartyRequest(String legalName, String name, BigDecimal commissionPercent) {}
