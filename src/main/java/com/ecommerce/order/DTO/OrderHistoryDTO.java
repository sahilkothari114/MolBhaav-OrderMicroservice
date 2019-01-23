package com.ecommerce.order.DTO;

import com.ecommerce.order.document.Product;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document
public class OrderHistoryDTO {
    @Id
    private String orderId;
    private long userId;
    private LocalDateTime placedOn;
    private List<ViewCartProductDTO> productList = new ArrayList<>();

    public OrderHistoryDTO() {
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public LocalDateTime getPlacedOn() {
        return placedOn;
    }

    public void setPlacedOn(LocalDateTime placedOn) {
        this.placedOn = placedOn;
    }

    public List<ViewCartProductDTO> getProductList() {
        return productList;
    }

    public void setProductList(List<ViewCartProductDTO> productList) {
        this.productList = productList;
    }

    @Override
    public String toString() {
        return "OrderHistoryDTO{" +
                "orderId='" + orderId + '\'' +
                ", userId=" + userId +
                ", placedOn=" + placedOn +
                ", productList=" + productList +
                '}';
    }
}
