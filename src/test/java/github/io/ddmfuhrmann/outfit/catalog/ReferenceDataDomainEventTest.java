package github.io.ddmfuhrmann.outfit.catalog;

import github.io.ddmfuhrmann.outfit.catalog.domain.event.*;
import github.io.ddmfuhrmann.outfit.catalog.domain.model.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReferenceDataDomainEventTest {

    @Test
    void sizeRenameRegistersEvent() {
        var size = Size.create("P");
        size.resetRegisteredEvents();
        size.rename("G");
        assertThat(size.getRegisteredEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(SizeRenamed.class);
        assertThat(((SizeRenamed) size.getRegisteredEvents().iterator().next()).newDescription()).isEqualTo("G");
    }

    @Test
    void sizeCreateWithBlankDescriptionThrows() {
        assertThatThrownBy(() -> Size.create("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sizeRenameWithBlankDescriptionThrows() {
        var size = Size.create("P");
        assertThatThrownBy(() -> size.rename(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void colorRenameRegistersEvent() {
        var color = Color.create("Azul");
        color.resetRegisteredEvents();
        color.rename("Vermelho");
        assertThat(color.getRegisteredEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(ColorRenamed.class);
        assertThat(((ColorRenamed) color.getRegisteredEvents().iterator().next()).newDescription()).isEqualTo("Vermelho");
    }

    @Test
    void brandRenameRegistersEvent() {
        var brand = Brand.create("Nike");
        brand.resetRegisteredEvents();
        brand.rename("Adidas");
        assertThat(brand.getRegisteredEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(BrandRenamed.class);
        assertThat(((BrandRenamed) brand.getRegisteredEvents().iterator().next()).newDescription()).isEqualTo("Adidas");
    }

    @Test
    void categoryRenameRegistersEvent() {
        var category = Category.create("Camisetas", "6109.10.00");
        category.resetRegisteredEvents();
        category.rename("Camisetas Polo", "6105.10.00");
        assertThat(category.getRegisteredEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(CategoryRenamed.class);
        assertThat(((CategoryRenamed) category.getRegisteredEvents().iterator().next()).newDescription()).isEqualTo("Camisetas Polo");
    }

    @Test
    void addSupplierToEmptyBrandSucceeds() {
        var brand = Brand.create("TestBrand");
        brand.addSupplier(123L);
        assertThat(brand.getSupplierIds()).containsExactly(123L);
    }

    @Test
    void addDuplicateSupplierThrowsIllegalState() {
        var brand = Brand.create("TestBrand");
        brand.addSupplier(123L);
        assertThatThrownBy(() -> brand.addSupplier(123L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void addNullSupplierThrowsIllegalArgument() {
        var brand = Brand.create("TestBrand");
        assertThatThrownBy(() -> brand.addSupplier(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeSupplierSucceeds() {
        var brand = Brand.create("TestBrand");
        brand.addSupplier(123L);
        brand.removeSupplier(123L);
        assertThat(brand.getSupplierIds()).isEmpty();
    }

    @Test
    void removeAbsentSupplierThrowsIllegalState() {
        var brand = Brand.create("TestBrand");
        assertThatThrownBy(() -> brand.removeSupplier(999L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void removeNullSupplierThrowsIllegalArgument() {
        var brand = Brand.create("TestBrand");
        assertThatThrownBy(() -> brand.removeSupplier(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
