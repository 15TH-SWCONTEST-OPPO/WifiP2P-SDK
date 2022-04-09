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

/**
 * API异常枚举类
 * @author zhangt2333
 */

@Getter
@AllArgsConstructor
public enum WifiExceptionEnum {
    PARAMETER_ERROR(400, "请求参数错误"),

    UNKNOWN_ERROR(500, "服务器出错"),
    INTERNAL_ERROR(500, "内部服务出错"),
    SERVER_BUSY(500, "服务器正忙，请重试"),
    CONNECT_ERROR(400,"连接失败");

    public int code;
    public String message;

    WifiExceptionEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}