/*
 * Copyright 2020-2022 the original author or authors.
 *
 * Licensed under the Affero General Public License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package com.myapp.utils;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * 自定义异常类
 * @author zhangt2333
 */

@Getter
@ToString
@AllArgsConstructor
public class WifiException extends RuntimeException {
    public int code;
    public String message;
    public Throwable exception;

    public WifiException(Throwable e, WifiExceptionEnum wifiExceptionEnum) {
        this.exception = e;
        this.code = wifiExceptionEnum.code;
        this.message = wifiExceptionEnum.message;
    }

    public WifiException(WifiExceptionEnum wifiExceptionEnum) {
        this.code = wifiExceptionEnum.code;
        this.message = wifiExceptionEnum.message;
    }

    public WifiException(WifiExceptionEnum wifiExceptionEnum, String additionalMessage) {
        this.code = wifiExceptionEnum.code;
        this.message = wifiExceptionEnum.message + " " + additionalMessage;
    }
}