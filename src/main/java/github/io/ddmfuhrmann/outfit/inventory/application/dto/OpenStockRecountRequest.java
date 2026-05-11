package github.io.ddmfuhrmann.outfit.inventory.application.dto;

import java.time.LocalDate;

public record OpenStockRecountRequest(LocalDate date, String notes) {}
