package org.asamk.signal.json;

import org.whispersystems.signalservice.api.messages.SignalServiceTypingMessage;

public class JsonTypingMessage {

    String action;
    long timestamp;
    byte[] group;

    public JsonTypingMessage(SignalServiceTypingMessage message) {
        this.action = message.getAction().toString();
        this.timestamp = message.getTimestamp();
        this.group = message.getGroupId().orNull();
    }

}
