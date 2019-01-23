package com.ecommerce.order.service;

import com.ecommerce.order.document.Cart;

import java.util.List;

public interface CartService {
    public Cart save(Cart cart);
    public Cart findOne(long userId);
    public  Cart update(Cart cart);
    public  void delete(long userId);
    public List<Cart> findAll();

}
