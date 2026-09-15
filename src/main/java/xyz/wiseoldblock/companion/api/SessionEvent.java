package xyz.wiseoldblock.companion.api;

public enum SessionEvent {
    CONNECT("connect"),
    DISCONNECT("disconnect");

    private final String eventName;

    SessionEvent(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }
}