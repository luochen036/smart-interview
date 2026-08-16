import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudyRecordRepository extends JpaRepository<StudyRecord, Long> {
    List<StudyRecord> findByUserId(Long userId);
}