package com.ecommerce.order.service;

import com.ecommerce.order.document.Order;
import com.ecommerce.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService{
    @Autowired
    OrderRepository orderRepository;

    @Override
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    @Override
    public Order findOne(String orderId) {
        return orderRepository.findOne(orderId);
    }

    @Override
    public Order update(Order order) {
        return orderRepository.save(order);
    }

    @Override
    public void delete(String orderId) {
        orderRepository.delete(orderId);
    }

    @Override
    public List<Order> findAll() {
        List<Order> orderList= new ArrayList<>();
        Iterable<Order> orderIterable = orderRepository.findAll();
        orderIterable.forEach(orderList::add);
        return orderList;
    }

    @Override
    public List<Order> findByUserId(long userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    public int countByProductList_MerchantId(String merchantId) {
        return orderRepository.countByProductList_MerchantId(merchantId);
    }
}
