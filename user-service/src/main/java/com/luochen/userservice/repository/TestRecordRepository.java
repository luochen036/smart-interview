import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TestRecordRepository extends JpaRepository<TestRecord, Long> {
    List<TestRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
}