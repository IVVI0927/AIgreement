package com.example.legalai.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
public class CollaborationController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/collaborate/edit")
    @SendTo("/topic/contract-edits")
    public Map<String, Object> handleContractEdit(@Payload Map<String, Object> edit) {
        edit.put("timestamp", LocalDateTime.now().toString());
        return edit;
    }

    @MessageMapping("/collaborate/comment")
    @SendTo("/topic/contract-comments")
    public Map<String, Object> handleContractComment(@Payload Map<String, Object> comment) {
        comment.put("timestamp", LocalDateTime.now().toString());
        return comment;
    }

    @MessageMapping("/collaborate/cursor")
    @SendTo("/topic/cursor-positions")
    public Map<String, Object> handleCursorPosition(@Payload Map<String, Object> cursor) {
        return cursor;
    }

    public void notifyUsersOfChange(String contractId, String changeType, Object data) {
        Map<String, Object> notification = Map.of(
            "contractId", contractId,
            "changeType", changeType,
            "data", data,
            "timestamp", LocalDateTime.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/contract-updates/" + contractId, notification);
    }
}