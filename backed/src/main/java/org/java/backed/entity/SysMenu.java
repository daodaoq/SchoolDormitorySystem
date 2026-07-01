package org.java.backed.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_menu")
public class SysMenu {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String menuName;

    private String menuType;

    private String path;

    private String icon;

    private String permissionCode;

    private Integer sortOrder;

    private Integer visible;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private List<SysMenu> children;
}
