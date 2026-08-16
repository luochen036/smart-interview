import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
// 面试记录实体类
@Data
@Entity
@Table(name = "interview_records")
public class InterviewRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Lob
    private String resume;      // 简历原文

    @Lob
    private String questions;   // AI 出的题（JSON 字符串）

    private Integer score;      // 平均分

    @Lob
    private String feedback;    // 点评

    private LocalDateTime createdAt = LocalDateTime.now();
}