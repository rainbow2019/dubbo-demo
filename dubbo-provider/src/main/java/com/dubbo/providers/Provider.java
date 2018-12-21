package com.dubbo.providers;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Provider {
    public static void main(String[] args) throws Exception{
        ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext(new String[]{"spring/spring-dubbo.xml"});
        context.start();
        System.in.read();//暂停这里
    }
}
