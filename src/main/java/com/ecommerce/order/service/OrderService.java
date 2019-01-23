package com.ecommerce.order.service;

import com.ecommerce.order.document.Order;

import java.util.List;
public interface OrderService {    Order save(Order order);
    Order findOne(String orderId);
    Order update(Order order);
    void delete(String orderId);
    List<Order> findAll();
    List<Order> findByUserId(long userId);
    int countByProductList_MerchantId(String merchantId);

}
