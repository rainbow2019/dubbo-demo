package com.dubbo.impl;

import com.dubbo.interfaces.User;

public class StudUser implements User {
    @Override
    public String concat(String prefix,String suffix){
        return prefix+"-----Dubbo------"+suffix;
    }
}
