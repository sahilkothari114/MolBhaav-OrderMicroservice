package com.ecommerce.order.controller;

import com.ecommerce.merchant.DTO.MerchantDTO;
import com.ecommerce.order.DTO.AddToCartRequestDTO;
import com.ecommerce.order.DTO.ProductMerchantDTO;
import com.ecommerce.order.DTO.ViewCartProductDTO;
import com.ecommerce.order.document.Cart;
import com.ecommerce.order.document.Product;
import com.ecommerce.order.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import util.Constant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

@RequestMapping("/cart")
@RestController
public class CartController {
    @Autowired
    CartService cartService;
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CartController.class);

    @RequestMapping(value = "/add",method = RequestMethod.POST)
    public void addToCart(@RequestBody AddToCartRequestDTO addToCartRequestDTO){
        Cart cart = new Cart();
        cart = cartService.findOne(addToCartRequestDTO.getUserId());
        Product product = new Product();
        product.setMerchantId(addToCartRequestDTO.getMerchantId());
        product.setProductId(addToCartRequestDTO.getProductId());
        product.setQuantity(addToCartRequestDTO.getQuantity());
        List<Product> productList;
        if (cart==null){
            cart = new Cart();
            productList=new ArrayList<>();
            cart.setUserId(addToCartRequestDTO.getUserId());
        }else {
            productList = cart.getProductList();
        }
        productList.add(product);
        cart.setProductList(productList);
        cartService.save(cart);
    }

    @RequestMapping(value = "/saveCart", method = RequestMethod.POST)
    public ResponseEntity<String> saveCart(@RequestBody Cart cart){
        Cart cartSaved = cartService.save(cart);
        return new ResponseEntity<String>(Long.toString(cartSaved.getUserId()),HttpStatus.CREATED);
    }

    @RequestMapping(value = "/viewCart/{userId}",method = RequestMethod.GET)
    public ResponseEntity<List<ViewCartProductDTO>> viewCart(@PathVariable("userId") Long userId){
        Cart cart = cartService.findOne(userId);
        List<ViewCartProductDTO> viewCartProductDTOS = new ArrayList<>();

        if (cart == null){
            return new ResponseEntity<List<ViewCartProductDTO>>(viewCartProductDTOS,HttpStatus.NO_CONTENT);

        }
        List<Product> productList = cart.getProductList();
        LOGGER.info("productList"+productList);
        List<ProductMerchantDTO> productMerchantDTOList = new ArrayList<>();
        for (Product product:productList) {
            ProductMerchantDTO productMerchantDTO = new ProductMerchantDTO();
            com.ecommerce.merchant.DTO.MerchantDTO merchantDTO = new MerchantDTO();
            merchantDTO.setMerchantId(product.getMerchantId());
            productMerchantDTO.setMerchant(merchantDTO);
            productMerchantDTO.setProductId(product.getProductId());
            productMerchantDTOList.add(productMerchantDTO);
        }
        LOGGER.info("productMerchantDTOList:"+productMerchantDTOList);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers=new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity requestEntity=new HttpEntity(productMerchantDTOList, headers);
        ResponseEntity<?> entityResponse = restTemplate.exchange(Constant.MERCHANT_MICROSERVICE_BASE_URL+"/productMerchants/getByProductMerchantList", HttpMethod.POST,requestEntity,List.class);
        List<ProductMerchantDTO> productMerchantDTOList1 = (List)entityResponse.getBody();
        LOGGER.info("productMerchantDTOList1:"+productMerchantDTOList1);
        ObjectMapper objectMapper = new ObjectMapper();
        Iterator iterator= productMerchantDTOList1.iterator();
        viewCartProductDTOS = new ArrayList<>();
        while (iterator.hasNext()) {
            ProductMerchantDTO productMerchantDTO= objectMapper.convertValue(iterator.next(), ProductMerchantDTO.class);
            ViewCartProductDTO viewCartProductDTO = new ViewCartProductDTO();
            viewCartProductDTO.setProductId(productMerchantDTO.getProductId());
            viewCartProductDTO.setMerchantId(productMerchantDTO.getMerchant().getMerchantId());
            viewCartProductDTO.setAvailableQuantity(productMerchantDTO.getQuantity());
            viewCartProductDTO.setPrice(productMerchantDTO.getPrice());
            viewCartProductDTOS.add(viewCartProductDTO);
        }
        LOGGER.info(viewCartProductDTOS.toString());

        requestEntity=new HttpEntity(viewCartProductDTOS, headers);
        entityResponse = restTemplate.exchange(Constant.PRODUCT_MICROSERVICE_BASE_URL+"/products/getByList", HttpMethod.POST,requestEntity,List.class);
        List<ViewCartProductDTO> viewCartProductDTOS1= (List)entityResponse.getBody();
        objectMapper = new ObjectMapper();
        iterator= viewCartProductDTOS1.iterator();
        viewCartProductDTOS = new ArrayList<>();
        while (iterator.hasNext()) {
            ViewCartProductDTO viewCartProductDTO1= objectMapper.convertValue(iterator.next(), ViewCartProductDTO.class);

            viewCartProductDTOS.add(viewCartProductDTO1);
        }




        return new ResponseEntity<List<ViewCartProductDTO>>(viewCartProductDTOS,HttpStatus.OK);
    }

}
