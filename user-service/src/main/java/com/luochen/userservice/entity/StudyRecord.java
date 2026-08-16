import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
// 学习记录实体类
@Data
@Entity
@Table(name = "study_records")
public class StudyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long questionId;
    private LocalDateTime createdAt = LocalDateTime.now();
}