package com.ai.tutor.appointment.mapper;

import com.ai.tutor.appointment.model.dto.user.BaseUserInfo;
import com.ai.tutor.appointment.model.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE phone = #{phone} LIMIT 1")
    User selectByPhone(String phone);

    @Select("SELECT * FROM user WHERE id = #{id} LIMIT 1")
    User selectById(@Param("id") Long id);

    int insert(User user);

    /**
     * 根据手机号与用户类型查询用户
     * @param phone 手机号
     * @param userType 用户类型（1教师 2家长）
     * @return 用户对象
     */
    User selectByPhoneAndUserType(@Param("phone") String phone,
                                  @Param("userType") Integer userType);

    int updateUserBaseInfo(@Param("userInfo") BaseUserInfo userInfo,@Param("id") Long id);

    @Update("UPDATE user SET user_type = #{userType}, update_time = now() WHERE id = #{id}")
    int updateUserType(@Param("id") Long id, @Param("userType") Integer userType);

    @Update("UPDATE user SET password = #{newPassword} WHERE id = #{id}")
    int updateUserPassWord(String newPassword, Long id);

    @Update("UPDATE user SET phone = #{newPhone} WHERE id = #{id}")
    int updateUserPhone(@Param("newPhone") String newPhone, @Param("id") Long id);

    List<User> selectByIds(@Param("ids") List<Long> ids);
}
