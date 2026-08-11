package com.flashshoes.model;

import java.util.List;

/**
 * Standard US shoe sizes the store sells in. This is a store-operator decision,
 * not something the source product-photo dataset contains (no image dataset
 * carries per-product size availability, same reasoning as price/stock in
 * import_dataset.py). Real shoe retailers work exactly like this: a fixed
 * size range per gender, independent of which specific product you're looking at.
 */
public final class SizeCatalog {

    private SizeCatalog() {
        // utility class, never instantiated
    }

    public static final List<String> MEN_SIZES =
            List.of("6", "7", "8", "9", "10", "11", "12", "13");

    public static final List<String> WOMEN_SIZES =
            List.of("4", "5", "6", "7", "8", "9", "10", "11");

    // Union of both ranges, in ascending numeric order, for Unisex-labeled products.
    public static final List<String> UNISEX_SIZES =
            List.of("4", "5", "6", "7", "8", "9", "10", "11", "12", "13");

    public static List<String> sizesFor(String gender) {
        if ("Women".equalsIgnoreCase(gender)) return WOMEN_SIZES;
        if ("Men".equalsIgnoreCase(gender)) return MEN_SIZES;
        return UNISEX_SIZES;
    }
}