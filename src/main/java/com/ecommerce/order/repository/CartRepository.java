package com.ecommerce.order.repository;

import com.ecommerce.order.document.Cart;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartRepository extends CrudRepository<Cart,Long> { }
