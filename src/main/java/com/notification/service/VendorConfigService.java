package com.notification.service;

import com.notification.entity.VendorConfig;
import com.notification.mapper.VendorConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 供应商配置服务
 * 
 * @author Notification System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VendorConfigService {
    
    private final VendorConfigMapper vendorConfigMapper;
    
    /**
     * 根据供应商编码获取配置
     * 
     * @param vendorCode 供应商编码
     * @return 供应商配置，如果不存在或未启用返回null
     */
    public VendorConfig getVendorConfig(String vendorCode) {
        VendorConfig config = vendorConfigMapper.selectByVendorCodeAndEnabled(vendorCode);
        if (config == null) {
            log.warn("Vendor config not found or disabled: {}", vendorCode);
        }
        return config;
    }
    
    /**
     * 获取所有启用的供应商配置
     * 
     * @return 启用的供应商配置列表
     */
    public List<VendorConfig> getAllEnabledConfigs() {
        return vendorConfigMapper.selectAllEnabled();
    }
}
