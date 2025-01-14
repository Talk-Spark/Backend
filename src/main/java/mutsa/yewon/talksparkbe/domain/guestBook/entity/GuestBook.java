package mutsa.yewon.talksparkbe.domain.guestBook.entity;

import jakarta.persistence.*;
import lombok.*;
import mutsa.yewon.talksparkbe.domain.game.entity.Room;
import mutsa.yewon.talksparkbe.domain.sparkUser.entity.SparkUser;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class GuestBook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long guestBookId;

    @ManyToOne
    @JoinColumn(name = "spark_user_id")
    private SparkUser sparkUser;

    private String guestBookContent;

    @CreatedDate
    private LocalDateTime guestBookDateTime;

    private boolean anonymity;

    @Builder
    public GuestBook(SparkUser sparkUser, String guestBookContent, boolean anonymity) {
        this.sparkUser = sparkUser;
        this.guestBookContent = guestBookContent;
        this.guestBookDateTime = LocalDateTime.now();
        this.anonymity = anonymity;
    }

    @Setter
    @ManyToOne(cascade = CascadeType.REMOVE) //guestBookRoom삭제 시 같이 삭제됨
    @JoinColumn(name = "guestbook_room_id")
    private GuestBookRoom guestBookRoom;

}
