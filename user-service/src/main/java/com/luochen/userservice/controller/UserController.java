package com.luochen.userservice.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.luochen.userservice.entity.User;
import com.luochen.userservice.repository.UserRepository;
import com.luochen.userservice.util.PermissionGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepo;

    // 注册入口只允许创建普通用户，管理员由已有管理员在后台设置。
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        if (userRepo.findByUsername(user.getUsername()) != null) {
            result.put("ok", false);
            result.put("message", "用户名已存在");
            return result;
        }
        user.setRole("user");
        userRepo.save(user);
        result.put("ok", true);
        result.put("userId", user.getId());
        return result;
    }

    // 登录：POST /users/login
    // value = Sentinel 里的资源名；blockHandler = 被限流/熔断时走的兜底方法
    @PostMapping("/login")
    @SentinelResource(value = "userLogin", blockHandler = "loginBlocked")
    public Map<String, Object> login(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        User found = userRepo.findByUsername(user.getUsername());
        if (found != null && found.getPassword().equals(user.getPassword())) {
            result.put("ok", true);
            result.put("userId", found.getId());
            result.put("message", "登录成功");
            result.put("role", found.getRole());
            result.put("token", PermissionGuard.issueToken(found.getId(), found.getRole()));
        } else {
            result.put("ok", false);
            result.put("message", "用户名或密码错误");
        }
        return result;
    }

    // 兜底方法：注意参数 = 原参数 + BlockException
    public Map<String, Object> loginBlocked(User user, BlockException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("ok", false);
        result.put("message", "🛡️ 系统繁忙，请稍后再试～");
        return result;
    }

    // 以下接口供管理员维护用户数据。
    @GetMapping
    public List<User> list(@RequestHeader(value = "Authorization", required = false) String role) {
        PermissionGuard.requireAdmin(role);
        return userRepo.findAll();
    }

    @GetMapping("/{id}")
    public User get(@PathVariable Long id,
                    @RequestHeader(value = "Authorization", required = false) String role) {
        PermissionGuard.requireAdmin(role);
        return userRepo.findById(id).orElseThrow();
    }

    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody User input,
                       @RequestHeader(value = "Authorization", required = false) String role) {
        PermissionGuard.requireAdmin(role);
        User user = userRepo.findById(id).orElseThrow();
        user.setUsername(input.getUsername());
        user.setRole(input.getRole());
        if (input.getPassword() != null && !input.getPassword().isBlank()) {
            user.setPassword(input.getPassword());
        }
        return userRepo.save(user);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id,
                       @RequestHeader(value = "Authorization", required = false) String role) {
        PermissionGuard.requireAdmin(role);
        userRepo.deleteById(id);
    }
}
