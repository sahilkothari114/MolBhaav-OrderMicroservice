package com.ecommerce.order.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
@Document
public class Cart {
    @Id
    private long userId;
    private List<Product> productList = new ArrayList<>();

    public Cart() {
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public List<Product> getProductList() {
        return productList;
    }

    public void setProductList(List<Product> productList) {
        this.productList = productList;
    }

    @Override
    public String toString() {
        return "Cart{" +
                "userId=" + userId +
                ", productList=" + productList +
                '}';
    }
}
