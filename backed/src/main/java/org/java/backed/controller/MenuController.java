package org.java.backed.controller;

import lombok.RequiredArgsConstructor;
import org.java.backed.common.Result;
import org.java.backed.entity.SysMenu;
import org.java.backed.service.MenuService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/tree")
    public Result<List<SysMenu>> tree() {
        return Result.ok(menuService.getAllMenuTree());
    }

    @PostMapping
    public Result<SysMenu> add(@RequestBody SysMenu menu) {
        menuService.save(menu);
        return Result.ok(menu);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysMenu menu) {
        menu.setId(id);
        menuService.updateById(menu);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.removeById(id);
        return Result.ok();
    }
}
