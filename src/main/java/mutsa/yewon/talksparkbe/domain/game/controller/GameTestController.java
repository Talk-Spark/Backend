package mutsa.yewon.talksparkbe.domain.game.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import mutsa.yewon.talksparkbe.domain.card.entity.Card;
import mutsa.yewon.talksparkbe.domain.game.service.dto.CardBlanksDto;
import mutsa.yewon.talksparkbe.domain.game.service.dto.CardQuestion;
import mutsa.yewon.talksparkbe.domain.game.service.dto.UserCardQuestions;
import mutsa.yewon.talksparkbe.domain.game.service.util.GameState;
import mutsa.yewon.talksparkbe.domain.game.service.util.QuestionGenerator;
import mutsa.yewon.talksparkbe.domain.sparkUser.entity.SparkUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/test")
@RestController
@RequiredArgsConstructor
public class GameTestController {

    @Getter
    private final Map<Long, GameState> gameStates = new HashMap<>();
    private final QuestionGenerator questionGenerator;

    @GetMapping
    public ResponseEntity<?> test() {
        SparkUser user1 = SparkUser.builder()
                .id(1L)
                .kakaoId("eriulfvliadnfiuva")
                .name("abc")
                .password("gaknfkalenfjknalekf")
                .build();
        SparkUser user2 = SparkUser.builder()
                .id(2L)
                .kakaoId("vxczvc")
                .name("def")
                .password("eiunvblaef")
                .build();
        Card card1 = Card.builder()
                .id(1L)
                .name("abc")
                .age(25)
                .major("컴공")
                .mbti("istp")
                .hobby("취미없음")
                .lookAlike("닮음")
                .slogan("뭘봐")
                .tmi("zxczxcv")
                .sparkUser(user1)
                .build();
        Card card2 = Card.builder()
                .id(2L)
                .name("def")
                .age(26)
                .major("컴공")
                .mbti("intp")
                .hobby("취미")
                .lookAlike("닮음aaa")
                .slogan("뭘봐zxvc")
                .tmi("pompm")
                .sparkUser(user2)
                .build();

        List<UserCardQuestions> questions = questionGenerator.execute(List.of(card1, card2), 2);

        GameState gameState = new GameState(List.of(card1, card2), questions); // 게임 상태를 초기화
        gameStates.put(1L, gameState); // 특정 방 번호에 게임 상태 할당

        // 각 참가자들마다의 빈칸정보를 만들기
        for (UserCardQuestions ucq : questions) {
            Long sparkUserId = ucq.getSparkUserId();
            List<String> blanks = ucq.getQuestions().stream().map(CardQuestion::getFieldName).toList();
            gameStates.get(1L).getCardBlanksDtos().add(CardBlanksDto.of(sparkUserId, blanks));
        }

        return ResponseEntity.ok(gameStates);
    }

    @GetMapping("/cur-question")
    public ResponseEntity<?> currentQuestion() {
        return ResponseEntity.ok(gameStates.get(1L).getCurrentQuestion());
    }

    @GetMapping("/cur-blank")
    public ResponseEntity<?> currentBlanks() {
        return ResponseEntity.ok(gameStates.get(1L).getCurrentCardBlanks());
    }

}
