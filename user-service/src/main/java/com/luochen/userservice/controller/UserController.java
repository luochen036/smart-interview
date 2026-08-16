package com.luochen.userservice.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.luochen.userservice.entity.User;
import com.luochen.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepo;

    // 注册：POST /users/register
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        if (userRepo.findByUsername(user.getUsername()) != null) {
            result.put("ok", false);
            result.put("message", "用户名已存在");
            return result;
        }
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
}