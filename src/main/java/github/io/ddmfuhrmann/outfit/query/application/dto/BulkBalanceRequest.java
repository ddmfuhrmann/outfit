package github.io.ddmfuhrmann.outfit.query.application.dto;

import java.util.List;

public record BulkBalanceRequest(List<Long> skuIds) {}
