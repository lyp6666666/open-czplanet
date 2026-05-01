package com.ai.tutor.admin.mapper;

import com.ai.tutor.admin.model.entity.CustomerServiceConfig;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AdminCustomerServiceConfigMapper {

    @Select("""
            SELECT id,
                   enabled,
                   channel_type AS channelType,
                   display_name AS displayName,
                   wechat_no AS wechatNo,
                   qq_no AS qqNo,
                   qr_code_object_key AS qrCodeObjectKey,
                   service_time AS serviceTime,
                   description,
                   update_admin_id AS updateAdminId,
                   create_time AS createTime,
                   update_time AS updateTime
            FROM customer_service_config
            WHERE id = 1
            LIMIT 1
            """)
    CustomerServiceConfig selectSingleton();

    @Insert("""
            INSERT INTO customer_service_config (
              id, enabled, channel_type, display_name, wechat_no, qq_no, qr_code_object_key,
              service_time, description, update_admin_id, create_time, update_time
            ) VALUES (
              1, #{enabled}, #{channelType}, #{displayName}, #{wechatNo}, #{qqNo}, #{qrCodeObjectKey},
              #{serviceTime}, #{description}, #{updateAdminId}, NOW(3), NOW(3)
            )
            """)
    int insertSingleton(CustomerServiceConfig config);

    @Update("""
            UPDATE customer_service_config
            SET enabled = #{enabled},
                channel_type = #{channelType},
                display_name = #{displayName},
                wechat_no = #{wechatNo},
                qq_no = #{qqNo},
                qr_code_object_key = #{qrCodeObjectKey},
                service_time = #{serviceTime},
                description = #{description},
                update_admin_id = #{updateAdminId},
                update_time = NOW(3)
            WHERE id = 1
            """)
    int updateSingleton(CustomerServiceConfig config);
}
