package com.ecommerce.order.service;

import com.ecommerce.order.document.Cart;
import com.ecommerce.order.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    CartRepository cartRepository;

    @Override
    public Cart save(Cart cart) {
        return cartRepository.save(cart);
    }

    @Override
    public Cart findOne(long userId) {
        return cartRepository.findOne(userId);
    }

    @Override
    public Cart update(Cart cart) {
        return cartRepository.save(cart);
    }

    @Override
    public void delete(long userId) {
        cartRepository.delete(userId);
    }

    @Override
    public List<Cart> findAll() {
        List<Cart> cartList= new ArrayList<>();
        Iterable<Cart> cartIterable = cartRepository.findAll();
        cartIterable.forEach(cartList::add);
        return cartList;
    }


}
