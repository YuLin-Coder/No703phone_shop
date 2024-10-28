package com.phonemarket.mapper;
/**
 * 与手机内存有关的mapper接口
 */
import java.util.List;

import org.springframework.stereotype.Repository;

import com.phonemarket.entity.Memory;

@Repository
public interface MemoryMapper {
	Memory findMemoryById(Integer id);
	
	/*查询所有手机内存类型*/
	List<Memory> findAllMemory();
}
