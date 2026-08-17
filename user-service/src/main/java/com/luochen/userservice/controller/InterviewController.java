package com.luochen.userservice.controller;

import com.luochen.userservice.entity.InterviewRecord;
import com.luochen.userservice.repository.InterviewRepository;
import com.luochen.userservice.util.PermissionGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/interviews")
public class InterviewController {

    @Autowired
    private InterviewRepository repo;

    // 保存一次面试：POST /interviews
    @PostMapping
    public Map<String, Object> save(@RequestBody InterviewRecord record) {
        repo.save(record);
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("recordId", record.getId());
        return result;
    }

    // 查历史记录：GET /interviews/user/{userId}
    @GetMapping("/user/{userId}")
    public List<InterviewRecord> list(@PathVariable Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @GetMapping("/{id}")
    public InterviewRecord get(@PathVariable Long id,
                               @RequestHeader(value = "Authorization", required = false) String role) {
        PermissionGuard.requireAdmin(role);
        return repo.findById(id).orElseThrow();
    }

    @GetMapping
    public List<InterviewRecord> listAll(@RequestHeader(value = "Authorization", required = false) String role) {
        PermissionGuard.requireAdmin(role);
        return repo.findAll();
    }

    @PutMapping("/{id}")
    public InterviewRecord update(@PathVariable Long id, @RequestBody InterviewRecord input,
                                  @RequestHeader(value = "Authorization", required = false) String role) {
        PermissionGuard.requireAdmin(role);
        InterviewRecord record = repo.findById(id).orElseThrow();
        record.setUserId(input.getUserId());
        record.setResume(input.getResume());
        record.setQuestions(input.getQuestions());
        record.setScore(input.getScore());
        record.setFeedback(input.getFeedback());
        return repo.save(record);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id,
                       @RequestHeader(value = "Authorization", required = false) String role) {
        PermissionGuard.requireAdmin(role);
        repo.deleteById(id);
    }
}
