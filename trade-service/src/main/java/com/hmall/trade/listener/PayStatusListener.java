package com.hmall.trade.listener;


import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayStatusListener {


    private final IOrderService orderService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "trade.pay.success.queue", durable = "true"),
            exchange = @Exchange("pay.direct"),
            key = "pay.success"
    ))
    public void listenPaySuccess(Long orderId){
        //获取订单
        Order order = orderService.getById(orderId);
        //判断订单是否为未支付
        if (order == null || order.getStatus() != 1){
            return;
        }



        orderService.markOrderPaySuccess(orderId);
    }
}
