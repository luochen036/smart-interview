package com.luochen.questionservice.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luochen.questionservice.entity.Question;
import com.luochen.questionservice.repository.QuestionRepository;
import com.luochen.questionservice.util.PermissionGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/questions")
public class QuestionController {

    @Autowired private QuestionRepository questionRepo;
    @Autowired private StringRedisTemplate redis;      // Redis 客户端
    @Autowired private ObjectMapper objectMapper;      // JSON 工具

    // 分类参数为空时返回全部题目，便于管理员统一维护。
    @GetMapping
    public List<Question> list(@RequestParam(required = false) String category) throws Exception {
        if (category == null || category.isBlank()) {
            return questionRepo.findAll();
        }
        String key = "questions:" + category;          // 缓存键
        String cached = redis.opsForValue().get(key);  // ① 先问 Redis
        if (cached != null) {
            return objectMapper.readValue(cached, new TypeReference<List<Question>>() {});
        }
        List<Question> qs = questionRepo.findByCategory(category);   // ② 没缓存就查 MySQL
        redis.opsForValue().set(key, objectMapper.writeValueAsString(qs),
                Duration.ofMinutes(10));              // ③ 写回 Redis
        return qs;
    }

    // 随机组卷：POST /questions/paper  {category, count}
    @PostMapping("/paper")
    public List<Question> paper(@RequestBody Map<String, Object> body) throws Exception {
        List<Question> all = new ArrayList<>(list((String) body.get("category")));
        Collections.shuffle(all);                       // 洗牌
        int count = Math.min((Integer) body.get("count"), all.size());
        return all.subList(0, count);                   // 抽前 N 道
    }

    @GetMapping("/{id}")
    public Question get(@PathVariable Long id) {
        return questionRepo.findById(id).orElseThrow();
    }

    @PostMapping
    public Question create(@RequestBody Question question,
                           @RequestHeader(value = "Authorization", required = false) String role) {
        PermissionGuard.requireAdmin(role);
        question.setId(null);
        Question saved = questionRepo.save(question);
        clearCache(saved.getCategory());
        return saved;
    }

    @PutMapping("/{id}")
    public Question update(@PathVariable Long id, @RequestBody Question input,
                           @RequestHeader(value = "Authorization", required = false) String role) {
        PermissionGuard.requireAdmin(role);
        Question question = questionRepo.findById(id).orElseThrow();
        String oldCategory = question.getCategory();
        question.setCategory(input.getCategory());
        question.setTitle(input.getTitle());
        question.setAnswer(input.getAnswer());
        question.setDifficulty(input.getDifficulty());
        Question saved = questionRepo.save(question);
        clearCache(oldCategory);
        clearCache(saved.getCategory());
        return saved;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id,
                       @RequestHeader(value = "Authorization", required = false) String role) {
        PermissionGuard.requireAdmin(role);
        Question question = questionRepo.findById(id).orElseThrow();
        questionRepo.delete(question);
        clearCache(question.getCategory());
    }

    // 题目变更后立即删除旧缓存，避免用户读到过期数据。
    private void clearCache(String category) {
        if (category != null) {
            redis.delete("questions:" + category);
        }
    }
}
