package github.io.ddmfuhrmann.outfit.sales.application.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReturnItemsRequest(@NotEmpty List<ReturnItemRequest> items) {}
