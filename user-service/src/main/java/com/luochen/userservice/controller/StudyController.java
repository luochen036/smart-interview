import com.smartinterview.entity.StudyRecord;
import com.smartinterview.repository.StudyRecordRepository;
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
}