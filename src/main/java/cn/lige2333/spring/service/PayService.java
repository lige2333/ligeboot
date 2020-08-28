package cn.lige2333.spring.service;

import cn.lige2333.spring.annotation.Component;
import cn.lige2333.spring.annotation.Value;
import cn.lige2333.spring.entity.Order;

@Component
public class PayService {
    @Value("pay.value")
    private String paid;

    public void payOrder(Order order) {
        order.setPaid(paid);

    }
}
