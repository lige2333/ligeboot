package cn.lige2333.spring.service;

import cn.lige2333.spring.annotation.Autowired;
import cn.lige2333.spring.annotation.Component;
import cn.lige2333.spring.entity.Order;

@Component
public class OrderService {

    @Autowired
    private PayService payService;

    public Order initOrder(String id, String value) {
        Order order = new Order(id, value);
        payService.payOrder(order);
        return order;
    }
}
