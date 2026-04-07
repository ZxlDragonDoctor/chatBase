package com.zxl.chatbase.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonitorException extends RuntimeException {
	private Integer code;
	private Integer type;//系统内部异常类型
	private String msg;//异常信息

	public static MonitorException build(Integer code, String msg) {
		return MonitorException.builder().code(code).msg(msg).build();
	}

	public static MonitorException build(String msg) {
		return MonitorException.builder().code(ResultCode.ERROR.getCode()).msg(msg).build();
	}
}
