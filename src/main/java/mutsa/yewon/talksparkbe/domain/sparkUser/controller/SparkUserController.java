package mutsa.yewon.talksparkbe.domain.sparkUser.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import mutsa.yewon.talksparkbe.domain.sparkUser.SparkUserControllerDocs;
import mutsa.yewon.talksparkbe.domain.sparkUser.dto.SparkUserDTO;
import mutsa.yewon.talksparkbe.domain.sparkUser.service.RefreshTokenService;
import mutsa.yewon.talksparkbe.domain.sparkUser.service.SparkUserService;
import mutsa.yewon.talksparkbe.global.dto.ResponseDTO;
import mutsa.yewon.talksparkbe.global.util.JWTUtil;
import mutsa.yewon.talksparkbe.global.util.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Log4j2
public class SparkUserController implements SparkUserControllerDocs {

    private final SparkUserService sparkUserService;

    private final JWTUtil jwtUtil;


    @PostMapping("/api/member/kakao")
    public Map<String, Object> login(@RequestParam("accessToken") String accessToken) {

        log.info(accessToken);

        SparkUserDTO kakaoUser = sparkUserService.getKakaoUser(accessToken);

        Map<String, Object> claims = kakaoUser.getClaims();

        String JWTAccessToken = jwtUtil.generateToken(claims, 60 * 12);
        String JWTRefreshToken = jwtUtil.generateToken(claims, 60 * 24);

        claims.put("accessToken", JWTAccessToken);
        claims.put("refreshToken", JWTRefreshToken);

        return claims;
    }

    @GetMapping("/api/member/refresh")
    public Map<String, String> refresh(String refreshToken) {
        Map<String, Object> claims = jwtUtil.validateToken(refreshToken);
//
        String accessToken = jwtUtil.generateToken(claims, 60);
//
//        Map<String, String> newJwtToken =
//                refreshTokenService.getNewRefreshToken((Long) claims.get("sparkUserId"), refreshToken);
//
//        return newJwtToken;

        return Map.of("accessToken", accessToken);
    }

    @DeleteMapping("/api/member/leave")
    public ResponseEntity<?> accountDelete(String accessToken) {
        // jwtfilter를 거치지 않기 때문에 등록 x

        Map<String, Object> claims = jwtUtil.validateToken(accessToken);

        Number sparkUserIdNumber = (Number) claims.get("sparkUserId");
        Long sparkUserId = (sparkUserIdNumber != null) ? sparkUserIdNumber.longValue() : null;

        Long deletedUserId = sparkUserService.deleteAccount(sparkUserId);

        return ResponseEntity.ok().body(ResponseDTO.ok("회원정보가 삭제되었습니다.", deletedUserId));
    }
}
