package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE phone = #{phone} LIMIT 1")
    User selectByPhone(String phone);

    @Insert("INSERT INTO user (name, phone, create_time, update_time) VALUES (#{name}, #{phone}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    /**
     * 根据手机号与用户类型查询用户
     * @param phone 手机号
     * @param userType 用户类型（1教师 2家长）
     * @return 用户对象
     */
    User selectByPhoneAndUserType(@Param("phone") String phone,
                                  @Param("userType") Integer userType);
}
