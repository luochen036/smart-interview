package com.luochen.userservice.controller;

import com.luochen.userservice.entity.StudyRecord;
import com.luochen.userservice.repository.StudyRecordRepository;
import com.luochen.userservice.util.PermissionGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/study")
public class StudyController {

    @Autowired
    private StudyRecordRepository repo;

    // 学习打卡：POST /study  {userId, questionId}
    @PostMapping
    public Map<String, Object> mark(@RequestBody StudyRecord record) {
        repo.save(record);
        Map<String, Object> r = new HashMap<>();
        r.put("ok", true);
        return r;
    }

    // 学习进度：GET /study/user/{userId}（个人中心用）
    @GetMapping("/user/{userId}")
    public List<StudyRecord> progress(@PathVariable Long userId) {
        return repo.findByUserId(userId);
    }

    @GetMapping("/{id}")
    public StudyRecord get(@PathVariable Long id,
                           @RequestHeader(value = "Authorization", required = false) String role) {
        PermissionGuard.requireAdmin(role);
        return repo.findById(id).orElseThrow();
    }

    @GetMapping
    public List<StudyRecord> list(@RequestHeader(value = "Authorization", required = false) String role) {
        PermissionGuard.requireAdmin(role);
        return repo.findAll();
    }

    @PutMapping("/{id}")
    public StudyRecord update(@PathVariable Long id, @RequestBody StudyRecord input,
                              @RequestHeader(value = "Authorization", required = false) String role) {
        PermissionGuard.requireAdmin(role);
        StudyRecord record = repo.findById(id).orElseThrow();
        record.setUserId(input.getUserId());
        record.setQuestionId(input.getQuestionId());
        return repo.save(record);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id,
                       @RequestHeader(value = "Authorization", required = false) String role) {
        PermissionGuard.requireAdmin(role);
        repo.deleteById(id);
    }
}
