package common;

public final class Protocol {
    private Protocol() {}

    public static final int DEFAULT_TCP_PORT = 5050;
    public static final int DEFAULT_SERVER_UDP_PORT = 5051;

    public static final String STATUS_OK = "ok";
    public static final String STATUS_ERROR = "error";
    public static final String STATUS_UNAUTHORIZED = "unauthorized";

    public static final String NOTIF_FUNDRAISER_CREATED = "notif_fundraiser_created";
    public static final String NOTIF_DONATION = "notif_donation";
    public static final String NOTIF_FUNDRAISER_COMPLETED = "notif_fundraiser_completed";
    public static final String NOTIF_JOIN_REQUEST = "notif_join_request";
    public static final String NOTIF_JOIN_RESULT = "notif_join_result";
    public static final String NOTIF_MEMBER_REMOVED = "notif_member_removed";
}
