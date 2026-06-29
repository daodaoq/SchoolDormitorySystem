package org.java.backed.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解 — 标记 Controller 方法自动记录审计日志
 *
 * 使用示例:
 * @OpLog(module = "学生管理", action = "新增", description = "新增学生")
 * @PostMapping
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpLog {

    /** 操作模块，如「学生管理」「账单管理」「系统管理」 */
    String module();

    /** 操作类型，如「新增」「修改」「删除」「登录」「支付」 */
    String action();

    /** 操作描述模板，支持 SpEL 占位符 #{#paramName} */
    String description() default "";

    /** 是否记录请求参数 */
    boolean logParams() default true;

    /** 是否记录响应结果 */
    boolean logResult() default false;
}
