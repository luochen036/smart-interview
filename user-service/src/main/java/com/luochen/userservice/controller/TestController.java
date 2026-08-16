package com.luochen.userservice.controller;

import com.luochen.userservice.entity.TestRecord;
import com.luochen.userservice.repository.TestRecordRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/tests")
public class TestController {

    @Autowired
    private TestRecordRepository repo;

    @Autowired
    private RabbitTemplate rabbitTemplate;   // 发消息的"邮递员"

    // 提交测试：POST /tests  {userId, category, questions, answers}
    @PostMapping
    public Map<String, Object> submit(@RequestBody TestRecord record) {
        record.setStatus("reporting");       // 报告生成中
        repo.save(record);
        // 📨 投递消息：AI 服务稍后异步生成报告，用户不用傻等
        rabbitTemplate.convertAndSend("test.exchange", "test.submitted", record.getId());
        Map<String, Object> r = new HashMap<>();
        r.put("ok", true);
        r.put("testId", record.getId());
        r.put("status", "reporting");
        return r;
    }

    // AI 回写报告：POST /tests/{id}/report  {score, report}
    @PostMapping("/{id}/report")
    public Map<String, Object> saveReport(@PathVariable Long id,
                                          @RequestBody Map<String, String> body) {
        TestRecord t = repo.findById(id).orElse(null);
        Map<String, Object> r = new HashMap<>();
        if (t == null) { r.put("ok", false); return r; }
        t.setScore(Integer.valueOf(body.get("score")));
        t.setReport(body.get("report"));
        t.setStatus("done");                 // 报告完成
        repo.save(t);
        r.put("ok", true);
        return r;
    }

    // 取单条测试：GET /tests/{id}（MQ 消费者取答卷用）
    @GetMapping("/{id}")
    public TestRecord get(@PathVariable Long id) {
        return repo.findById(id).orElse(null);
    }

    // 历史：GET /tests/user/{userId}（个人中心用）
    @GetMapping("/user/{userId}")
    public List<TestRecord> history(@PathVariable Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }
}