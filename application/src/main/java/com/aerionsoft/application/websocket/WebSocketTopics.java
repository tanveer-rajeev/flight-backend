package com.aerionsoft.application.websocket;

public final class WebSocketTopics {

    public static final String ENDPOINT = "/ws";

    /** Alias when frontends use {@code apiBase + "/ws"} (apiBase already ends with {@code /api}). */
    public static final String ENDPOINT_API_ALIAS = "/api/ws";

    /** Client app: send heartbeats to stay online */
    public static final String APP_PRESENCE_HEARTBEAT = "/presence/heartbeat";

    /** Per-session acks (subscribe as /user/queue/presence) */
    public static final String QUEUE_PRESENCE = "/queue/presence";

    /** Admin dashboard: live active-users list (same payload as GET /api/admin/summery/active-users) */
    public static final String TOPIC_ADMIN_ACTIVE_USERS = "/topic/admin/active-users";

    /** Admin dashboard: live ops activity feed (BOOKING, WALLET, ADMIN, TICKET_ACTION) */
    public static final String TOPIC_ADMIN_ACTIVITY_FEED = "/topic/admin/activity-feed";

    /** Admin: request immediate broadcast to all subscribers */
    public static final String APP_ADMIN_ACTIVE_USERS_REFRESH = "/admin/active-users/refresh";

    public static final String PERMISSION_VIEW_SUMMERY = "view-summery";
    public static final String PERMISSION_VIEW_ACTIVITY_LOG = "view-activity-log";

    /** Live chat: client → server send message */
    public static final String APP_CHAT_SEND = "/chat/send";

    /** Live chat: client → server typing indicator */
    public static final String APP_CHAT_TYPING = "/chat/typing";

    /** Live chat: client → server mark read */
    public static final String APP_CHAT_READ = "/chat/read";

    /** Per-user live chat events (subscribe as /user/queue/chat) */
    public static final String QUEUE_CHAT = "/queue/chat";

    /** Shared admin inbox feed */
    public static final String TOPIC_ADMIN_CHAT_INBOX = "/topic/admin/chat/inbox";

    public static final String PERMISSION_MANAGE_LIVE_CHAT = "manage-live-chat";

    public static final String IP_ATTRIBUTE = "WS_IP";
    public static final String USER_AGENT_ATTRIBUTE = "WS_USER_AGENT";

    private WebSocketTopics() {
    }
}
