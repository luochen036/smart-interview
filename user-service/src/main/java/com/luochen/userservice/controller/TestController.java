package com.luochen.userservice.controller;

import com.luochen.userservice.entity.TestRecord;
import com.luochen.userservice.config.RabbitConfig;
import com.luochen.userservice.repository.TestRecordRepository;
import com.luochen.userservice.util.PermissionGuard;
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
        // ⚠️ 必须发 String！RabbitTemplate 默认对 Long 用 Java 序列化，Python 消费者解析不了
        publishReportTask(record.getId());
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

    // 报告长时间未完成时允许重新投递，消息队列异常恢复后无需重新答题。
    @PostMapping("/{id}/retry")
    public Map<String, Object> retry(@PathVariable Long id) {
        TestRecord record = repo.findById(id).orElseThrow();
        if (!"done".equals(record.getStatus())) {
            record.setStatus("reporting");
            repo.save(record);
            publishReportTask(id);
        }
        return Map.of("ok", true, "status", record.getStatus());
    }

    // 历史：GET /tests/user/{userId}（个人中心用）
    @GetMapping("/user/{userId}")
    public List<TestRecord> history(@PathVariable Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @GetMapping
    public List<TestRecord> list(@RequestHeader(value = "Authorization", required = false) String role) {
        PermissionGuard.requireAdmin(role);
        return repo.findAll();
    }

    @PutMapping("/{id}")
    public TestRecord update(@PathVariable Long id, @RequestBody TestRecord input,
                             @RequestHeader(value = "Authorization", required = false) String role) {
        PermissionGuard.requireAdmin(role);
        TestRecord record = repo.findById(id).orElseThrow();
        record.setUserId(input.getUserId());
        record.setCategory(input.getCategory());
        record.setQuestions(input.getQuestions());
        record.setAnswers(input.getAnswers());
        record.setScore(input.getScore());
        record.setReport(input.getReport());
        record.setStatus(input.getStatus());
        return repo.save(record);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id,
                       @RequestHeader(value = "Authorization", required = false) String role) {
        PermissionGuard.requireAdmin(role);
        repo.deleteById(id);
    }

    private void publishReportTask(Long testId) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.TEST_EXCHANGE,
                RabbitConfig.TEST_QUEUE,
                String.valueOf(testId)
        );
    }
}
