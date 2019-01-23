package com.ecommerce.order.repository;

import com.ecommerce.order.document.Order;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends CrudRepository<Order,String> {
    public List<Order> findByUserId(long userId);
    public int countByProductList_MerchantId(String merchantId);
}
