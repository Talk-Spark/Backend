package mutsa.yewon.talksparkbe.domain.sparkUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import mutsa.yewon.talksparkbe.domain.sparkUser.dto.SparkUserDTO;
import mutsa.yewon.talksparkbe.global.dto.ResponseDTO;
import mutsa.yewon.talksparkbe.global.exception.ErrorCode;
import mutsa.yewon.talksparkbe.global.swagger.ApiErrorCodes;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@Tag(name = "사용자 API", description = "사용자 로그인/회원가입 및 RefreshToken을 통한 AccessToken 재발급을 위한 API")
public interface SparkUserControllerDocs {

    @Operation(summary = "로그인 또는 회원가입", description = "서비스 이용자인지 판단 후 로그인/회원가입을 진행하기 위한 API")
    @ApiErrorCodes({ErrorCode.INVALID_JWT_TOKEN, ErrorCode.INVALID_PARAMETER, ErrorCode.INTERNAL_SERVER_ERROR})
    @ApiResponse(responseCode = "201", description = "유저 로그인/회원가입 성공",
    content = @Content(mediaType = "application/json", schema = @Schema(type = "Map", example = "{\n" +
            "  \"accessToken\": \"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...\",\n" +
            "  \"refreshToken\": \"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...\",\n" +
            "  \"kakaoId\": \"3776885192\",\n" +
            "  \"name\": \"박승범\",\n" +
            "  \"password\": \"string\",\n" +
            "  \"roleNames\": [\"USER\"],\n" +
            "  \"sparkUserId\": 1\n" +
            "}")))
    Map<String, Object> login(String accessToken);

    @Operation(summary = "AccessToken 재발급", description = "RefreshToken을 이용한 AccessToken 재발급 API")
    @ApiErrorCodes({ErrorCode.TOKEN_REQUIRED, ErrorCode.JWT_TOKEN_EXPIRED, ErrorCode.INVALID_JWT_TOKEN})
    @ApiResponse(responseCode = "200", description = "토큰 재발급 성공",
            content = @Content(mediaType = "application/json", schema = @Schema(type = "Map", example = """
                    {
                        "accessToken" : "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJwYXNzd29yZCI6IiQyYSQxMCRiZlhBbzhhN2FJQjNYa1p1QndFMDJlRlJway9sTk5uNkNMOEtHc3VIVC5iUThWcUxpYS9xYSIsIm5hbWUiOiLrsJXsirnrspQiLCJrYWthb0lkIjoiMzc3Njg4NTE5MiIsInJvbGVOYW1lcyI6WyJVU0VSIl0sInNwYXJrVXNlcklkIjoxLCJpYXQiOjE3MzE2NDg1OTcsImV4cCI6MTczMTY0OTE5N30.Uq1Cu7IAuCt6o61HNheIFRWV9XRqpA8unNNY2ySORC4", 
                        "refreshToken" : "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJwYXNzd29yZCI6IiQyYSQxMCRiZlhBbzhhN2FJQjNYa1p1QndFMDJlRlJway9sTk5uNkNMOEtHc3VIVC5iUThWcUxpYS9xYSIsIm5hbWUiOiLrsJXsirnrspQiLCJrYWthb0lkIjoiMzc3Njg4NTE5MiIsInJvbGVOYW1lcyI6WyJVU0VSIl0sInNwYXJrVXNlcklkIjoxLCJpYXQiOjE3MzE2NDg1OTcsImV4cCI6MTczMTczNDk5N30.e8psA40ogHbewqqkrtrkRRYoLAR_0XC51y8Z6uyhNY0"
                    }
                    """)))

    Map<String, String> refresh(String refreshToken);

    @Operation(summary = "회원 탈퇴", description = "AccessToken을 이용해 회원 특정 후 회원 정보를 삭제하는 API")
    @ApiErrorCodes({ErrorCode.JWT_TOKEN_EXPIRED, ErrorCode.TOKEN_REQUIRED, ErrorCode.INVALID_JWT_TOKEN})
    @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공",
    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDTO.class),
    examples = {
            @ExampleObject(
                    value = """
                            {
                                "status": 200,
                                "message": "회원정보가 삭제되었습니다.",
                                "data": 1
                            }
                            """
            )
    }))
    ResponseEntity<?> accountDelete(String accessToken);
}
