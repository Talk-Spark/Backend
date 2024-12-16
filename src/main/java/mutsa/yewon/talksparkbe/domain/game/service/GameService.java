package mutsa.yewon.talksparkbe.domain.game.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import mutsa.yewon.talksparkbe.domain.card.dto.CardResponseDTO;
import mutsa.yewon.talksparkbe.domain.card.entity.Card;
import mutsa.yewon.talksparkbe.domain.game.entity.Room;
import mutsa.yewon.talksparkbe.domain.game.entity.RoomParticipate;
import mutsa.yewon.talksparkbe.domain.game.repository.RoomRepository;
import mutsa.yewon.talksparkbe.domain.game.service.dto.CardQuestion;
import mutsa.yewon.talksparkbe.domain.game.service.dto.SwitchSubject;
import mutsa.yewon.talksparkbe.domain.game.service.dto.UserCardQuestions;
import mutsa.yewon.talksparkbe.domain.game.service.util.GameState;
import mutsa.yewon.talksparkbe.domain.game.service.util.QuestionGenerator;
import mutsa.yewon.talksparkbe.domain.sparkUser.entity.SparkUser;
import mutsa.yewon.talksparkbe.global.exception.CustomTalkSparkException;
import mutsa.yewon.talksparkbe.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GameService {

    // TODO: 이거 레디스에 관리하면될듯
    // 방Id : 게임상태
    @Getter
    private final Map<Long, GameState> gameStates = new HashMap<>();

    private final RoomRepository roomRepository;
    private final QuestionGenerator questionGenerator;

    @Transactional(readOnly = true)
    public void startGame(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomTalkSparkException(ErrorCode.ROOM_NOT_FOUND));

        List<Card> selectedCards = room.getRoomParticipates().stream()
                .map(RoomParticipate::getSparkUser)
                .map(SparkUser::getCards)
                .map(cardList -> cardList.get(0)) // 갖고 있는 카드들 중 각각 가장 첫번째 카드 선택
                .toList(); // 참가자들의 명함 한장씩을 선택함

        List<UserCardQuestions> questions = questionGenerator.execute(selectedCards, room.getDifficulty()); // 선택된 명함들을 가지고 난이도를 기반으로 문제 만들기

        GameState gameState = new GameState(selectedCards, questions, room.getRoomParticipates().size()); // 게임 상태를 초기화
        gameStates.put(roomId, gameState); // 특정 방 번호에 게임 상태 할당
    }

    public CardQuestion getQuestion(Long roomId) {
        GameState gameState = gameStates.get(roomId);
        if (gameState == null || !gameState.hasNextQuestion()) return null;
        return gameState.getCurrentQuestion();
    }

    @Transactional(readOnly = true)
    public CardResponseDTO getCurrentCard(Long roomId) {
        return CardResponseDTO.fromCard(gameStates.get(roomId).getCurrentCard());
    }

    public void submitAnswer(Long roomId, Long sparkUserId, String answer) {
        GameState gameState = Optional.ofNullable(gameStates.get(roomId)).orElseThrow();
        gameState.recordScore(sparkUserId, answer);
    }

    public boolean allPeopleSubmitted(Long roomId) {
        GameState gameState = gameStates.get(roomId);
        return gameState.getCurrentQuestionAnswerNum().equals(gameState.getRoomPeople());
    }

    public Map<Long, Boolean> getSingleQuestionScoreBoard(Long roomId) {
        GameState gameState = gameStates.get(roomId);
        return gameState.getCurrentQuestionCorrect();
    }

    public void loadNextQuestion(Long roomId) {
        GameState gameState = gameStates.get(roomId);
        gameState.loadNextQuestion();
    }

    public SwitchSubject isSwitchingSubject(Long roomId) {
        GameState gameState = gameStates.get(roomId);
        return gameState.isSwitchingSubject();
    }

    public Map<Long, Integer> getScores(Long roomId) {
        GameState gameState = gameStates.get(roomId);
        return gameState == null ? Collections.emptyMap() : gameState.getScores();
    }

    public List<CardResponseDTO> getAllRelatedCards(Long roomId) {
        GameState gameState = gameStates.get(roomId);
        return gameState.getCards().stream().map(CardResponseDTO::fromCard).toList();
    }

    @Transactional
    public void insertCardCopies(Long roomId) {
        GameState gameState = gameStates.get(roomId);
        gameState.getParticipantIds();
        gameState.getCards();

        // 명함 보관함 있는 브랜치랑 병합 후 작업
    }

}