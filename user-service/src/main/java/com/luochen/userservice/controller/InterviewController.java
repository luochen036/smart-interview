import com.smartinterview.entity.InterviewRecord;
import com.smartinterview.repository.InterviewRepository;
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
}