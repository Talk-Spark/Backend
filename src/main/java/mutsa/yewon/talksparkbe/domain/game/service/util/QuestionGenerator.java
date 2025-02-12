package mutsa.yewon.talksparkbe.domain.game.service.util;

import lombok.extern.log4j.Log4j2;
import mutsa.yewon.talksparkbe.domain.card.entity.Card;
import mutsa.yewon.talksparkbe.domain.game.entity.DummyOption;
import mutsa.yewon.talksparkbe.domain.game.service.dto.CardQuestion;
import mutsa.yewon.talksparkbe.domain.game.service.dto.UserCardQuestions;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Log4j2
public class QuestionGenerator {

    public List<UserCardQuestions> execute(List<Card> cards, int difficulty) {
        int maxNumOfQuestions = 0;
        if (difficulty <= 2) maxNumOfQuestions = difficulty + 1;
        else if (difficulty == 3) maxNumOfQuestions = 5;
        List<UserCardQuestions> userCardQuestionsList = new ArrayList<>();

        // 빈칸 필드를 생성할 키 리스트 (명함의 필드명)
        List<DummyOption> keys = List.of(DummyOption.MBTI, DummyOption.HOBBY, DummyOption.LOOKALIKE,
                DummyOption.TMI, DummyOption.SELFDESCRIPTION);

        Map<String, Set<String>> listOfOptions = new HashMap<>();

        Map<String, Integer> fieldCount = new HashMap<>();
        int numOfPeople = cards.size();

        // 각 필드마다, 이 필드를 채워놓은 놈이 사람수만큼은 되어야 문제로 출제 가능
        for (DummyOption k : keys) {

            Set<String> options = new HashSet<>(); // 보기로 만들 후보들

            for (Card c : cards){

                log.info("현재 명함" + c);

                String fieldValue = getFieldValue(c, k.getOption()); // 각 명함별로 해당 필드의 항목 추출

                log.info("명함 항목" + fieldValue);

                if (fieldValue != null){
                    options.add(fieldValue);
                }
            }

            log.info("현재 보기 들" + options);

            if(numOfPeople < 4){
                if(k.equals(DummyOption.MBTI)){
                    for(String option : DummyOption.MBTI.getDummyOptions()){
                        options.add(option);
                        if(options.size() == 4){
                            break;
                        }
                    }
                }else{
                    for(int i = 0; i < 4 - numOfPeople; i++){
                        options.add(k.getDummyOptions().get(i));
                    }
                }
            }
            listOfOptions.put(k.getOption(), options);
            fieldCount.put(k.getOption(), options.size()); // 각 필드를 채워놓은 놈의 숫자 맵

            log.info("각 명함 항목들의 데이터로 보기를 만들어냅니다. " + listOfOptions);
            log.info("각 항목 당 답을 한 인원 수 " + fieldCount);

        }

        for (Card c : cards) { // 각 명함별로
            List<String> fields = new ArrayList<>(List.of("mbti", "hobby", "lookAlike", "selfDescription", "tmi"));
            List<CardQuestion> questions = new ArrayList<>(); // 문제들을 담을 리스트

            if(numOfPeople <= 4){
                fields.removeIf(field -> getFieldValue(c, field) == null || fieldCount.get(field) < numOfPeople); // 인원수가 4명이하인 경우, 답안을 제출한 인원 수가 현재 인원 수보다 작으면 문제 출제 불가
            }
            else{
                fields.removeIf(field -> getFieldValue(c, field) == null || fieldCount.get(field) < 4);
            }

            Collections.shuffle(fields);

            int numOfQuestions = Math.min(maxNumOfQuestions, fields.size()); // 출제가능한 문제 수는 문제 항목의 수!

            for (int i=0;i<numOfQuestions;i++) {
                questions.add(generateQuestion(c, fields.get(i), listOfOptions.get(fields.get(i)), numOfPeople));
            }

            userCardQuestionsList.add(new UserCardQuestions(c.getSparkUser().getId(), questions)); // 어떤 사람의 카드에 대한 질문들 생성 완료
        }

        userCardQuestionsList.sort(Comparator.comparing(UserCardQuestions::getSparkUserId)); // 유저아이디 오름차순. 그래서 같은 유저아이디의 문제가 쫙 나옴.

        log.info(userCardQuestionsList);

        return userCardQuestionsList;
    }

    private CardQuestion generateQuestion(Card c, String fieldName, Set<String> cardData, int numOfPeople) {
        String correctAnswer = getFieldValue(c, fieldName);
        List<String> options = generateOptions(correctAnswer, cardData, numOfPeople);
        return new CardQuestion(c.getId(), c.getSparkUser().getId(), fieldName, correctAnswer, options);
    }

    // 카드의 특정 필드 값을 반환 (Reflection 활용)
    private String getFieldValue(Card card, String fieldName) {
        try {
            return switch (fieldName) {
                case "name" -> card.getName();
                case "age" -> card.getAge() != null ? card.getAge().toString() : null;
                case "mbti" -> card.getMbti();
                case "hobby" -> card.getHobby();
                case "lookAlike" -> card.getLookAlike();
                case "selfDescription" -> card.getSlogan();
                case "tmi" -> card.getTmi();
                default -> null;
            };
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 보기 생성: 다른 참가자의 해당 필드 값 + 정답
    private List<String> generateOptions(String correctAnswer, Set<String> cardData, int numOfPeople) {
        int maxOptionNum = Math.min(4, numOfPeople);

        if(maxOptionNum < 4){
            return new ArrayList<>(cardData);
        }

        List<String> canBeOptions = new ArrayList<>(cardData);

        canBeOptions.remove(correctAnswer);

        List<String> options = new ArrayList<>(getOptionsByCombi(canBeOptions));

        options.add(correctAnswer);
        Collections.shuffle(options); // 보기 순서 섞기

        return options;
    }

    private Set<String> getOptionsByCombi(List<String> candidates){

        Set<String> options = new HashSet<>();

        Random random = new Random();

        while(options.size() < 3){
            int index = random.nextInt(candidates.size());

            options.add(candidates.get(index));
        }

        return options;
    }



}
