package mutsa.yewon.talksparkbe.domain.game.service;

import lombok.RequiredArgsConstructor;
import mutsa.yewon.talksparkbe.domain.game.controller.request.RoomCreateRequest;
import mutsa.yewon.talksparkbe.domain.game.controller.request.RoomJoinRequest;
import mutsa.yewon.talksparkbe.domain.game.entity.Room;
import mutsa.yewon.talksparkbe.domain.game.entity.RoomParticipate;
import mutsa.yewon.talksparkbe.domain.game.repository.RoomParticipateRepository;
import mutsa.yewon.talksparkbe.domain.game.repository.RoomRepository;
import mutsa.yewon.talksparkbe.domain.game.service.dto.httpResponse.RoomListResponse;
import mutsa.yewon.talksparkbe.domain.game.service.dto.httpResponse.RoomParticipantResponse;
import mutsa.yewon.talksparkbe.domain.sparkUser.entity.SparkUser;
import mutsa.yewon.talksparkbe.domain.sparkUser.repository.SparkUserRepository;
import mutsa.yewon.talksparkbe.global.exception.CustomTalkSparkException;
import mutsa.yewon.talksparkbe.global.exception.ErrorCode;
import mutsa.yewon.talksparkbe.global.util.JWTUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final SparkUserRepository sparkUserRepository;
    private final RoomRepository roomRepository;
    private final RoomParticipateRepository roomParticipateRepository;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;

    private final JWTUtil jwtUtil;

    private static final String ROOM_COUNT_KEY = "room:participateCount";

    @Transactional
    public Long createRoom(RoomCreateRequest roomCreateRequest) {
//        SparkUser sparkUser = sparkUserRepository.findById(roomCreateRequest.getHostId()).orElseThrow(() -> new RuntimeException("사람 못찾음"));
        Room room = roomRepository.save(roomCreateRequest.toRoomEntity());
//        RoomParticipate roomParticipate = RoomParticipate.builder().room(room).sparkUser(sparkUser).isOwner(true).build();
//        roomParticipateRepository.save(roomParticipate);
//        room.assignRoomParticipate(roomParticipate);

        redisTemplate.opsForHash().put(ROOM_COUNT_KEY, room.getRoomId().toString(), "0");

        return room.getRoomId();
    }

    public List<RoomListResponse> listAllRooms() {
        List<Room> rooms = roomRepository.findAllWithParticipates();
        List<RoomListResponse> roomListResponses = new ArrayList<>();

        for (Room r : rooms) {
            RoomListResponse response = RoomListResponse.from(r);
            response.setHostName(r.getRoomParticipates().get(0).getSparkUser().getName());
            response.setCurrentPeople(getParticipateCount(r.getRoomId()));

            roomListResponses.add(response);
        }

        return roomListResponses;
    }

    @Transactional
    public boolean joinRoom(RoomJoinRequest roomJoinRequest) {
        Room room = roomRepository.findById(roomJoinRequest.getRoomId()).orElseThrow(() -> new RuntimeException("방 못찾음"));

        String jwt = roomJoinRequest.getAccessToken().replace("Bearer ", "");
        Map<String, Object> claims = jwtUtil.validateToken(jwt);
        String kakaoId = (String) claims.get("kakaoId");
        SparkUser sparkUser = sparkUserRepository.findByKakaoId(kakaoId).orElseThrow(() -> new RuntimeException("유저 못찾음"));

        // 방에 입장할 때 락을 획득
        RLock lock = redissonClient.getLock("roomLock:" + roomJoinRequest.getRoomId());

        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) { // 최대 대기 시간 5초, 락 보유 시간 10초로 설정
                if (!canJoin(room)) throw new CustomTalkSparkException(ErrorCode.ROOM_FULL);
                else {
                    addParticipateToRoom(room, sparkUser, roomJoinRequest.getIsHost());
                    return true;
                }
            } else throw new CustomTalkSparkException(ErrorCode.LOCK_TIMEOUT); // 락을 획득하지 못한 경우 (대기 시간 초과)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomTalkSparkException(ErrorCode.ROOM_JOIN_INTERRUPTED);
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    public List<RoomParticipantResponse> getParticipantList(Long roomId) {
        List<RoomParticipantResponse> response = new ArrayList<>();
        List<RoomParticipate> roomParticipates = roomParticipateRepository.findByRoomIdWithSparkUser(roomId);

        for (RoomParticipate rp : roomParticipates)
            response.add(RoomParticipantResponse.from(rp));

        return response;
    }

    public boolean checkHost(Long roomId, SparkUser sparkUser) {
        RoomParticipate ownerParticipate = roomParticipateRepository.findByRoomIdWithOwner(roomId).orElseThrow(() -> new CustomTalkSparkException(ErrorCode.ROOM_NOT_FOUND));
        return (sparkUser.equals(ownerParticipate.getSparkUser()));
    }

    public int getParticipateCount(Long roomId) {
        String count = (String) redisTemplate.opsForHash().get(ROOM_COUNT_KEY, roomId.toString());
        return count != null ? Integer.parseInt(count) : 0;
    }

    @Transactional
    public boolean leaveRoom(RoomJoinRequest roomJoinRequest) {
        Room room = roomRepository.findById(roomJoinRequest.getRoomId()).orElseThrow(() -> new RuntimeException("방 못찾음"));

        System.out.println("roomJoinRequest 에서 토큰 = " + roomJoinRequest.getAccessToken());
        String jwt = roomJoinRequest.getAccessToken().replace("Bearer ", "");
        Map<String, Object> claims = jwtUtil.validateToken(jwt);
        String kakaoId = (String) claims.get("kakaoId");
        SparkUser sparkUser = sparkUserRepository.findByKakaoId(kakaoId).orElseThrow(() -> new RuntimeException("유저 못찾음"));

        // 방에서 퇴장할 때 락을 획득
        RLock lock = redissonClient.getLock("roomLock:" + roomJoinRequest.getRoomId());

        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                if (!canJoin(room)) throw new CustomTalkSparkException(ErrorCode.ROOM_FULL);
                else {
                    removeParticipateToRoom(room, sparkUser);
                    return true;
                }
            } else throw new CustomTalkSparkException(ErrorCode.LOCK_TIMEOUT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomTalkSparkException(ErrorCode.ROOM_JOIN_INTERRUPTED);
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    private boolean canJoin(Room room) {
        int currentCount = getParticipateCount(room.getRoomId());
        return currentCount < room.getMaxPeople();
    }
    private void addParticipateToRoom(Room room, SparkUser sparkUser, boolean isHost) {
        RoomParticipate roomParticipate = RoomParticipate.builder().room(room).sparkUser(sparkUser).isOwner(isHost).build();
        roomParticipateRepository.save(roomParticipate);
        room.assignRoomParticipate(roomParticipate);
        redisTemplate.opsForHash().increment(ROOM_COUNT_KEY, room.getRoomId().toString(), 1);
    }
    private void removeParticipateToRoom(Room room, SparkUser sparkUser) {
        RoomParticipate roomParticipate = roomParticipateRepository.findByRoomAndSparkUser(room, sparkUser);
        roomParticipateRepository.delete(roomParticipate);
        redisTemplate.opsForHash().increment(ROOM_COUNT_KEY, room.getRoomId().toString(), -1);
    }

}
