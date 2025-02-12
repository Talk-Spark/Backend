package mutsa.yewon.talksparkbe.domain.sparkUser.service;

import lombok.RequiredArgsConstructor;
import mutsa.yewon.talksparkbe.global.exception.CustomTalkSparkException;
import mutsa.yewon.talksparkbe.global.exception.ErrorCode;
import mutsa.yewon.talksparkbe.global.util.JWTUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    private final JWTUtil jwtUtil;

    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60;

    public void saveRefreshToken(String refreshToken, Long userId) {
        String key = "refresh:user" + userId;
        redisTemplate.opsForValue().set(key, refreshToken);
        redisTemplate.expire(key, REFRESH_TOKEN_EXPIRATION, TimeUnit.SECONDS);
    }
    
    public Map<String, String> getNewRefreshToken(Long userId, String refreshToken) {
        String key = "refresh:user" + userId;

        Map<String, Object> userInfo = jwtUtil.validateToken(refreshToken);

        if(!validateRefreshToken(refreshToken, key)){
            throw new CustomTalkSparkException(ErrorCode.JWT_TOKEN_EXPIRED);
        }

        String newAccessToken = jwtUtil.generateToken(userInfo, 60 * 12);
        String newRefreshToken = jwtUtil.generateToken(userInfo, 60 * 24);

        redisTemplate.delete(key);

        saveRefreshToken(newRefreshToken, userId);

        return Map.of("accessToken", newAccessToken, "refreshToken", newRefreshToken);
    }

    private boolean validateRefreshToken(String refreshToken, String key) {
        String originalToken = redisTemplate.opsForValue().get(key);
        if (originalToken == null || !originalToken.equals(refreshToken)) {
            return false;
        }
        return true;
    }
}
