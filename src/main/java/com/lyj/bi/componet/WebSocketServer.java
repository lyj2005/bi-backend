package com.lyj.bi.componet;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
/**
* webSocket服务端
 */
@Component
@Slf4j
@ServerEndpoint("/websocket/{userId}")
public class WebSocketServer {
    /**
     * 线程安全的无序的集合
     */
    private static final CopyOnWriteArraySet<Session> SESSIONS = new CopyOnWriteArraySet<>();

    /**
     * 存储在线连接数
     */
    private static final Map<String, Session> SESSION_POOL = new HashMap<>();

    /**
     * 连接建立成功调用方法
     * @param session
     * @param userId
     */
    @OnOpen
    public void onOpen(Session session, @PathParam(value = "userId") String userId) {
        try {
            // 将新的连接添加到SESSIONS集合中
            SESSIONS.add(session);
            // 将新的连接添加到SESSION_POOL集合中，以userId为键
            SESSION_POOL.put(userId, session);
            // 记录日志，显示当前连接总数
            log.info("【WebSocket消息】建立一个新的连接，userId为：" + userId + "，当前连接总数为：" + SESSIONS.size());
        } catch (Exception e) {
            // 捕获异常并打印堆栈信息
            e.printStackTrace();
        }
    }

    /**
     * 连接关闭调用方法
     * @param session
     */
    @OnClose
    public void onClose(Session session) {
        try {
            SESSIONS.remove(session);
            log.info("【WebSocket消息】连接断开，总数为：" + SESSIONS.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 收到客户端消息后调用方法
     * @param userId
     * @param message
     */
    @OnMessage
    public void onMessage(@PathParam("userId") String userId,String message) {
        log.info("【WebSocket消息】收到客户端"+userId+"消息：" + message);
    }

    /**
     * 此为广播消息
     *
     * @param message 消息
     */
    public void sendAllMessage(String message) {
        // 打印日志：广播消息
        log.info("【WebSocket消息】广播消息：" + message);
        // 遍历所有Session
        for (Session session : SESSIONS) {
            try {
                // 如果Session打开
                if (session.isOpen()) {
                    // 异步发送文本消息
                    session.getAsyncRemote().sendText(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 此为单点消息
     *
     * @param userId  用户编号
     * @param message 消息
     */
    public void sendOneMessage(long userId, String message) {
        // 从SESSION_POOL中获取指定userId的Session
        Session session = SESSION_POOL.get(userId);
        // 如果Session不为空且处于打开状态
        if (session != null && session.isOpen()) {
            try {
                // 同步Session
                synchronized (session) {
                    // 打印日志
                    log.info("【WebSocket消息】单点消息：" + message);
                    // 使用异步的方式发送消息
                    session.getAsyncRemote().sendText(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 此为单点消息(多人)
     *
     * @param userIds 用户编号列表
     * @param message 消息
     */
    public void sendMoreMessage(String[] userIds, String message) {
        for (String userId : userIds) {
            Session session = SESSION_POOL.get(userId);
            if (session != null && session.isOpen()) {
                try {
                    log.info("【WebSocket消息】单点消息：" + message);
                    session.getAsyncRemote().sendText(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}