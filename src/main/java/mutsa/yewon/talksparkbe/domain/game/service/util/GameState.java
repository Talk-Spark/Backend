package mutsa.yewon.talksparkbe.domain.game.service.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import mutsa.yewon.talksparkbe.domain.card.entity.Card;
import mutsa.yewon.talksparkbe.domain.game.service.dto.*;
import mutsa.yewon.talksparkbe.domain.sparkUser.entity.SparkUser;
import mutsa.yewon.talksparkbe.domain.sparkUser.repository.SparkUserRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@Getter
@Log4j2
public class GameState {
 // 현재 문제의 정답 여부, 답을 한 인원 수
    private final Map<Long, Card> playerInfo = new HashMap<>(); // 참가자 ID : 참가자 명함
    private final List<CardQuestion> questions; // 전체 문제
    private Long currentSubjectId; // 차라리 현재 질문지를 관리하는게..
    private final List<CorrectAnswerDto> currentQuestionCorrect = new ArrayList<>(); // 현재 문제 정답 여부. 유저아이디 : 맞춤
    private final Map<Long, Integer> scores = new HashMap<>(); // 유저아이디 : 점수
    private final List<CardBlanksDto> cardBlanksDtos = new ArrayList<>();

    public GameState(List<Card> cards, List<UserCardQuestions> userCardQuestions) {
        this.questions = new LinkedList<>();
        userCardQuestions.forEach(q -> {
                    playerInfo.put(q.getSparkUserId(), matchToQuestion(cards, q.getSparkUserId()));
                    questions.addAll(q.getQuestions());
                });
        this.currentSubjectId = userCardQuestions.get(0).getSparkUserId();
    }

    public CardQuestion getCurrentQuestion() {
        return questions.get(0);
    }

    public boolean hasNextQuestion() {
        return !questions.isEmpty();
    }

    public void recordScore(Long sparkUserId, String answer) {
        CardQuestion currentQuestion = questions.get(0);

        log.info("현재 문제 정보 = " + currentQuestion);

//        Card card = cards.stream()// TODO : 참여자 정보를 객체로! (참여자 ID, 명함 테마, 참여자 이름)
//                .filter(c -> c.getSparkUser().getId().equals(sparkUserId)).findFirst()
//                .orElseThrow();

        Card card = playerInfo.get(sparkUserId);

        if (currentQuestion.getCorrectAnswer().equals(answer)) { // 정답을 맞춘 경우

            log.info("답안 맞춤 " + sparkUserId + "제출한 답안 = " + answer);

            scores.put(sparkUserId, scores.getOrDefault(sparkUserId, 0) + 1); // 점수 +1

            currentQuestionCorrect.add(CorrectAnswerDto.of(sparkUserId, Boolean.TRUE, card));

        }

        else{

            log.info("답안 틀림 " + sparkUserId + "제출한 답안 = " + answer);

            currentQuestionCorrect.add(CorrectAnswerDto.of(sparkUserId, Boolean.FALSE, card));
        }

        log.info("currentstate = " + currentQuestionCorrect);
    }

    public Integer getCurrentQuestionAnswerNum() {
//        CardQuestion currentQuestion = questions.get(0);
//        return answerNums.get(currentQuestion);

        return currentQuestionCorrect.size();
    }

    public void loadNextQuestion() {
        if (questions.isEmpty()) return;
        questions.remove(0);
        currentQuestionCorrect.clear();
    }

    public SwitchSubject isSwitchingSubject() {
        if (questions.size() < 2) return SwitchSubject.END;
        else if (!questions.get(1).getCardOwnerId().equals(currentSubjectId)) {
            currentSubjectId = questions.get(1).getCardOwnerId();
            return SwitchSubject.TRUE;
        } else return SwitchSubject.FALSE;
    }

    public Card getCurrentCard() {
        Long cardOwnerId = getCurrentQuestion().getCardOwnerId();
        return playerInfo.get(cardOwnerId);
//        return cards.stream().filter(c -> c.getId().equals(cardId)).findFirst().orElse(null);
    }

    public CardBlanksDto getCurrentCardBlanks() {
        Long sparkUserId = getCurrentQuestion().getCardOwnerId();
        return cardBlanksDtos.stream().filter(cbd -> cbd.getSparkUserId().equals(sparkUserId)).findFirst().orElse(null);
    }

    private Card matchToQuestion(List<Card> cards, Long sparkUserId) {
        return cards.stream()
                .filter(c -> c.getSparkUser().getId().equals(sparkUserId)).findFirst()
                .orElseThrow();
    }
}
