import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinterview.entity.Category;
import com.smartinterview.entity.Question;
import com.smartinterview.repository.CategoryRepository;
import com.smartinterview.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/questions")
public class QuestionController {

    @Autowired private CategoryRepository categoryRepo;
    @Autowired private QuestionRepository questionRepo;
    @Autowired private StringRedisTemplate redis;      // Redis 客户端
    @Autowired private ObjectMapper objectMapper;      // JSON 工具

    // 分类列表：GET /questions/categories
    @GetMapping("/categories")
    public List<Category> categories() {
        return categoryRepo.findAll();
    }

    // 某分类的题目（带缓存）：GET /questions?category=Redis
    @GetMapping
    public List<Question> list(@RequestParam String category) throws Exception {
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
        List<Question> all = list((String) body.get("category"));
        Collections.shuffle(all);                       // 洗牌
        int count = Math.min((Integer) body.get("count"), all.size());
        return all.subList(0, count);                   // 抽前 N 道
    }
}