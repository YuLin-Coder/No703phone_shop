package com.phonemarket.entity;
/**
 * 手机内存
 */
import java.io.Serializable;

public class Memory  implements Serializable{
	private Integer memoryId;
	private String memoryName;
	public Integer getMemoryId() {
		return memoryId;
	}
	public void setMemoryId(Integer memoryId) {
		this.memoryId = memoryId;
	}
	public String getMemoryName() {
		return memoryName;
	}
	public void setMemoryName(String memoryName) {
		this.memoryName = memoryName;
	}
}
