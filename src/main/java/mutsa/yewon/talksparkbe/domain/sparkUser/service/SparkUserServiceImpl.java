package mutsa.yewon.talksparkbe.domain.sparkUser.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import mutsa.yewon.talksparkbe.domain.sparkUser.dto.SparkUserDTO;
import mutsa.yewon.talksparkbe.domain.sparkUser.entity.SparkUser;
import mutsa.yewon.talksparkbe.domain.sparkUser.entity.SparkUserRole;
import mutsa.yewon.talksparkbe.domain.sparkUser.repository.SparkUserRepository;
import mutsa.yewon.talksparkbe.global.exception.CustomTalkSparkException;
import mutsa.yewon.talksparkbe.global.exception.ErrorCode;
import mutsa.yewon.talksparkbe.global.util.SecurityUtil;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@RequiredArgsConstructor
@Log4j2
@Service
public class SparkUserServiceImpl implements SparkUserService {

    private final SparkUserRepository sparkUserRepository;

    private final PasswordEncoder passwordEncoder;
    private final SecurityUtil securityUtil;

    @Override
    public SparkUserDTO getKakaoUser(String accessToken) {

        log.info(accessToken);

        Map<String, String> kakaoData = getKakaoUserNickname(accessToken);

        String kakaoId = kakaoData.get("kakaoId");
        String name = kakaoData.get("name");

        Optional<SparkUser> sparkUser = sparkUserRepository.findByKakaoId(kakaoId);

        if(sparkUser.isPresent()) {
            SparkUserDTO sparkUserDTO = SparkUserDTO.from(sparkUser.get());
            return sparkUserDTO;
        }

        SparkUser createdUser = makeSparkUser(kakaoId,name);

        sparkUserRepository.save(createdUser);

        SparkUserDTO sparkUserDTO = SparkUserDTO.from(createdUser);

        return sparkUserDTO;
    }

    private Map<String,String> getKakaoUserNickname(String accessToken) {
        String kakaoGetUserURL = "https://kapi.kakao.com/v2/user/me";

        log.info(accessToken);

        WebClient webClient = WebClient.builder().build();

        LinkedHashMap response = webClient.get()
                .uri(kakaoGetUserURL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> Mono.error(new CustomTalkSparkException(ErrorCode.INVALID_PARAMETER)))
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(new CustomTalkSparkException(ErrorCode.INTERNAL_SERVER_ERROR)))
                .bodyToMono(LinkedHashMap.class)
                .block();


        String kakaoId = String.valueOf(response.get("id"));

        LinkedHashMap<String, String> properties = (LinkedHashMap<String, String>) response.get("properties");


        String name = properties.get("nickname");


        return Map.of("kakaoId", kakaoId, "name", name);
    }

    private SparkUser makeSparkUser(String kakaoId, String name) {
        String tempPassword = makeTempPassword();

        SparkUser sparkUser = SparkUser.builder()
                .kakaoId(kakaoId)
                .name(name)
                .password(passwordEncoder.encode(tempPassword)).build();

        sparkUser.addMemberRole(SparkUserRole.USER);

        return sparkUser;
    }

    private String makeTempPassword() {
        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < 10; i++) {
            buffer.append((char)((int)(Math.random() * 55) + 65));
        }

        return buffer.toString();
    }

    @Override
    @Transactional
    public SparkUserDTO generateTestUser(String name) {
        Optional<SparkUser> sparkUser = sparkUserRepository.findByName(name);
        if(sparkUser.isPresent()) {
            return SparkUserDTO.from(sparkUser.get());
        }

        Random random = new Random();
        int randomNumber = random.nextInt(901) + 100; // 901은 (1000 - 100 + 1)
        SparkUser createdUser = makeSparkUser(String.valueOf(randomNumber), name);
        sparkUserRepository.save(createdUser);
        return SparkUserDTO.from(createdUser);
    }

    @Override
    @Transactional
    public Long deleteAccount(Long sparkUserId) {

        sparkUserRepository.findById(sparkUserId)
                        .orElseThrow(()-> new CustomTalkSparkException(ErrorCode.USER_NOT_EXIST));

        sparkUserRepository.deleteById(sparkUserId);

        return sparkUserId;
    }

}
