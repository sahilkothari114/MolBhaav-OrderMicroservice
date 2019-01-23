package com.ecommerce.order.DTO;

public class MerchantOrders {
    private String merchantId;
    private int ordersMade;

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public int getOrdersMade() {
        return ordersMade;
    }

    public void setOrdersMade(int ordersMade) {
        this.ordersMade = ordersMade;
    }
}
