package com.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notification.entity.VendorConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 供应商配置 Mapper 接口
 * 
 * @author Notification System
 */
@Mapper
public interface VendorConfigMapper extends BaseMapper<VendorConfig> {
    
    /**
     * 根据供应商编码查询启用的配置
     * 
     * @param vendorCode 供应商编码
     * @return 供应商配置，如果不存在或未启用返回null
     */
    @Select("SELECT * FROM vendor_config " +
            "WHERE vendor_code = #{vendorCode} " +
            "AND enabled = 1 " +
            "LIMIT 1")
    VendorConfig selectByVendorCodeAndEnabled(@Param("vendorCode") String vendorCode);
    
    /**
     * 查询所有启用的供应商配置
     * 
     * @return 启用的供应商配置列表
     */
    @Select("SELECT * FROM vendor_config WHERE enabled = 1")
    List<VendorConfig> selectAllEnabled();
}
