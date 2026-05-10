package github.io.ddmfuhrmann.outfit.query.application.listener;

import github.io.ddmfuhrmann.outfit.catalog.domain.event.*;
import github.io.ddmfuhrmann.outfit.query.application.usecase.*;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class ReferenceDataListener {

    private final IndexColorUseCase indexColor;
    private final IndexBrandUseCase indexBrand;
    private final IndexCategoryUseCase indexCategory;
    private final IndexSizeUseCase indexSize;
    private final UpdateColorNameInProductsUseCase updateColorInProducts;
    private final UpdateBrandNameInProductsUseCase updateBrandInProducts;
    private final UpdateCategoryNameInProductsUseCase updateCategoryInProducts;
    private final UpdateSizeNameInProductsUseCase updateSizeInProducts;
    private final DeleteBrandFromIndexUseCase deleteBrandFromIndex;
    private final DeleteCategoryFromIndexUseCase deleteCategoryFromIndex;
    private final DeleteColorFromIndexUseCase deleteColorFromIndex;
    private final DeleteSizeFromIndexUseCase deleteSizeFromIndex;

    public ReferenceDataListener(IndexColorUseCase indexColor,
                                 IndexBrandUseCase indexBrand,
                                 IndexCategoryUseCase indexCategory,
                                 IndexSizeUseCase indexSize,
                                 UpdateColorNameInProductsUseCase updateColorInProducts,
                                 UpdateBrandNameInProductsUseCase updateBrandInProducts,
                                 UpdateCategoryNameInProductsUseCase updateCategoryInProducts,
                                 UpdateSizeNameInProductsUseCase updateSizeInProducts,
                                 DeleteBrandFromIndexUseCase deleteBrandFromIndex,
                                 DeleteCategoryFromIndexUseCase deleteCategoryFromIndex,
                                 DeleteColorFromIndexUseCase deleteColorFromIndex,
                                 DeleteSizeFromIndexUseCase deleteSizeFromIndex) {
        this.indexColor = indexColor;
        this.indexBrand = indexBrand;
        this.indexCategory = indexCategory;
        this.indexSize = indexSize;
        this.updateColorInProducts = updateColorInProducts;
        this.updateBrandInProducts = updateBrandInProducts;
        this.updateCategoryInProducts = updateCategoryInProducts;
        this.updateSizeInProducts = updateSizeInProducts;
        this.deleteBrandFromIndex = deleteBrandFromIndex;
        this.deleteCategoryFromIndex = deleteCategoryFromIndex;
        this.deleteColorFromIndex = deleteColorFromIndex;
        this.deleteSizeFromIndex = deleteSizeFromIndex;
    }

    @ApplicationModuleListener
    public void on(ColorCreated event) {
        indexColor.execute(event.id(), event.description());
    }

    @ApplicationModuleListener
    public void on(BrandCreated event) {
        indexBrand.execute(event.id(), event.description());
    }

    @ApplicationModuleListener
    public void on(CategoryCreated event) {
        indexCategory.execute(event.id(), event.description());
    }

    @ApplicationModuleListener
    public void on(SizeCreated event) {
        indexSize.execute(event.id(), event.description());
    }

    @ApplicationModuleListener
    public void on(ColorRenamed event) {
        indexColor.execute(event.colorId(), event.newDescription());
        updateColorInProducts.execute(event.colorId(), event.newDescription());
    }

    @ApplicationModuleListener
    public void on(BrandRenamed event) {
        indexBrand.execute(event.brandId(), event.newDescription());
        updateBrandInProducts.execute(event.brandId(), event.newDescription());
    }

    @ApplicationModuleListener
    public void on(CategoryRenamed event) {
        indexCategory.execute(event.categoryId(), event.newDescription());
        updateCategoryInProducts.execute(event.categoryId(), event.newDescription());
    }

    @ApplicationModuleListener
    public void on(SizeRenamed event) {
        indexSize.execute(event.sizeId(), event.newDescription());
        updateSizeInProducts.execute(event.sizeId(), event.newDescription());
    }

    @ApplicationModuleListener
    public void on(BrandDeleted event) {
        deleteBrandFromIndex.execute(event.id());
    }

    @ApplicationModuleListener
    public void on(CategoryDeleted event) {
        deleteCategoryFromIndex.execute(event.id());
    }

    @ApplicationModuleListener
    public void on(ColorDeleted event) {
        deleteColorFromIndex.execute(event.id());
    }

    @ApplicationModuleListener
    public void on(SizeDeleted event) {
        deleteSizeFromIndex.execute(event.id());
    }
}
