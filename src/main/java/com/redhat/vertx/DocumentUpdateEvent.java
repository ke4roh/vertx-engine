package com.redhat.vertx;

public class DocumentUpdateEvent {
    public static final DocumentUpdateEventCodec CODEC = new DocumentUpdateEventCodec();

    private String uuid;
    private String name;
    private Object value;

    public DocumentUpdateEvent(String uuid, String name, Object value) {
        this.uuid = uuid;
        this.name = name;
        this.value = value;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

}
