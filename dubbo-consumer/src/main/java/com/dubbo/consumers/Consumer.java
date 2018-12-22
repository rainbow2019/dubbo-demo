package com.dubbo.consumers;

import com.alibaba.dubbo.rpc.cluster.LoadBalance;
import com.alibaba.dubbo.rpc.cluster.loadbalance.RandomLoadBalance;
import com.dubbo.interfaces.User;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Consumer {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext(new String[]{"spring/spring-dubbo-consumer.xml"});
        context.start();
        User user = (User) context.getBean("userService");
        System.out.println("========================================================================================" +
                "===============================================================================================");
        String result = user.concat("Hello,", "Today");
        System.out.println(result);
        System.out.println("========================================================================================" +
                "===============================================================================================");
        System.exit(1);
//        LoadBalance
    }
}
