package com.redhat.vertx;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.Json;

public class DocumentUpdateEventCodec implements MessageCodec<DocumentUpdateEvent, DocumentUpdateEvent> {
    @Override
    public void encodeToWire(Buffer buffer, DocumentUpdateEvent event) {
        String json = Json.encode(event);
        buffer.appendInt(json.length());
        buffer.appendString(json);
    }

    @Override
    public DocumentUpdateEvent decodeFromWire(int pos, Buffer buffer) {
        int length = buffer.getInt(pos);
        pos += 4;

        return Json.decodeValue(buffer.slice(pos, pos + length), DocumentUpdateEvent.class);
    }

    @Override
    public DocumentUpdateEvent transform(DocumentUpdateEvent due) {
        return new DocumentUpdateEvent(due.getUuid(), due.getName(), due.getValue());
    }

    @Override
    public String name() {
        return "DocumentUpdateEvent";
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}
