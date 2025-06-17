package org.example.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import lombok.extern.slf4j.Slf4j;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/** general chat controller */
@RestController
@RequestMapping("/ai/v3/chat/")
@Slf4j
public class GeneralChatController {

    private final ChatClient jdbcChatClient;
    private final ChatClient redisChatClient;

    public GeneralChatController(
            @Qualifier("deepseekV3ClientWithJdbc") ChatClient jdbcChatClient,
            @Qualifier("deepseekV3ClientWithRedis") ChatClient redisChatClient) {
        this.jdbcChatClient = jdbcChatClient;
        this.redisChatClient = redisChatClient;
    }

    @PostMapping(
            value = "/t1",
            produces = {MediaType.TEXT_EVENT_STREAM_VALUE})
    @ResponseBody
    public Object chatWithJdbc(@RequestBody String body) {
        JSONObject entries = JSONUtil.parseObj(body);
        String text = entries.getStr("text");
        Boolean stream = entries.getBool("stream", false);
        String conversationId = entries.getStr("conversationId");

        log.info("开始对话聊天，会话ID：{}", conversationId);
        var request =
                jdbcChatClient
                        .prompt()
                        .system("你是乐观小王，回答问题简练精要。")
                        .advisors(advisor -> advisor.param(CONVERSATION_ID, conversationId))
                        .advisors(new SimpleLoggerAdvisor())
                        .user(text);

        try {
            log.info("开始生成回答，是否流式输出：{}", stream);
            return stream ? request.stream().content() : request.call().content();
        } catch (Exception e) {
            log.error("对话聊天发生异常，会话ID：{}", conversationId, e);
            throw e;
        }
    }

    @PostMapping(
            value = "/t2",
            produces = {MediaType.TEXT_EVENT_STREAM_VALUE})
    @ResponseBody
    public Object chatWithRedis(@RequestBody String body) {
        JSONObject entries = JSONUtil.parseObj(body);
        String text = entries.getStr("text");
        Boolean stream = entries.getBool("stream", false);
        String conversationId = entries.getStr("conversationId");

        log.info("开始对话聊天，会话ID：{}", conversationId);
        var request =
                redisChatClient
                        .prompt()
                        .system("你是乐观小王，回答问题简练精要。")
                        .advisors(advisor -> advisor.param(CONVERSATION_ID, conversationId))
                        .advisors(new SimpleLoggerAdvisor())
                        .user(text);

        try {
            log.info("开始生成回答，是否流式输出：{}", stream);
            return stream ? request.stream().content() : request.call().content();
        } catch (Exception e) {
            log.error("对话聊天发生异常，会话ID：{}", conversationId, e);
            throw e;
        }
    }
}
