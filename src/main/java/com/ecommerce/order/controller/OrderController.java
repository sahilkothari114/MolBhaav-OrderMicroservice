package com.ecommerce.order.controller;

import com.ecommerce.order.DTO.*;
import com.ecommerce.order.document.Cart;
import com.ecommerce.order.document.Order;
import com.ecommerce.order.document.Product;
import com.ecommerce.order.service.CartService;
import com.ecommerce.order.service.OrderService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
    public ResponseEntity<String> placeOrder( Long userId){
        System.out.println(cartService.findOne(userId));
        Order order = cartToOrder(cartService.findOne(userId));
        Order order1 = orderService.save(order);
        if (order1!=null){
            cartService.delete(userId);
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.put(Constant.MERCHANT_MICROSERVICE_BASE_URL+"/productMerchants/reduceQuantity",order1.getProductList());

            try {
                sendmail(userId,order.getOrderId());
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return new ResponseEntity<>(order1.getOrderId(),HttpStatus.ACCEPTED);
    }

    @RequestMapping(value = "/getOrderHistory/{userId}", method = RequestMethod.GET)
    public List<Order> getOrderHistory(@PathVariable("userId") long userId){
        return orderService.findByUserId(userId);
    }

    @RequestMapping(value = "/orderHistory/{userId}",method = RequestMethod.GET)
    public ResponseEntity<List<OrderHistoryDTO>> orderHistory(@PathVariable("userId")Long userId){
        List<Order> orderList = orderService.findByUserId(userId);
        List<OrderHistoryDTO> orderHistoryDTOS=new ArrayList<>();
        for (Order order:orderList) {
            OrderHistoryDTO orderHistoryDTO = new OrderHistoryDTO();
            orderHistoryDTO.setOrderId(order.getOrderId());
            orderHistoryDTO.setPlacedOn(order.getPlacedOn());
            orderHistoryDTO.setUserId(order.getUserId());
            List<ViewCartProductDTO> viewCartProductDTOS = new ArrayList<>();



            if (order == null){
                return new ResponseEntity<List<OrderHistoryDTO>>(orderHistoryDTOS,HttpStatus.NO_CONTENT);

            }
            List<Product> productList = order.getProductList();
            LOGGER.info("productList"+productList);
            List<ProductMerchantDTO> productMerchantDTOList = new ArrayList<>();
            ViewCartProductDTO viewCartProductDTO = new ViewCartProductDTO();

            for (Product product:productList) {
                ProductMerchantDTO productMerchantDTO = new ProductMerchantDTO();
                com.ecommerce.merchant.DTO.MerchantDTO merchantDTO = new com.ecommerce.merchant.DTO.MerchantDTO();
                merchantDTO.setMerchantId(product.getMerchantId());
                productMerchantDTO.setMerchant(merchantDTO);
                productMerchantDTO.setProductId(product.getProductId());
                productMerchantDTOList.add(productMerchantDTO);
                viewCartProductDTO.setQuantity(product.getQuantity());

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
            LOGGER.info(viewCartProductDTOS1.toString());
            iterator= viewCartProductDTOS1.iterator();
            viewCartProductDTOS = new ArrayList<>();
            while (iterator.hasNext()) {
                ViewCartProductDTO viewCartProductDTO1= objectMapper.convertValue(iterator.next(), ViewCartProductDTO.class);

                viewCartProductDTOS.add(viewCartProductDTO1);
            }

            orderHistoryDTO.setProductList(viewCartProductDTOS);
            LOGGER.info(orderHistoryDTO.toString());
            orderHistoryDTOS.add(orderHistoryDTO);
        }
        return new ResponseEntity<List<OrderHistoryDTO>>(orderHistoryDTOS,HttpStatus.OK);

    }

    @RequestMapping(value = "/odersMade", method = RequestMethod.POST)
    public ResponseEntity<List<MerchantOrders>> odersMade(@RequestBody List<MerchantOrders> merchantOrdersList){
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

    private void sendmail(long userId, String orderId) throws AddressException, MessagingException, IOException {
        RestTemplate restTemplate = new RestTemplate();
        UserDTO userDTO = restTemplate.getForObject(Constant.USER_MICROSERVICE_BASE_URL+"/users/profile/"+userId, UserDTO.class);

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

        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse("sahilkothari114@gmail.com"));
        msg.setSubject("Order Placed Successfully!");


        msg.setSentDate(new Date());

        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional //EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\"><head>\n" +
                "    <!--[if gte mso 9]><xml>\n" +
                "     <o:OfficeDocumentSettings>\n" +
                "      <o:AllowPNG/>\n" +
                "      <o:PixelsPerInch>96</o:PixelsPerInch>\n" +
                "     </o:OfficeDocumentSettings>\n" +
                "    </xml><![endif]-->\n" +
                "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width\">\n" +
                "    <!--[if !mso]><!--><meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\"><!--<![endif]-->\n" +
                "    <title></title>\n" +
                "    \n" +
                "    \n" +
                "    <style type=\"text/css\" id=\"media-query\">\n" +
                "      body {\n" +
                "  margin: 0;\n" +
                "  padding: 0; }\n" +
                "\n" +
                "table, tr, td {\n" +
                "  vertical-align: top;\n" +
                "  border-collapse: collapse; }\n" +
                "\n" +
                ".ie-browser table, .mso-container table {\n" +
                "  table-layout: fixed; }\n" +
                "\n" +
                "* {\n" +
                "  line-height: inherit; }\n" +
                "\n" +
                "a[x-apple-data-detectors=true] {\n" +
                "  color: inherit !important;\n" +
                "  text-decoration: none !important; }\n" +
                "\n" +
                "[owa] .img-container div, [owa] .img-container button {\n" +
                "  display: block !important; }\n" +
                "\n" +
                "[owa] .fullwidth button {\n" +
                "  width: 100% !important; }\n" +
                "\n" +
                "[owa] .block-grid .col {\n" +
                "  display: table-cell;\n" +
                "  float: none !important;\n" +
                "  vertical-align: top; }\n" +
                "\n" +
                ".ie-browser .num12, .ie-browser .block-grid, [owa] .num12, [owa] .block-grid {\n" +
                "  width: 600px !important; }\n" +
                "\n" +
                ".ExternalClass, .ExternalClass p, .ExternalClass span, .ExternalClass font, .ExternalClass td, .ExternalClass div {\n" +
                "  line-height: 100%; }\n" +
                "\n" +
                ".ie-browser .mixed-two-up .num4, [owa] .mixed-two-up .num4 {\n" +
                "  width: 200px !important; }\n" +
                "\n" +
                ".ie-browser .mixed-two-up .num8, [owa] .mixed-two-up .num8 {\n" +
                "  width: 400px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.two-up .col, [owa] .block-grid.two-up .col {\n" +
                "  width: 300px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.three-up .col, [owa] .block-grid.three-up .col {\n" +
                "  width: 200px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.four-up .col, [owa] .block-grid.four-up .col {\n" +
                "  width: 150px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.five-up .col, [owa] .block-grid.five-up .col {\n" +
                "  width: 120px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.six-up .col, [owa] .block-grid.six-up .col {\n" +
                "  width: 100px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.seven-up .col, [owa] .block-grid.seven-up .col {\n" +
                "  width: 85px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.eight-up .col, [owa] .block-grid.eight-up .col {\n" +
                "  width: 75px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.nine-up .col, [owa] .block-grid.nine-up .col {\n" +
                "  width: 66px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.ten-up .col, [owa] .block-grid.ten-up .col {\n" +
                "  width: 60px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.eleven-up .col, [owa] .block-grid.eleven-up .col {\n" +
                "  width: 54px !important; }\n" +
                "\n" +
                ".ie-browser .block-grid.twelve-up .col, [owa] .block-grid.twelve-up .col {\n" +
                "  width: 50px !important; }\n" +
                "\n" +
                "@media only screen and (min-width: 620px) {\n" +
                "  .block-grid {\n" +
                "    width: 600px !important; }\n" +
                "  .block-grid .col {\n" +
                "    vertical-align: top; }\n" +
                "    .block-grid .col.num12 {\n" +
                "      width: 600px !important; }\n" +
                "  .block-grid.mixed-two-up .col.num4 {\n" +
                "    width: 200px !important; }\n" +
                "  .block-grid.mixed-two-up .col.num8 {\n" +
                "    width: 400px !important; }\n" +
                "  .block-grid.two-up .col {\n" +
                "    width: 300px !important; }\n" +
                "  .block-grid.three-up .col {\n" +
                "    width: 200px !important; }\n" +
                "  .block-grid.four-up .col {\n" +
                "    width: 150px !important; }\n" +
                "  .block-grid.five-up .col {\n" +
                "    width: 120px !important; }\n" +
                "  .block-grid.six-up .col {\n" +
                "    width: 100px !important; }\n" +
                "  .block-grid.seven-up .col {\n" +
                "    width: 85px !important; }\n" +
                "  .block-grid.eight-up .col {\n" +
                "    width: 75px !important; }\n" +
                "  .block-grid.nine-up .col {\n" +
                "    width: 66px !important; }\n" +
                "  .block-grid.ten-up .col {\n" +
                "    width: 60px !important; }\n" +
                "  .block-grid.eleven-up .col {\n" +
                "    width: 54px !important; }\n" +
                "  .block-grid.twelve-up .col {\n" +
                "    width: 50px !important; } }\n" +
                "\n" +
                "@media (max-width: 620px) {\n" +
                "  .block-grid, .col {\n" +
                "    min-width: 320px !important;\n" +
                "    max-width: 100% !important;\n" +
                "    display: block !important; }\n" +
                "  .block-grid {\n" +
                "    width: calc(100% - 40px) !important; }\n" +
                "  .col {\n" +
                "    width: 100% !important; }\n" +
                "    .col > div {\n" +
                "      margin: 0 auto; }\n" +
                "  img.fullwidth, img.fullwidthOnMobile {\n" +
                "    max-width: 100% !important; }\n" +
                "  .no-stack .col {\n" +
                "    min-width: 0 !important;\n" +
                "    display: table-cell !important; }\n" +
                "  .no-stack.two-up .col {\n" +
                "    width: 50% !important; }\n" +
                "  .no-stack.mixed-two-up .col.num4 {\n" +
                "    width: 33% !important; }\n" +
                "  .no-stack.mixed-two-up .col.num8 {\n" +
                "    width: 66% !important; }\n" +
                "  .no-stack.three-up .col.num4 {\n" +
                "    width: 33% !important; }\n" +
                "  .no-stack.four-up .col.num3 {\n" +
                "    width: 25% !important; }\n" +
                "  .mobile_hide {\n" +
                "    min-height: 0px;\n" +
                "    max-height: 0px;\n" +
                "    max-width: 0px;\n" +
                "    display: none;\n" +
                "    overflow: hidden;\n" +
                "    font-size: 0px; } }\n" +
                "\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body class=\"clean-body\" style=\"margin: 0;padding: 0;-webkit-text-size-adjust: 100%;background-color: #E9E9E9\">\n" +
                "  <style type=\"text/css\" id=\"media-query-bodytag\">\n" +
                "    @media (max-width: 520px) {\n" +
                "      .block-grid {\n" +
                "        min-width: 320px!important;\n" +
                "        max-width: 100%!important;\n" +
                "        width: 100%!important;\n" +
                "        display: block!important;\n" +
                "      }\n" +
                "\n" +
                "      .col {\n" +
                "        min-width: 320px!important;\n" +
                "        max-width: 100%!important;\n" +
                "        width: 100%!important;\n" +
                "        display: block!important;\n" +
                "      }\n" +
                "\n" +
                "        .col > div {\n" +
                "          margin: 0 auto;\n" +
                "        }\n" +
                "\n" +
                "      img.fullwidth {\n" +
                "        max-width: 100%!important;\n" +
                "      }\n" +
                "\t\t\timg.fullwidthOnMobile {\n" +
                "        max-width: 100%!important;\n" +
                "      }\n" +
                "      .no-stack .col {\n" +
                "\t\t\t\tmin-width: 0!important;\n" +
                "\t\t\t\tdisplay: table-cell!important;\n" +
                "\t\t\t}\n" +
                "\t\t\t.no-stack.two-up .col {\n" +
                "\t\t\t\twidth: 50%!important;\n" +
                "\t\t\t}\n" +
                "\t\t\t.no-stack.mixed-two-up .col.num4 {\n" +
                "\t\t\t\twidth: 33%!important;\n" +
                "\t\t\t}\n" +
                "\t\t\t.no-stack.mixed-two-up .col.num8 {\n" +
                "\t\t\t\twidth: 66%!important;\n" +
                "\t\t\t}\n" +
                "\t\t\t.no-stack.three-up .col.num4 {\n" +
                "\t\t\t\twidth: 33%!important;\n" +
                "\t\t\t}\n" +
                "\t\t\t.no-stack.four-up .col.num3 {\n" +
                "\t\t\t\twidth: 25%!important;\n" +
                "\t\t\t}\n" +
                "      .mobile_hide {\n" +
                "        min-height: 0px!important;\n" +
                "        max-height: 0px!important;\n" +
                "        max-width: 0px!important;\n" +
                "        display: none!important;\n" +
                "        overflow: hidden!important;\n" +
                "        font-size: 0px!important;\n" +
                "      }\n" +
                "    }\n" +
                "  </style>\n" +
                "  <!--[if IE]><div class=\"ie-browser\"><![endif]-->\n" +
                "  <!--[if mso]><div class=\"mso-container\"><![endif]-->\n" +
                "  <table class=\"nl-container\" style=\"border-collapse: collapse;table-layout: fixed;border-spacing: 0;mso-table-lspace: 0pt;mso-table-rspace: 0pt;vertical-align: top;min-width: 320px;Margin: 0 auto;background-color: #E9E9E9;width: 100%\" cellpadding=\"0\" cellspacing=\"0\">\n" +
                "\t<tbody>\n" +
                "\t<tr style=\"vertical-align: top\">\n" +
                "\t\t<td style=\"word-break: break-word;border-collapse: collapse !important;vertical-align: top\">\n" +
                "    <!--[if (mso)|(IE)]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td align=\"center\" style=\"background-color: #E9E9E9;\"><![endif]-->\n" +
                "\n" +
                "    <div style=\"background-color:#E9E9E9;\">\n" +
                "      <div style=\"Margin: 0 auto;min-width: 320px;max-width: 600px;overflow-wrap: break-word;word-wrap: break-word;word-break: break-word;background-color: #E9E9E9;\" class=\"block-grid \">\n" +
                "        <div style=\"border-collapse: collapse;display: table;width: 100%;background-color:#E9E9E9;\">\n" +
                "          <!--[if (mso)|(IE)]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td style=\"background-color:#E9E9E9;\" align=\"center\"><table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width: 600px;\"><tr class=\"layout-full-width\" style=\"background-color:#E9E9E9;\"><![endif]-->\n" +
                "\n" +
                "              <!--[if (mso)|(IE)]><td align=\"center\" width=\"600\" style=\" width:600px; padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;\" valign=\"top\"><![endif]-->\n" +
                "            <div class=\"col num12\" style=\"min-width: 320px;max-width: 600px;display: table-cell;vertical-align: top;\">\n" +
                "              <div style=\"background-color: transparent; width: 100% !important;\">\n" +
                "              <!--[if (!mso)&(!IE)]><!--><div style=\"border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent; padding-top:0px; padding-bottom:0px; padding-right: 0px; padding-left: 0px;\"><!--<![endif]-->\n" +
                "\n" +
                "                  \n" +
                "                    <div align=\"center\" class=\"img-container center  autowidth  \" style=\"padding-right: 20px;  padding-left: 20px;\">\n" +
                "<!--[if mso]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr style=\"line-height:0px;line-height:0px;\"><td style=\"padding-right: 20px; padding-left: 20px;\" align=\"center\"><![endif]-->\n" +
                "<div style=\"line-height:20px;font-size:1px\">&#160;</div>  <img class=\"center  autowidth \" align=\"center\" border=\"0\" src=\"https://d1oco4z2z1fhwp.cloudfront.net/templates/default/65/yourlogo_dark.png\" alt=\"Image\" title=\"Image\" style=\"outline: none;text-decoration: none;-ms-interpolation-mode: bicubic;clear: both;display: block !important;border: 0;height: auto;float: none;width: 100%;max-width: 100px\" width=\"100\">\n" +
                "<div style=\"line-height:20px;font-size:1px\">&#160;</div><!--[if mso]></td></tr></table><![endif]-->\n" +
                "</div>\n" +
                "\n" +
                "                  \n" +
                "                  \n" +
                "                    <div align=\"center\" class=\"img-container center  autowidth  fullwidth \" style=\"padding-right: 0px;  padding-left: 0px;\">\n" +
                "<!--[if mso]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr style=\"line-height:0px;line-height:0px;\"><td style=\"padding-right: 0px; padding-left: 0px;\" align=\"center\"><![endif]-->\n" +
                "  <img class=\"center  autowidth  fullwidth\" align=\"center\" border=\"0\" src=\"https://d1oco4z2z1fhwp.cloudfront.net/templates/default/65/Order(2).jpg\" alt=\"Image\" title=\"Image\" style=\"outline: none;text-decoration: none;-ms-interpolation-mode: bicubic;clear: both;display: block !important;border: 0;height: auto;float: none;width: 100%;max-width: 600px\" width=\"600\">\n" +
                "<!--[if mso]></td></tr></table><![endif]-->\n" +
                "</div>\n" +
                "\n" +
                "                  \n" +
                "              <!--[if (!mso)&(!IE)]><!--></div><!--<![endif]-->\n" +
                "              </div>\n" +
                "            </div>\n" +
                "          <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->\n" +
                "        </div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    <div style=\"background-color:#E9E9E9;\">\n" +
                "      <div style=\"Margin: 0 auto;min-width: 320px;max-width: 600px;overflow-wrap: break-word;word-wrap: break-word;word-break: break-word;background-color: #FF5F54;\" class=\"block-grid \">\n" +
                "        <div style=\"border-collapse: collapse;display: table;width: 100%;background-color:#FF5F54;\">\n" +
                "          <!--[if (mso)|(IE)]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td style=\"background-color:#E9E9E9;\" align=\"center\"><table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width: 600px;\"><tr class=\"layout-full-width\" style=\"background-color:#FF5F54;\"><![endif]-->\n" +
                "\n" +
                "              <!--[if (mso)|(IE)]><td align=\"center\" width=\"600\" style=\" width:600px; padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;\" valign=\"top\"><![endif]-->\n" +
                "            <div class=\"col num12\" style=\"min-width: 320px;max-width: 600px;display: table-cell;vertical-align: top;\">\n" +
                "              <div style=\"background-color: transparent; width: 100% !important;\">\n" +
                "              <!--[if (!mso)&(!IE)]><!--><div style=\"border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent; padding-top:0px; padding-bottom:0px; padding-right: 0px; padding-left: 0px;\"><!--<![endif]-->\n" +
                "\n" +
                "                  \n" +
                "                    <div class=\"\">\n" +
                "\t<!--[if mso]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td style=\"padding-right: 20px; padding-left: 20px; padding-top: 20px; padding-bottom: 10px;\"><![endif]-->\n" +
                "\t<div style=\"color:#FFFFFF;font-family:Arial, 'Helvetica Neue', Helvetica, sans-serif;line-height:150%; padding-right: 20px; padding-left: 20px; padding-top: 20px; padding-bottom: 10px;\">\t\n" +
                "\t\t<div style=\"font-size:12px;line-height:18px;color:#FFFFFF;font-family:Arial, 'Helvetica Neue', Helvetica, sans-serif;text-align:left;\"><p style=\"margin: 0;font-size: 14px;line-height: 21px;text-align: center\"><span style=\"font-size: 24px; line-height: 36px;\"><span style=\"line-height: 36px; font-size: 24px;\">Your&#160;order from MolBhaav&#160;</span></span></p><p style=\"margin: 0;font-size: 14px;line-height: 21px;text-align: center\"><span style=\"font-size: 24px; line-height: 36px;\"><span style=\"line-height: 36px; font-size: 24px;\"><strong>#"+orderId+"</strong> has shipped!</span></span></p></div>\t\n" +
                "\t</div>\n" +
                "\t<!--[if mso]></td></tr></table><![endif]-->\n" +
                "</div>\n" +
                "                  \n" +
                "                  \n" +
                "                    \n" +
                "<div align=\"center\" class=\"button-container center \" style=\"padding-right: 20px; padding-left: 20px; padding-top:10px; padding-bottom:25px;\">\n" +
                "  <!--[if mso]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-spacing: 0; border-collapse: collapse; mso-table-lspace:0pt; mso-table-rspace:0pt;\"><tr><td style=\"padding-right: 20px; padding-left: 20px; padding-top:10px; padding-bottom:25px;\" align=\"center\"><v:roundrect xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:w=\"urn:schemas-microsoft-com:office:word\" href=\"\" style=\"height:39pt; v-text-anchor:middle; width:88pt;\" arcsize=\"8%\" strokecolor=\"#FFFFFF\" fillcolor=\"#FFFFFF\"><w:anchorlock/><v:textbox inset=\"0,0,0,0\"><center style=\"color:#27294A; font-family:Arial, 'Helvetica Neue', Helvetica, sans-serif; font-size:16px;\"><![endif]-->\n" +
                "    <div style=\"color: #27294A; background-color: #FFFFFF; border-radius: 4px; -webkit-border-radius: 4px; -moz-border-radius: 4px; max-width: 118px; width: 68px;width: auto; border-top: 0px solid transparent; border-right: 0px solid transparent; border-bottom: 0px solid transparent; border-left: 0px solid transparent; padding-top: 10px; padding-right: 25px; padding-bottom: 10px; padding-left: 25px; font-family: Arial, 'Helvetica Neue', Helvetica, sans-serif; text-align: center; mso-border-alt: none;\">\n" +
                "      <span style=\"font-family:Arial, 'Helvetica Neue', Helvetica, sans-serif;font-size:16px;line-height:32px;\"><strong><span style=\"font-family: arial, helvetica, sans-serif; font-size: 16px; line-height: 32px;\">Shop More</span></strong></span>\n" +
                "    </div>\n" +
                "  <!--[if mso]></center></v:textbox></v:roundrect></td></tr></table><![endif]-->\n" +
                "</div>\n" +
                "\n" +
                "                  \n" +
                "              <!--[if (!mso)&(!IE)]><!--></div><!--<![endif]-->\n" +
                "              </div>\n" +
                "            </div>\n" +
                "          <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->\n" +
                "        </div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    <div style=\"background-color:#E9E9E9;\">\n" +
                "      <div style=\"Margin: 0 auto;min-width: 320px;max-width: 600px;overflow-wrap: break-word;word-wrap: break-word;word-break: break-word;background-color: #FFFFFF;\" class=\"block-grid \">\n" +
                "        <div style=\"border-collapse: collapse;display: table;width: 100%;background-color:#FFFFFF;\">\n" +
                "          <!--[if (mso)|(IE)]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td style=\"background-color:#E9E9E9;\" align=\"center\"><table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width: 600px;\"><tr class=\"layout-full-width\" style=\"background-color:#FFFFFF;\"><![endif]-->\n" +
                "\n" +
                "              <!--[if (mso)|(IE)]><td align=\"center\" width=\"600\" style=\" width:600px; padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;\" valign=\"top\"><![endif]-->\n" +
                "            <div class=\"col num12\" style=\"min-width: 320px;max-width: 600px;display: table-cell;vertical-align: top;\">\n" +
                "              <div style=\"background-color: transparent; width: 100% !important;\">\n" +
                "              <!--[if (!mso)&(!IE)]><!--><div style=\"border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent; padding-top:0px; padding-bottom:0px; padding-right: 0px; padding-left: 0px;\"><!--<![endif]-->\n" +
                "\n" +
                "                  \n" +
                "                    <div align=\"center\" class=\"img-container center  autowidth  \" style=\"padding-right: 20px;  padding-left: 20px;\">\n" +
                "<!--[if mso]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr style=\"line-height:0px;line-height:0px;\"><td style=\"padding-right: 20px; padding-left: 20px;\" align=\"center\"><![endif]-->\n" +
                "<div style=\"line-height:20px;font-size:1px\">&#160;</div>  <img class=\"center  autowidth \" align=\"center\" border=\"0\" src=\"https://d1oco4z2z1fhwp.cloudfront.net/templates/default/65/LS_Spread2.png\" alt=\"Image\" title=\"Image\" style=\"outline: none;text-decoration: none;-ms-interpolation-mode: bicubic;clear: both;display: block !important;border: 0;height: auto;float: none;width: 100%;max-width: 283px\" width=\"283\">\n" +
                "<div style=\"line-height:20px;font-size:1px\">&#160;</div><!--[if mso]></td></tr></table><![endif]-->\n" +
                "</div>\n" +
                "\n" +
                "                  \n" +
                "                  \n" +
                "                    <div class=\"\">\n" +
                "\t<!--[if mso]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td style=\"padding-right: 0px; padding-left: 0px; padding-top: 0px; padding-bottom: 30px;\"><![endif]-->\n" +
                "\t<div style=\"color:#27294A;font-family:Arial, 'Helvetica Neue', Helvetica, sans-serif;line-height:150%; padding-right: 0px; padding-left: 0px; padding-top: 0px; padding-bottom: 30px;\">\t\n" +
                "\t\t<div style=\"font-size:12px;line-height:18px;color:#27294A;font-family:Arial, 'Helvetica Neue', Helvetica, sans-serif;text-align:left;\"><p style=\"margin: 0;font-size: 14px;line-height: 21px;text-align: center\"><span style=\"font-size: 24px; line-height: 36px;\"><span style=\"line-height: 36px; font-size: 24px;\"><span style=\"line-height: 36px; font-size: 24px;\">Get more deals for your next delivery! </span></span></span></p></div>\t\n" +
                "\t</div>\n" +
                "\t<!--[if mso]></td></tr></table><![endif]-->\n" +
                "</div>\n" +
                "                  \n" +
                "              <!--[if (!mso)&(!IE)]><!--></div><!--<![endif]-->\n" +
                "              </div>\n" +
                "            </div>\n" +
                "          <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->\n" +
                "        </div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    <div style=\"background-color:#E9E9E9;\">\n" +
                "      <div style=\"Margin: 0 auto;min-width: 320px;max-width: 600px;overflow-wrap: break-word;word-wrap: break-word;word-break: break-word;background-color: #27294A;\" class=\"block-grid \">\n" +
                "        <div style=\"border-collapse: collapse;display: table;width: 100%;background-color:#27294A;\">\n" +
                "          <!--[if (mso)|(IE)]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td style=\"background-color:#E9E9E9;\" align=\"center\"><table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width: 600px;\"><tr class=\"layout-full-width\" style=\"background-color:#27294A;\"><![endif]-->\n" +
                "\n" +
                "              <!--[if (mso)|(IE)]><td align=\"center\" width=\"600\" style=\" width:600px; padding-right: 15px; padding-left: 15px; padding-top:15px; padding-bottom:15px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;\" valign=\"top\"><![endif]-->\n" +
                "            <div class=\"col num12\" style=\"min-width: 320px;max-width: 600px;display: table-cell;vertical-align: top;\">\n" +
                "              <div style=\"background-color: transparent; width: 100% !important;\">\n" +
                "              <!--[if (!mso)&(!IE)]><!--><div style=\"border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent; padding-top:15px; padding-bottom:15px; padding-right: 15px; padding-left: 15px;\"><!--<![endif]-->\n" +
                "\n" +
                "                  \n" +
                "                    \n" +
                "<div align=\"center\" style=\"padding-right: 0px; padding-left: 0px; padding-bottom: 0px;\" class=\"\">\n" +
                "  <div style=\"display: table; max-width:141px;\">\n" +
                "  <!--[if (mso)|(IE)]><table width=\"141\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td style=\"border-collapse:collapse; padding-right: 0px; padding-left: 0px; padding-bottom: 0px;\"  align=\"center\"><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse; mso-table-lspace: 0pt;mso-table-rspace: 0pt; width:141px;\"><tr><td width=\"32\" style=\"width:32px; padding-right: 10px;\" valign=\"top\"><![endif]-->\n" +
                "    <table align=\"left\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"32\" height=\"32\" style=\"border-collapse: collapse;table-layout: fixed;border-spacing: 0;mso-table-lspace: 0pt;mso-table-rspace: 0pt;vertical-align: top;Margin-right: 10px\">\n" +
                "      <tbody><tr style=\"vertical-align: top\"><td align=\"left\" valign=\"middle\" style=\"word-break: break-word;border-collapse: collapse !important;vertical-align: top\">\n" +
                "        <a href=\"https://www.facebook.com/\" title=\"Facebook\" target=\"_blank\">\n" +
                "          <img src=\"images/facebook.png\" alt=\"Facebook\" title=\"Facebook\" width=\"32\" style=\"outline: none;text-decoration: none;-ms-interpolation-mode: bicubic;clear: both;display: block !important;border: none;height: auto;float: none;max-width: 32px !important\">\n" +
                "        </a>\n" +
                "      <div style=\"line-height:5px;font-size:1px\">&#160;</div>\n" +
                "      </td></tr>\n" +
                "    </tbody></table>\n" +
                "      <!--[if (mso)|(IE)]></td><td width=\"32\" style=\"width:32px; padding-right: 10px;\" valign=\"top\"><![endif]-->\n" +
                "    <table align=\"left\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"32\" height=\"32\" style=\"border-collapse: collapse;table-layout: fixed;border-spacing: 0;mso-table-lspace: 0pt;mso-table-rspace: 0pt;vertical-align: top;Margin-right: 10px\">\n" +
                "      <tbody><tr style=\"vertical-align: top\"><td align=\"left\" valign=\"middle\" style=\"word-break: break-word;border-collapse: collapse !important;vertical-align: top\">\n" +
                "        <a href=\"http://twitter.com/\" title=\"Twitter\" target=\"_blank\">\n" +
                "          <img src=\"images/twitter.png\" alt=\"Twitter\" title=\"Twitter\" width=\"32\" style=\"outline: none;text-decoration: none;-ms-interpolation-mode: bicubic;clear: both;display: block !important;border: none;height: auto;float: none;max-width: 32px !important\">\n" +
                "        </a>\n" +
                "      <div style=\"line-height:5px;font-size:1px\">&#160;</div>\n" +
                "      </td></tr>\n" +
                "    </tbody></table>\n" +
                "      <!--[if (mso)|(IE)]></td><td width=\"32\" style=\"width:32px; padding-right: 0;\" valign=\"top\"><![endif]-->\n" +
                "    <table align=\"left\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"32\" height=\"32\" style=\"border-collapse: collapse;table-layout: fixed;border-spacing: 0;mso-table-lspace: 0pt;mso-table-rspace: 0pt;vertical-align: top;Margin-right: 0\">\n" +
                "      <tbody><tr style=\"vertical-align: top\"><td align=\"left\" valign=\"middle\" style=\"word-break: break-word;border-collapse: collapse !important;vertical-align: top\">\n" +
                "        <a href=\"http://plus.google.com/\" title=\"Google+\" target=\"_blank\">\n" +
                "          <img src=\"images/googleplus.png\" alt=\"Google+\" title=\"Google+\" width=\"32\" style=\"outline: none;text-decoration: none;-ms-interpolation-mode: bicubic;clear: both;display: block !important;border: none;height: auto;float: none;max-width: 32px !important\">\n" +
                "        </a>\n" +
                "      <div style=\"line-height:5px;font-size:1px\">&#160;</div>\n" +
                "      </td></tr>\n" +
                "    </tbody></table>\n" +
                "    <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->\n" +
                "  </div>\n" +
                "</div>\n" +
                "                  \n" +
                "              <!--[if (!mso)&(!IE)]><!--></div><!--<![endif]-->\n" +
                "              </div>\n" +
                "            </div>\n" +
                "          <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->\n" +
                "        </div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    <div style=\"background-color:transparent;\">\n" +
                "      <div style=\"Margin: 0 auto;min-width: 320px;max-width: 600px;overflow-wrap: break-word;word-wrap: break-word;word-break: break-word;background-color: #E9E9E9;\" class=\"block-grid \">\n" +
                "        <div style=\"border-collapse: collapse;display: table;width: 100%;background-color:#E9E9E9;\">\n" +
                "          <!--[if (mso)|(IE)]><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td style=\"background-color:transparent;\" align=\"center\"><table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"width: 600px;\"><tr class=\"layout-full-width\" style=\"background-color:#E9E9E9;\"><![endif]-->\n" +
                "\n" +
                "              <!--[if (mso)|(IE)]><td align=\"center\" width=\"600\" style=\" width:600px; padding-right: 0px; padding-left: 0px; padding-top:0px; padding-bottom:0px; border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent;\" valign=\"top\"><![endif]-->\n" +
                "            <div class=\"col num12\" style=\"min-width: 320px;max-width: 600px;display: table-cell;vertical-align: top;\">\n" +
                "              <div style=\"background-color: transparent; width: 100% !important;\">\n" +
                "              <!--[if (!mso)&(!IE)]><!--><div style=\"border-top: 0px solid transparent; border-left: 0px solid transparent; border-bottom: 0px solid transparent; border-right: 0px solid transparent; padding-top:0px; padding-bottom:0px; padding-right: 0px; padding-left: 0px;\"><!--<![endif]-->\n" +
                "\n" +
                "                  \n" +
                "                    \n" +
                "<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" class=\"divider \" style=\"border-collapse: collapse;table-layout: fixed;border-spacing: 0;mso-table-lspace: 0pt;mso-table-rspace: 0pt;vertical-align: top;min-width: 100%;-ms-text-size-adjust: 100%;-webkit-text-size-adjust: 100%\">\n" +
                "    <tbody>\n" +
                "        <tr style=\"vertical-align: top\">\n" +
                "            <td class=\"divider_inner\" style=\"word-break: break-word;border-collapse: collapse !important;vertical-align: top;padding-right: 15px;padding-left: 15px;padding-top: 15px;padding-bottom: 15px;min-width: 100%;mso-line-height-rule: exactly;-ms-text-size-adjust: 100%;-webkit-text-size-adjust: 100%\">\n" +
                "                <table class=\"divider_content\" align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"border-collapse: collapse;table-layout: fixed;border-spacing: 0;mso-table-lspace: 0pt;mso-table-rspace: 0pt;vertical-align: top;border-top: 0px solid transparent;-ms-text-size-adjust: 100%;-webkit-text-size-adjust: 100%\">\n" +
                "                    <tbody>\n" +
                "                        <tr style=\"vertical-align: top\">\n" +
                "                            <td style=\"word-break: break-word;border-collapse: collapse !important;vertical-align: top;mso-line-height-rule: exactly;-ms-text-size-adjust: 100%;-webkit-text-size-adjust: 100%\">\n" +
                "                                <span></span>\n" +
                "                            </td>\n" +
                "                        </tr>\n" +
                "                    </tbody>\n" +
                "                </table>\n" +
                "            </td>\n" +
                "        </tr>\n" +
                "    </tbody>\n" +
                "</table>\n" +
                "                  \n" +
                "              <!--[if (!mso)&(!IE)]><!--></div><!--<![endif]-->\n" +
                "              </div>\n" +
                "            </div>\n" +
                "          <!--[if (mso)|(IE)]></td></tr></table></td></tr></table><![endif]-->\n" +
                "        </div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "   <!--[if (mso)|(IE)]></td></tr></table><![endif]-->\n" +
                "\t\t</td>\n" +
                "  </tr>\n" +
                "  </tbody>\n" +
                "  </table>\n" +
                "  <!--[if (mso)|(IE)]></div><![endif]-->\n" +
                "\n" +
                "\n" +
                "</body></html>", "text/html");

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
        order.setProductList(cart.getProductList());
        order.setUserId(cart.getUserId());
        order.setPlacedOn(LocalDateTime.now());
        return order;
    }

}
