import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "test_records")
public class TestRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String category;

    @Lob
    private String questions;   // 试卷 JSON

    @Lob
    private String answers;     // 答卷 JSON

    private Integer score;      // AI 打的分数

    @Lob
    private String report;      // AI 分析报告

    private String status = "reporting";   // reporting → done

    private LocalDateTime createdAt = LocalDateTime.now();
}