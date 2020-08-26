package cn.lige2333.spring.controller;

import cn.lige2333.spring.annotation.Autowired;
import cn.lige2333.spring.annotation.Controller;
import cn.lige2333.spring.annotation.RequestMapping;
import cn.lige2333.spring.annotation.RequestParam;
import cn.lige2333.spring.entity.Order;
import cn.lige2333.spring.service.OrderService;
import com.alibaba.fastjson.JSON;

@Controller
public class OrderController {

    @Autowired
    private OrderService orderService;

    @RequestMapping("/test")
    public String orderControl(@RequestParam("id")String id,@RequestParam("value")String value){
        Order order = orderService.initOrder(id, value);
        return JSON.toJSONString(order);
    }
}
