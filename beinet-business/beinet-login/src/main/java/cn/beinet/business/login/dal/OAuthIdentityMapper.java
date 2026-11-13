package cn.beinet.business.login.dal;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OAuthIdentityMapper {
    // 使用供应商用户标识查本地 ID，邮箱只用于准入和资料更新，不作为身份关联键。
    @Select("SELECT user_id FROM oauth_identities WHERE provider = #{provider} AND subject = #{subject}")
    Long findUserId(@Param("provider") String provider, @Param("subject") String subject);

    // 由数据库复合主键保证唯一性，外键保证本地用户存在；由业务事务保证整体原子性。
    @Insert("INSERT INTO oauth_identities(provider, subject, user_id) VALUES(#{provider}, #{subject}, #{userId})")
    void insert(@Param("provider") String provider, @Param("subject") String subject,
                @Param("userId") Long userId);
}
