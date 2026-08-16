import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String category;
    @Lob
    private String title;     // 题目
    @Lob
    private String answer;    // 参考答案
    private Integer difficulty = 1;   // 1 简单 2 中等 3 困难
}
