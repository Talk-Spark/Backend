package mutsa.yewon.talksparkbe.domain.game.controller;

import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mutsa.yewon.talksparkbe.domain.card.entity.Card;
import mutsa.yewon.talksparkbe.domain.card.entity.CardThema;
import mutsa.yewon.talksparkbe.domain.card.repository.CardRepository;
import mutsa.yewon.talksparkbe.domain.game.controller.request.*;
import mutsa.yewon.talksparkbe.domain.game.service.dto.CardQuestion;
import mutsa.yewon.talksparkbe.domain.game.service.GameService;
import mutsa.yewon.talksparkbe.domain.game.service.RoomService;
import mutsa.yewon.talksparkbe.domain.game.service.dto.CorrectAnswerDto;
import mutsa.yewon.talksparkbe.domain.game.service.dto.SwitchSubject;
import mutsa.yewon.talksparkbe.domain.sparkUser.entity.SparkUser;
import mutsa.yewon.talksparkbe.domain.sparkUser.repository.SparkUserRepository;
import mutsa.yewon.talksparkbe.global.exception.CustomTalkSparkException;
import mutsa.yewon.talksparkbe.global.util.JWTUtil;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import mutsa.yewon.talksparkbe.global.util.SecurityUtil;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoomSocketIOHandler {

    private final SocketIOServer server;
    private final RoomService roomService;
    private final GameService gameService;
    private final SparkUserRepository sparkUserRepository;
    private final JWTUtil jwtUtil;
    private final CardRepository cardRepository;

    @PostConstruct
    public void startServer() {
        server.addConnectListener(client -> {
            System.out.println("Client connected: " + client.getSessionId());
            client.sendEvent("connectAck", "당신은 서버와 연결되었습니다.");
        });

        server.addEventListener("message", String.class, (client, data, ackSender) -> {
            System.out.println("Received: " + data);
            client.sendEvent("response", "Hello, client!");
        });

        server.addDisconnectListener(client -> {
            client.sendEvent("disconnecting", "당신은 이제 연결이 끊깁니다.");
        });
        server.start();
    }

    @PreDestroy
    public void stopServer() {
        try {
            if (server != null) {
                server.stop();
                log.info("Socket.IO 서버가 성공적으로 종료되었습니다.");
            } else {
                log.warn("Socket.IO 서버 인스턴스가 null입니다.");
            }
        } catch (Exception e) {
            log.error("Socket.IO 서버 종료 중 오류 발생", e);
        }
    }

    @PostConstruct
    public void startRoomListeners() {
        server.addEventListener("joinRoom", RoomJoinRequest.class, (client, data, ackSender) -> {
            try {
                System.out.println("joinRoom 받음. " + data.toString());
                roomService.joinRoom(data);
                server.getClient(client.getSessionId()).joinRoom(data.getRoomId().toString());
                server.getRoomOperations(data.getRoomId().toString()).sendEvent("roomUpdate", roomService.getParticipantList(data.getRoomId()));
            } catch (CustomTalkSparkException ce) {
                System.err.println("방 참가 중 서비스 오류 발생: " + ce.getErrorCode().getMessage());
                client.sendEvent("roomJoinError", "서비스 코드 때문에 방에 참가할 수 없습니다. 다시 시도해주세요.");
            } catch (Exception e) {
                System.err.println("방 참가 중 오류 발생: " + e.getMessage());
                client.sendEvent("roomJoinError", "방에 참가할 수 없습니다. 다시 시도해주세요.");
            }
        });

        server.addEventListener("leaveRoom", RoomJoinRequest.class, (client, data, ackSender) -> {
            try {
                System.out.println("leaveRoom 받음. " + data.toString());
                System.out.println("data 에서 액세스토큰 = " + data.getAccessToken());
                roomService.leaveRoom(data);
                server.getClient(client.getSessionId()).leaveRoom(data.getRoomId().toString());
                server.getRoomOperations(data.getRoomId().toString()).sendEvent("roomUpdate", roomService.getParticipantList(data.getRoomId()));
            } catch (CustomTalkSparkException ce) {
                client.sendEvent("roomLeaveError", "서비스 코드 때문에 방에서 퇴장할 수 없습니다. 다시 시도해주세요.");
            } catch (Exception e) {
                client.sendEvent("roomLeaveError", "방에서 퇴장할 수 없습니다. 다시 시도해주세요.");
            }
        });

        server.addEventListener("startGame", RoomJoinRequest.class, (client, data, ackSender) -> {
            try {
                Long roomId = data.getRoomId();
                roomService.changeStarted(roomId);
                server.getRoomOperations(roomId.toString()).sendEvent("startGame", "게임이 시작됩니다.");
            } catch (Exception e) {
                client.sendEvent("startGameError", "게임 시작 중 오류가 발생했습니다.");
            }
        });
    }

    /* ================ 게임 관련 ================ */

    @PostConstruct
    @Transactional
    public void startGameListeners() {
        server.addEventListener("joinGame", RoomJoinRequest.class, (client, data, ackSender) -> {
            server.getClient(client.getSessionId()).joinRoom(data.getRoomId().toString());

            String token = data.getAccessToken();
            String jwt = token.replace("Bearer ", "");
            Map<String, Object> claims = jwtUtil.validateToken(jwt);
            String kakaoId = (String) claims.get("kakaoId");
            SparkUser sparkUser = sparkUserRepository.findByKakaoId(kakaoId).orElseThrow(() -> new RuntimeException("유저 못찾음"));

            client.sendEvent("gameJoined", sparkUser.getId());
        });

        server.addEventListener("prepareQuizzes", GameStartRequest.class, (client, data, ackSender) -> {
            gameService.startGame(data.getRoomId());
        });

        server.addEventListener("getQuestion", QuestionRequest.class, (client, data, ackSender) -> {
            broadcastQuestion(data.getRoomId());
        });

        server.addEventListener("submitSelection", AnswerSubmitRequest.class, (client, data, ackSender) -> {
            Long roomId = data.getRoomId();
            Long sparkUserId = data.getSparkUserId();
            String answer = data.getAnswer();

            gameService.submitAnswer(roomId, sparkUserId, answer);
            if (gameService.allPeopleSubmitted(roomId)) {
                broadcastSingleQuestionResult(roomId);
                gameService.updateBlanks(roomId);
            }
        });

        server.addEventListener("next", QuestionRequest.class, (client, data, ackSender) -> {

            Long roomId = data.getRoomId();

            SwitchSubject switchSubject = gameService.isSwitchingSubject(roomId);

            switch (switchSubject){
                case END -> sendLastResult(roomId);
                case TRUE -> sendSingleResult(roomId);
                default ->  {
                    gameService.loadNextQuestion(roomId);
                    broadcastQuestion(roomId);
                }

            }

//            if (switchSubject.equals(SwitchSubject.END)) {
//                sendLastResult(roomId);
//            } else if (switchSubject.equals(SwitchSubject.TRUE)) {
//                sendSingleResult(roomId);
//            } else {
//                gameService.loadNextQuestion(roomId);
//                broadcastQuestion(roomId);
//            }
        });

        server.addEventListener("getEnd", EndRequest.class, (client, data, ackSender) -> {
            Long sparkUserId = data.getSparkUserId();
            Long roomId = data.getRoomId();

            server.getRoomOperations(roomId.toString()).sendEvent("scores", gameService.getScores(roomId), gameService.getAllRelatedCards(roomId));
            gameService.insertCardCopies(roomId, sparkUserId);
            gameService.endGame(roomId); // 게임 종료 시 저장하고 있던 게임 정보는 삭제
            roomService.changeFinished(roomId);
        });
    }

    private void sendSingleResult(Long roomId) {
        server.getRoomOperations(roomId.toString()).sendEvent("singleResult",
                gameService.getCurrentCard(roomId));
    }

    private void sendLastResult(Long roomId) {
        server.getRoomOperations(roomId.toString()).sendEvent("lastResult",
                gameService.getCurrentCard(roomId));
    }

    // 질문 브로드캐스트 메서드
    private void broadcastQuestion(Long roomId) {
        CardQuestion question = gameService.getQuestion(roomId);
        String roomName = roomService.getRoomName(roomId);
        server.getRoomOperations(roomId.toString()).sendEvent("question",
                gameService.getCurrentCard(roomId), gameService.getCurrentCardBlanks(roomId), question, roomName);
    }

    @Transactional(readOnly = true)
    public void broadcastSingleQuestionResult(Long roomId) {
        List<CorrectAnswerDto> singleQuestionScoreBoard = gameService.getSingleQuestionScoreBoard(roomId);

        if (!singleQuestionScoreBoard.isEmpty())
            server.getRoomOperations(roomId.toString()).sendEvent("singleQuestionScoreBoard", singleQuestionScoreBoard);
    }

}