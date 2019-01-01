package com.dubbo.registry;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class Test12 {

    public static void main(String[] args) {
        List<String> list = new ArrayList<String>();
        list.add("AAAAA");
        list.add("BBBBB");
        list.add("CCCCC");
        List<Long> list2 = new ArrayList<Long>();
        list2.add(10000L);
        list2.add(14444L);
        list2.add(15555L);
        // 利用StringUtils.join实现
        String result11 = StringUtils.join(list,';');
        System.out.println("result11 = " + result11);
        String result12 = StringUtils.join(list2 ,';');//这里的List<?>,不限制元素类型
        System.out.println("result12 = " + result12);
        // 利用String.join方法
        String result21 = String.join(";",list);
        System.out.println("result21 = " + result21);

        //利用Stream
        StringBuilder sb = new StringBuilder();
        list2.forEach(item ->{
            sb.append(Long.toString(item)).append(";");
        });
        String result31 = sb.substring(0,sb.length()-1);
        System.out.println("result31 = " + result31);
    }


    @Test
    public void test22(){
        ArrayList<Long> list2 = new ArrayList<Long>();
        list2.add(1L);
        list2.add(2L);
        ArrayList<String> list = new ArrayList<String>();
        list.add("sss");
        list.add("bbb");
        System.out.println("list = " + list.toString());
//        System.out.println("Collections. = " + Collections.);


        StringUtils.join(list,";");

        String.join(";",list);
//        String.join(";",list2);


//        StringBuilder sb = new StringBuilder();
//        list2.forEach(item ->{
//            sb.append(item.toString()).append(";");
//        });
        StringBuilder sb = new StringBuilder();
        list2.forEach(item ->{
            sb.append(Long.toString(item)).append(";");
        });
        String result = sb.substring(0,sb.length()-1);
        System.out.println("result = " + result);
    }
}
