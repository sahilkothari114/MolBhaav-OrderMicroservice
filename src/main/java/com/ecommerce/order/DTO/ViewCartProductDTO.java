package com.ecommerce.order.DTO;

import java.util.Collection;

public class ViewCartProductDTO {
    private String productId;
    private String merchantId;
    private String productName;
    private String categoryId;
    private String productImageUrl;
    private String productUsp;
    private String productDescription;
    private double Price;
    private int availableQuantity;
    private int quantity;

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public ViewCartProductDTO() {
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getProductImageUrl() {
        return productImageUrl;
    }

    public void setProductImageUrl(String productImageUrl) {
        this.productImageUrl = productImageUrl;
    }

    public String getProductUsp() {
        return productUsp;
    }

    public void setProductUsp(String productUsp) {
        this.productUsp = productUsp;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public double getPrice() {
        return Price;
    }

    public void setPrice(double price) {
        Price = price;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    @Override
    public String toString() {
        return "ViewCartProductDTO{" +
                "productId='" + productId + '\'' +
                ", merchantId='" + merchantId + '\'' +
                ", productName='" + productName + '\'' +
                ", categoryId='" + categoryId + '\'' +
                ", productImageUrl='" + productImageUrl + '\'' +
                ", productUsp='" + productUsp + '\'' +
                ", productDescription='" + productDescription + '\'' +
                ", Price=" + Price +
                ", availableQuantity=" + availableQuantity +
                '}';
    }
}