package com.ai.tutor.admin.mapper;

import com.ai.tutor.admin.model.dto.user.BaseUserInfo;
import com.ai.tutor.admin.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE open_id = #{openId} LIMIT 1")
    User selectByOpenId(@Param("openId") String openId);

    @Select("SELECT * FROM user WHERE phone = #{phone} LIMIT 1")
    User selectByPhone(String phone);

    @Select("SELECT * FROM user WHERE id = #{id} LIMIT 1")
    User selectById(@Param("id") Long id);

    int insert(User user);

    User selectByPhoneAndUserType(@Param("phone") String phone, @Param("userType") Integer userType);

    int updateUserBaseInfo(@Param("userInfo") BaseUserInfo userInfo, @Param("id") Long id);

    @Update("UPDATE user SET user_type = #{userType}, update_time = now() WHERE id = #{id}")
    int updateUserType(@Param("id") Long id, @Param("userType") Integer userType);

    @Update("UPDATE user SET ref_id = #{refId}, update_time = now() WHERE id = #{id}")
    int updateRefId(@Param("id") Long id, @Param("refId") Long refId);

    @Update("UPDATE user SET phone = #{newPhone} WHERE id = #{id}")
    int updateUserPhone(@Param("newPhone") String newPhone, @Param("id") Long id);

    List<User> selectByIds(@Param("ids") List<Long> ids);
}
