package cn.lige2333.spring.service;

import cn.lige2333.spring.annotation.Component;
import cn.lige2333.spring.entity.Order;

@Component
public class PayService {
    public void payOrder(Order order) {
        order.setPaid(true);
    }
}
