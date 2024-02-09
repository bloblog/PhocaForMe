package com.phofor.phocaforme.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.phofor.phocaforme.auth.entity.UserDeviceEntity;
import com.phofor.phocaforme.auth.entity.UserEntity;
import com.phofor.phocaforme.auth.repository.UserDeviceRepository;
import com.phofor.phocaforme.auth.repository.UserRepository;
import com.phofor.phocaforme.chat.entity.ChatRoom;
import com.phofor.phocaforme.notification.domain.FcmMessage;
import com.phofor.phocaforme.notification.domain.FcmMessages;
import com.phofor.phocaforme.notification.dto.NotificationDto;
import com.phofor.phocaforme.notification.dto.message.NotificationMessageDto;
import com.phofor.phocaforme.notification.dto.message.RequestDTO;
import com.phofor.phocaforme.notification.entity.NotificationEntity;
import com.phofor.phocaforme.notification.entity.NotificationType;
import com.phofor.phocaforme.notification.repository.FirebaseCloudMessageRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseCloudMessageServiceImpl implements FirebaseCloudMessageService{

    private final FirebaseCloudMessageRepository firebaseCloudMessageRepository;
    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;

    @Value("${project.url}")
    String domain;

    @Value("${project.API-URL}")
    String API_URL;

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper;

    // 알림 리스트
    @Override
    public List<NotificationMessageDto> getMessageList(String userId) {
        return firebaseCloudMessageRepository.findByUserEntity_UserId(userId);
    }

    // 알림 읽음 표시
    @Override
    public Boolean readMessage(Long notificationId) {
        // 해당 알림 SELECT
        Optional<NotificationEntity> notificationEntityOptional = firebaseCloudMessageRepository.findByNotificationId(notificationId);
        if (notificationEntityOptional.isPresent()) {
            NotificationEntity notificationEntity = notificationEntityOptional.get();
            notificationEntity.setReadStatus(true);

            // 데이터베이스에 변경 내용 저장
            firebaseCloudMessageRepository.save(notificationEntity);
            return true;
        }
        return false;
    }

    // 알림 제거
    @Override
    public Boolean deleteMessage(Long notificationId) {
        // 알림 유무 확인
        Optional<NotificationEntity> notificationEntityOptional = firebaseCloudMessageRepository.findByNotificationId(notificationId);
        if (notificationEntityOptional.isPresent()) {
            NotificationEntity notificationEntity = notificationEntityOptional.get();
            notificationEntity.setDeleteStatus(true);

            // 데이터베이스에 변경 내용 저장
            firebaseCloudMessageRepository.save(notificationEntity);
            return true;
        }
        return false;
    }

    // 테스트 용
    @Override
    public Boolean sendChatMessage(NotificationDto notificationDto) {
        Optional<UserEntity> senderUserOptional, receiverUserOptional;
        if(notificationDto.getUserId().equals(notificationDto.getOwnerId())){
            senderUserOptional = userRepository.findByUserId(notificationDto.getOwnerId());
            receiverUserOptional = userRepository.findByUserId(notificationDto.getVisitedId());
        }
        else{
            senderUserOptional = userRepository.findByUserId(notificationDto.getVisitedId());
            receiverUserOptional = userRepository.findByUserId(notificationDto.getOwnerId());
        }

        // 채팅방으로 회원 정보 가져오기
//        Optional<UserEntity> userEntityOptional = userRepository.findByUserId(notificationDto.getReceiverId());
        if (senderUserOptional.isPresent() && receiverUserOptional.isPresent()) {
            UserEntity senderUserEntity = senderUserOptional.get();
            UserEntity receiverUserEntity = receiverUserOptional.get();
            log.info("senderUser_id : {}, receiverUser_id : {}", senderUserEntity.getUserId(), receiverUserEntity.getUserId());

            notificationDto.setSenderId(senderUserEntity.getNickname());
            notificationDto.setReceiverId(receiverUserEntity.getNickname());

            notificationDto.setTitle(receiverUserEntity.getNickname() + "님 채팅 도착하였습니다!");
            notificationDto.setContent(senderUserEntity.getNickname() + "으로부터 새로운 채팅이 왔어요!! 확인 해보세요!!");
            notificationDto.setLink(domain + "/chatRoom"); // 채팅함으로 이동

            NotificationEntity notificationEntity = NotificationEntity.builder()
                    .userEntity(receiverUserEntity)
                    .title(notificationDto.getTitle())
                    .content(notificationDto.getContent()) // 알림 내용
                    .readStatus(false) // 읽지 않은 상태로 초기화
                    .notificationType(NotificationType.Chatting)
                    .deleteStatus(false)
                    .build();

            // 데이터베이스에 저장
            firebaseCloudMessageRepository.save(notificationEntity);
        } else {
            log.info("User with id not found");
            return false;
        }

        // 유저의 디바이스 정보 가져오기
        Optional<UserDeviceEntity> userDeviceEntityOptional = userDeviceRepository.findByUserId(receiverUserOptional.get().getUserId());
        if (userDeviceEntityOptional.isPresent()) {
            UserDeviceEntity userDeviceEntity = userDeviceEntityOptional.get();

            try{
                // 메세지 보내기
                String message = makeMessage(
                        userDeviceEntity.getDeviceToken(), notificationDto.getTitle(),
                        notificationDto.getContent(), notificationDto.getLink()
                );
                sendMessage(message);
                return true;
            } catch (Exception e){
                log.info("request error");
                return false;
            }
        }
        else {
            log.info("UserDevice with id {} not found", notificationDto.getReceiverId());
            return false;
        }
    }


    // 메세지 등록 후 알림 등록
    @Override
    public Boolean sendChatMessage(NotificationDto notificationDto, ChatRoom chatRoom, String userId) {
        //전달받은 userId를 chatRoom에 있는 ownerId, visiterId를 비교하여 같은게 작성자, 다른게 수신자
        Optional<UserEntity> senderUserOptional, receiverUserOptional;
        if(userId.equals(chatRoom.getOwnerId())){
            senderUserOptional = userRepository.findByUserId(chatRoom.getOwnerId());
            receiverUserOptional = userRepository.findByUserId(chatRoom.getVisiterId());
        }
        else{
            senderUserOptional = userRepository.findByUserId(chatRoom.getVisiterId());
            receiverUserOptional = userRepository.findByUserId(chatRoom.getOwnerId());
        }

        // 채팅방으로 회원 정보 가져오기
//        Optional<UserEntity> userEntityOptional = userRepository.findByUserId(notificationDto.getReceiverId());
        if (senderUserOptional.isPresent() && receiverUserOptional.isPresent()) {
            UserEntity senderUserEntity = senderUserOptional.get();
            UserEntity receiverUserEntity = receiverUserOptional.get();
            log.info("senderUser_id : {}, receiverUser_id : {}", senderUserEntity.getUserId(), receiverUserEntity.getUserId());

            notificationDto.setSenderId(senderUserEntity.getNickname());
            notificationDto.setReceiverId(receiverUserEntity.getNickname());

            notificationDto.setTitle(receiverUserEntity.getNickname() + "님 채팅 도착하였습니다!");
            notificationDto.setContent(senderUserEntity.getNickname() + "으로부터 새로운 채팅이 왔어요!! 확인 해보세요!!");
            notificationDto.setLink(domain + "/chatRoom"); // 채팅함으로 이동

            NotificationEntity notificationEntity = NotificationEntity.builder()
                    .userEntity(receiverUserEntity)
                    .title(notificationDto.getTitle())
                    .content(notificationDto.getContent()) // 알림 내용
                    .readStatus(false) // 읽지 않은 상태로 초기화
                    .notificationType(NotificationType.Chatting)
                    .deleteStatus(false)
                    .build();

            // 데이터베이스에 저장
            firebaseCloudMessageRepository.save(notificationEntity);
        } else {
            log.info("User with id not found");
            return false;
        }

        // 유저의 디바이스 정보 가져오기
        Optional<UserDeviceEntity> userDeviceEntityOptional = userDeviceRepository.findByUserId(receiverUserOptional.get().getUserId());
        if (userDeviceEntityOptional.isPresent()) {
            UserDeviceEntity userDeviceEntity = userDeviceEntityOptional.get();

            try{
                // 메세지 보내기
                String message = makeMessage(
                        userDeviceEntity.getDeviceToken(), notificationDto.getTitle(),
                        notificationDto.getContent(), notificationDto.getLink()
                );
                sendMessage(message);
                return true;
            } catch (Exception e){
                log.info("request error");
                return false;
            }
        }
        else {
            log.info("UserDevice with id {} not found", notificationDto.getReceiverId());
            return false;
        }
    }

    // 갈망포카 메세지 전송
    @Override
    public Boolean sendBiasMessage(List<String> ids, Long articleId) {

        String title = "갈망포카 출현!";
        String content = "새로운 갈망포카가 올라 왔어요!! 확인해보세요!!";
        String link = domain + "/barter/" + articleId;

        List<String> deviceTokens = new ArrayList<>();
        List<String> sendFail = new ArrayList<>();
        List<Integer> requestError = new ArrayList<>();

        int total = ids.size();
        for (int i = 0; i < ids.size(); i++){
            // 유저 정보 가져오기
            Optional<UserEntity> userEntityOptional = userRepository.findByUserId(ids.get(i));
            if (userEntityOptional.isPresent()) {
                UserEntity userEntity = userEntityOptional.get();
                log.info("user_id : {}", userEntity.getUserId());

                NotificationEntity notificationEntity = NotificationEntity.builder()
                        .userEntity(userEntity)
                        .title(title)
                        .content(content) // 알림 내용
                        .readStatus(false) // 읽지 않은 상태로 초기화
                        .notificationType(NotificationType.Article) // 알림 유형 설정, 실제 유형으로 교체 필요
                        .articleId(articleId) // 관련 글 ID, 필요한 경우 설정
                        .deleteStatus(false)
                        .build();
                // 데이터베이스에 저장
                firebaseCloudMessageRepository.save(notificationEntity);
            } else {
                log.info("User with id {} not found", ids.get(i));
                sendFail.add(ids.get(i));
                continue;
            }

            // 유저들의 디바이스 정보 가져오기
            deviceTokens = findDeviceTokensByIds(ids);
        }

        if(!deviceTokens.isEmpty()) {
            // 500개 단위로 나누기 - 한번에 처리할 수 있는 메세지 개수는 500개
            int fullBatches = total / 500;
            int remaining = total % 500;

            for (int i = 0; i <= fullBatches; i++) {
                int start = i * 500;
                int end = Math.min((i + 1) * 500, total);

                List<String> batchTokens = deviceTokens.subList(start, end);
                try {
                    // 메세지 보내기
                    String message = makeMulticastMessage(batchTokens, title, content, link);
                    sendMessage(message);
                } catch (Exception e) {
                    log.info("request error");
                    requestError.add(i);
                }
            }
            // 실패한 요청에 대한 재요청
            fullBatches = requestError.size() / 500;
            for(int i = 0; i <= fullBatches; i++) {
                int start = i * 500;
                int end = Math.min((i + 1) * 500, total);

                List<String> batchTokens = deviceTokens.subList(start, end);
                try {
                    // 메세지 보내기
                    String message = makeMulticastMessage(batchTokens, title, content, link);
                    sendMessage(message);
                } catch (Exception e) {
                    log.info("request error");
                }
            }
            return true;
        }
        return false;
    }

    public List<String> findDeviceTokensByIds(List<String> ids) {
        TypedQuery<String> query = entityManager.createQuery(
                "SELECT ud.deviceToken FROM UserDeviceEntity ud WHERE ud.userId IN :ids", String.class);
        query.setParameter("ids", ids);
        return query.getResultList();
    }

    // 메세지 보내기 테스트용
    @Override
    public void sendMessageTo(RequestDTO requestDTO) throws IOException {

        String message = makeMessage(
                requestDTO.getTargetToken(), requestDTO.getTitle(),
                requestDTO.getBody(), requestDTO.getLink()
        );

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(message,
                MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .addHeader(HttpHeaders.CONTENT_TYPE, "application/json; UTF-8")
                .build();

        Response response = client.newCall(request).execute();

        System.out.println(response.body().string());
    }

    private String makeMessage(String targetToken, String title, String body, String link) throws JsonProcessingException {
        FcmMessage fcmMessage = FcmMessage.builder()
                .message(FcmMessage.Message.builder()
                        .token(targetToken)
                        .data(Map.of(
                                "title", title,
                                "body", body,
                                "link", link
                                // "image", "이미지_URL" // 이미지 URL을 추가할 경우
                        ))
                        .build())
                .validateOnly(false)
                .build();
        return objectMapper.writeValueAsString(fcmMessage);
    }

    private String makeMulticastMessage(List<String> targetTokens, String title, String body, String link) throws JsonProcessingException {
        Map<String, Object> message = new HashMap<>();
        message.put("registration_ids", targetTokens);
        message.put("data", Map.of(
                "title", title,
                "body", body,
                "link", link
        )); // 데이터 메시지의 경우
        return objectMapper.writeValueAsString(message);
    }

    private void sendMessage(String message) throws Exception{
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(message,
                MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .addHeader(HttpHeaders.CONTENT_TYPE, "application/json; UTF-8")
                .build();
        Response response = client.newCall(request).execute();
        System.out.println(response.body().string());
    }
    private String getAccessToken() throws IOException {
        String firebaseConfigPath = "firebase/firebase_service_key.json";

        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(new ClassPathResource(firebaseConfigPath).getInputStream())
                .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));

        googleCredentials.refreshIfExpired();
        return googleCredentials.getAccessToken().getTokenValue();
    }
}