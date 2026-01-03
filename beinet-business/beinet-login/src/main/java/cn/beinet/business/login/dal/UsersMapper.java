package cn.beinet.business.login.dal;

import cn.beinet.business.login.dal.entity.Users;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户 Mapper 接口
 * 
 * @author youbl.blog.csdn.net
 * @since 2024-11-19 12:28:00
 */
@Mapper
public interface UsersMapper extends BaseMapper<Users> {
    String TABLE = "users";

    @Select("<script>" +
            "SELECT * FROM " + TABLE + " a " +
            "WHERE a.id IN " +
            "<foreach item='item' index='index' collection='idList' open='(' separator=',' close=')'>" +
            " #{item} " +
            "</foreach> " +
            "ORDER BY a.id DESC" +
            "</script>")
    List<Users> getListByIds(List<Long> idList);

    /**
     * 根据 GitHub ID 查询用户
     * 
     * @param githubId GitHub 用户ID
     * @return 用户信息
     */
    @Select("SELECT * FROM " + TABLE + " WHERE github_id = #{githubId} AND delflag = 0")
    Users selectByGithubId(@Param("githubId") Long githubId);

    /**
     * 根据 GitHub 用户名查询用户
     * 
     * @param githubLogin GitHub 用户名
     * @return 用户信息
     */
    @Select("SELECT * FROM " + TABLE + " WHERE github_login = #{githubLogin} AND delflag = 0")
    Users selectByGithubLogin(@Param("githubLogin") String githubLogin);

    /**
     * 更新用户最后登录信息
     * 
     * @param userId 用户ID
     * @param loginIp 登录IP
     * @param loginTime 登录时间
     * @return 更新行数
     */
    @Update("UPDATE " + TABLE + " SET last_login_ip = #{loginIp}, lastLoginDate = #{loginTime}, " +
            "login_count = login_count + 1 WHERE id = #{userId}")
    int updateLastLoginInfo(@Param("userId") Long userId, 
                           @Param("loginIp") String loginIp, 
                           @Param("loginTime") LocalDateTime loginTime);

    /**
     * 根据登录类型统计用户数量
     * 
     * @return 统计结果
     */
    @Select("SELECT login_type, COUNT(*) as count FROM " + TABLE + 
            " WHERE delflag = 0 GROUP BY login_type")
    List<java.util.Map<String, Object>> selectUserCountByLoginType();

    /**
     * 查询最近登录的用户
     * 
     * @param limit 限制数量
     * @return 用户列表
     */
    @Select("SELECT * FROM " + TABLE + " WHERE delflag = 0 AND lastLoginDate IS NOT NULL " +
            "ORDER BY lastLoginDate DESC LIMIT #{limit}")
    List<Users> selectRecentLoginUsers(@Param("limit") int limit);
}