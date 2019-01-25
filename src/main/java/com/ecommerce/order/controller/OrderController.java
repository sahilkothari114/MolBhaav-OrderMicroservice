package com.ecommerce.order.controller;

import com.ecommerce.order.DTO.*;
import com.ecommerce.order.document.Cart;
import com.ecommerce.order.document.Order;
import com.ecommerce.order.document.OrderProduct;
import com.ecommerce.order.document.Product;
import com.ecommerce.order.service.CartService;
import com.ecommerce.order.service.OrderService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import util.Constant;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;


@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/orders")
public class OrderController {
    @Autowired
    OrderService orderService;

    @Autowired
    CartService cartService;

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CartController.class);

    @RequestMapping(value = "/placeOrder", method = RequestMethod.POST)
    public ResponseEntity<String> placeOrder(@RequestBody Long userId){
        Cart cart = cartService.findOne(userId);
        if (cart==null) {
            return new ResponseEntity<String>("Cart is empty", HttpStatus.NO_CONTENT);
        }
        Order order = cartToOrder(cart);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers=new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity requestEntity=new HttpEntity(order.getProductList(), headers);
        ResponseEntity<?> entityResponse = restTemplate.exchange(Constant.MERCHANT_MICROSERVICE_BASE_URL+"/productMerchants/reduceQuantity", HttpMethod.POST,requestEntity,List.class);
        List<OrderProduct> productList = (List)entityResponse.getBody();
        LOGGER.info("productList"+productList);
        Iterator iterator = productList.iterator();
        ObjectMapper objectMapper = new ObjectMapper();
        List<OrderProduct> productList1 = new ArrayList<>();
        OrderProduct orderProduct  = new OrderProduct();
        while (iterator.hasNext()){
            OrderProduct product = objectMapper.convertValue(iterator.next(),OrderProduct.class);
            productList1.add(product);
        }
        LOGGER.info("productList1"+productList1);

        if(productList1.size()==0){
            return new ResponseEntity<>("Product is out of stock!",HttpStatus.BAD_REQUEST);
        }
        List<OrderProduct> orderProductList = new ArrayList<>();
        requestEntity=new HttpEntity(productList1, headers);
        entityResponse = restTemplate.exchange(Constant.PRODUCT_MICROSERVICE_BASE_URL+"/products/getByList", HttpMethod.POST,requestEntity,List.class);
        productList1= (List)entityResponse.getBody();
        objectMapper = new ObjectMapper();
        LOGGER.info(productList1.toString());
        iterator= productList1.iterator();
        productList = new ArrayList<>();
        while (iterator.hasNext()) {
            OrderProduct product= objectMapper.convertValue(iterator.next(), OrderProduct.class);
            orderProductList.add(product);
        }


        order.setProductList(orderProductList);
        order = orderService.save(order);
        cartService.delete(userId);
        try {
            sendMail(order);
        }catch (Exception e){
            e.printStackTrace();
        }

        return new ResponseEntity<>(order.getOrderId(),HttpStatus.OK);

    }
    @RequestMapping(value = "/getOrderHistory/{userId}", method = RequestMethod.GET)
    public List<Order> getOrderHistory(@PathVariable("userId") long userId){
        return orderService.findByUserId(userId);
    }

    @RequestMapping(value = "/orderHistory/{userId}",method = RequestMethod.GET)
    public ResponseEntity<List<Order>> orderHistory(@PathVariable("userId")Long userId){
        List<Order> orderList = orderService.findByUserId(userId);
        if (orderList.isEmpty()){
            return new ResponseEntity<List<Order>>(orderList, HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<List<Order>>(orderList, HttpStatus.OK);
    }

    @RequestMapping(value = "/ordersMade", method = RequestMethod.POST)
    public ResponseEntity<List<MerchantOrders>> ordersMade(@RequestBody List<MerchantOrders> merchantOrdersList){
        List<MerchantOrders> merchantOrdersList1 = new ArrayList<>();
        for (MerchantOrders merchantOrders:merchantOrdersList) {
            MerchantOrders merchantOrders1 = new MerchantOrders();
           int orderMade= orderService.countByProductList_MerchantId(merchantOrders.getMerchantId());
           merchantOrders1.setOrdersMade(orderMade);
           merchantOrders1.setMerchantId(merchantOrders.getMerchantId());
           merchantOrdersList1.add(merchantOrders1);
        }
        return new ResponseEntity<List<MerchantOrders>>(merchantOrdersList1,HttpStatus.OK);
    }

    private void sendMail(Order order) throws AddressException, MessagingException, IOException {
        RestTemplate restTemplate = new RestTemplate();
        UserDTO userDTO = restTemplate.getForObject(Constant.USER_MICROSERVICE_BASE_URL+"/users/profile/"+order.getUserId(), UserDTO.class);

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("molbhaav@gmail.com", "molbhaav123");
            }
        });
        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress("molbhaav@gmail.com", false));

        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(userDTO.getEmailId()));
        msg.setSubject("Order Placed!");


        msg.setSentDate(new Date());

        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(order,"text/html");


        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);
        //MimeBodyPart attachPart = new MimeBodyPart();

        //attachPart.attachFile("/var/tmp/image19.png");
        //multipart.addBodyPart(attachPart);
        msg.setContent(multipart);
        Transport.send(msg);
    }
    private Order cartToOrder(Cart cart){
        Order order = new Order();
        List<Product> productList = cart.getProductList();
        List<OrderProduct> orderProductList = new ArrayList<>();
        OrderProduct orderProduct = new OrderProduct();
        for (Product product: productList) {
            orderProduct.setProductId(product.getProductId());
            orderProduct.setMerchantId(product.getMerchantId());
            orderProduct.setQuantity(product.getQuantity());
            orderProduct.setPrice(product.getPrice());
            orderProductList.add(orderProduct);
        }

        order.setProductList(orderProductList);
        order.setUserId(cart.getUserId());
        order.setPlacedOn(LocalDateTime.now());
        return order;
    }
}
