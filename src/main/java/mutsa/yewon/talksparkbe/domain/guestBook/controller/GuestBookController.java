package mutsa.yewon.talksparkbe.domain.guestBook.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mutsa.yewon.talksparkbe.domain.guestBook.dto.guestBook.GuestBookContent;
import mutsa.yewon.talksparkbe.domain.guestBook.dto.guestBook.GuestBookListRequest;
import mutsa.yewon.talksparkbe.domain.guestBook.dto.guestBook.GuestBookPostRequestDTO;
import mutsa.yewon.talksparkbe.domain.guestBook.dto.guestBook.GuestBookListResponse;
import mutsa.yewon.talksparkbe.domain.guestBook.dto.room.GuestBookRoomListResponse;
import mutsa.yewon.talksparkbe.domain.guestBook.entity.GuestBook;
import mutsa.yewon.talksparkbe.domain.guestBook.service.GuestBookRoomService;
import mutsa.yewon.talksparkbe.domain.guestBook.service.GuestBookService;
import mutsa.yewon.talksparkbe.domain.sparkUser.entity.SparkUser;
import mutsa.yewon.talksparkbe.domain.sparkUser.repository.SparkUserRepository;
import mutsa.yewon.talksparkbe.global.dto.ResponseDTO;
import mutsa.yewon.talksparkbe.global.exception.CustomTalkSparkException;
import mutsa.yewon.talksparkbe.global.exception.ErrorCode;
import mutsa.yewon.talksparkbe.global.util.JWTUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping("/guestBooks")
@RestController
@RequiredArgsConstructor
public class GuestBookController {
    private final GuestBookService guestBookService;
    private final SparkUserRepository sparkUserRepository;
    private final GuestBookRoomService guestBookRoomService;

//    @Autowired
//    private final JWTUtil jwtUtil;

    @PostMapping("/{roomId}")
    public ResponseEntity<?> PostGuestBook(@RequestHeader("Authorization") String token,
                                           @PathVariable("roomId") Long roomId,
                                           @Valid @RequestBody GuestBookContent content) {

        JWTUtil jwtUtil = new JWTUtil();
        Map<String, Object> claims = jwtUtil.validateToken(token);
        String kakaoId = (String) claims.get("kakaoId");
        SparkUser sparkUser = sparkUserRepository.findByKakaoId(kakaoId).orElseThrow(() -> new RuntimeException("유저 못찾음"));

        try {
            GuestBookPostRequestDTO guestBookPostRequestDTO = new GuestBookPostRequestDTO(roomId, sparkUser.getId(), content);
            GuestBook guestBook = guestBookService.createGuestBook(guestBookPostRequestDTO);
            ResponseDTO<?> responseDTO = ResponseDTO.created("작성되었습니다.");
            return ResponseEntity.status(201).body(responseDTO);
        } catch (IllegalArgumentException e) {
            throw new CustomTalkSparkException(ErrorCode.INVALID_FORMAT);
        }
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<?> GetGuestBookList(@RequestHeader("Authorization") String token,
                                              @PathVariable("roomId") Long roomId) {

        JWTUtil jwtUtil = new JWTUtil();
        Map<String, Object> claims = jwtUtil.validateToken(token);
        String kakaoId = (String) claims.get("kakaoId");
        SparkUser sparkUser = sparkUserRepository.findByKakaoId(kakaoId).orElseThrow(() -> new RuntimeException("유저 못찾음"));

        try {
            GuestBookListRequest guestBookListRequest = new GuestBookListRequest(roomId, sparkUser.getId());
            GuestBookListResponse guestBookListResponse = guestBookService.getGuestBookList(guestBookListRequest);
            ResponseDTO<?> responseDTO = ResponseDTO.ok("방명록 내용이 조회되었습니다.");
            return ResponseEntity.status(200).body(responseDTO);
        } catch (IllegalArgumentException e) {
            throw new CustomTalkSparkException(ErrorCode.INVALID_FORMAT);
        }
    }

    @GetMapping
    public ResponseEntity<?> GetGuestBookRoomList(@RequestHeader("Authorization") String token,
                                                  @RequestParam(required = false) String search,
                                                  @RequestParam(required = false) String sortBy) {

        JWTUtil jwtUtil = new JWTUtil();
        Map<String, Object> claims = jwtUtil.validateToken(token);
        String kakaoId = (String) claims.get("kakaoId");
        SparkUser sparkUser = sparkUserRepository.findByKakaoId(kakaoId).orElseThrow(() -> new RuntimeException("유저 못찾음"));

        try {
            GuestBookRoomListResponse guestBookRoomListResponse = guestBookRoomService.getGuestBookRoomList(search,sortBy);
            ResponseDTO<?> responseDTO = ResponseDTO.ok("방명록 방들이 조회되었습니다.");
            return ResponseEntity.status(200).body(responseDTO);
        } catch (IllegalArgumentException e) {
            throw new CustomTalkSparkException(ErrorCode.INVALID_FORMAT);
        }

    }
}
