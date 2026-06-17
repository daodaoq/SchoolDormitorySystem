package org.java.backed.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.java.backed.entity.SysMenu;
import org.java.backed.entity.SysRoleMenu;
import java.util.List;

@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {
    @Select("SELECT m.permission_code FROM sys_role_menu rm JOIN sys_menu m ON rm.menu_id = m.id WHERE rm.role_id = #{roleId} AND m.permission_code IS NOT NULL")
    List<String> findPermissionsByRoleId(Long roleId);

    @Select("SELECT m.* FROM sys_role_menu rm JOIN sys_menu m ON rm.menu_id = m.id WHERE rm.role_id = #{roleId} AND m.visible = 1 ORDER BY m.sort_order")
    List<SysMenu> findMenusByRoleId(Long roleId);
}
