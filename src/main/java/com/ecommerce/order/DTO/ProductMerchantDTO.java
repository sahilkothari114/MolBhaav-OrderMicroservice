package com.ecommerce.order.DTO;

import com.ecommerce.merchant.DTO.MerchantDTO;

public class ProductMerchantDTO {
    private String productMerchantId;
    private com.ecommerce.merchant.DTO.MerchantDTO merchant;
    private String productId;
    private double price;
    private int quantity;
    private int rank;
    private double rating;
    private double ratingCount;

    public double getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(double ratingCount) {
        this.ratingCount = ratingCount;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public ProductMerchantDTO() {
    }

    public String getProductMerchantId() {
        return productMerchantId;
    }

    public void setProductMerchantId(String productMerchantId) {
        this.productMerchantId = productMerchantId;
    }

    public MerchantDTO getMerchant() {
        return merchant;
    }

    public void setMerchant(MerchantDTO merchant) {
        this.merchant = merchant;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "ProductMerchantDTO{" +
                "productMerchantId='" + productMerchantId + '\'' +
                ", merchant=" + merchant +
                ", productId='" + productId + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", rank=" + rank +
                ", rating=" + rating +
                '}';
    }
}
