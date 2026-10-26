package atf.shop.api;

import atf.shop.api.contract.ShopApiQueryParameter;
import atf.shop.api.contract.ShopProductSort;

import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable product filter/sort query used by component and integration tests. */
public record ShopProductQuery(
        String brandId,
        String categoryId,
        Boolean rental,
        String priceRange,
        ShopProductSort sort,
        Integer page) {

    public ShopProductQuery {
        brandId = optional(brandId);
        categoryId = optional(categoryId);
        priceRange = optional(priceRange);
        if (page != null && page < 1) throw new IllegalArgumentException("product page must be positive");
    }

    public static ShopProductQuery defaults() {
        return new ShopProductQuery(null, null, null, null, null, null);
    }

    public static ShopProductQuery sorted(ShopProductSort sort) {
        return new ShopProductQuery(null, null, null, null,
                java.util.Objects.requireNonNull(sort, "sort"), null);
    }

    public static ShopProductQuery category(String categoryId) {
        return new ShopProductQuery(null, categoryId, null, null, null, null);
    }

    /** Wire representation matches the public Practice Shop API contract. */
    public Map<String, String> asQuery() {
        Map<String, String> values = new LinkedHashMap<>();
        if (brandId != null) values.put(ShopApiQueryParameter.BRAND.wireValue(), brandId);
        if (categoryId != null) values.put(ShopApiQueryParameter.CATEGORY.wireValue(), categoryId);
        if (rental != null) values.put(ShopApiQueryParameter.RENTAL.wireValue(), rental.toString());
        if (priceRange != null) values.put(ShopApiQueryParameter.PRICE_RANGE.wireValue(), priceRange);
        if (sort != null) values.put(ShopApiQueryParameter.SORT.wireValue(), sort.wireValue());
        if (page != null) values.put(ShopApiQueryParameter.PAGE.wireValue(), Integer.toString(page));
        return Map.copyOf(values);
    }

    private static String optional(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
