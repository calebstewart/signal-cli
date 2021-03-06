package org.asamk.signal.json;

import org.asamk.Signal;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.SignalServiceGroup;
import org.whispersystems.signalservice.api.messages.shared.SharedContact;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class JsonDataMessage {

    long timestamp;
    String message;
    int expiresInSeconds;
    List<JsonAttachment> attachments;
    JsonGroupInfo groupInfo;
    List<JsonPreview> previews;
    List<JsonSharedContact> contacts;
    JsonSticker sticker;
    JsonReaction reaction;
    JsonQuote quote;

    JsonDataMessage(SignalServiceDataMessage dataMessage) {
        this.timestamp = dataMessage.getTimestamp();
        if (dataMessage.getGroupContext().isPresent() && dataMessage.getGroupContext().get().getGroupV1().isPresent()) {
            SignalServiceGroup groupInfo = dataMessage.getGroupContext().get().getGroupV1().get();
            this.groupInfo = new JsonGroupInfo(groupInfo);
        }
        if (dataMessage.getBody().isPresent()) {
            this.message = dataMessage.getBody().get();
        }
        this.expiresInSeconds = dataMessage.getExpiresInSeconds();
        if (dataMessage.getPreviews().isPresent() ) {
            this.previews = new ArrayList<>(dataMessage.getPreviews().get().size());
            for (SignalServiceDataMessage.Preview preview : dataMessage.getPreviews().get()) {
                this.previews.add(new JsonPreview(preview));
            }
        }
        if (dataMessage.getSharedContacts().isPresent() ) {
            this.contacts = new ArrayList<>(dataMessage.getSharedContacts().get().size());
            for (SharedContact contact : dataMessage.getSharedContacts().get()) {
                this.contacts.add(new JsonSharedContact(contact));
            }
        }
        if (dataMessage.getSticker().isPresent()) {
            this.sticker = new JsonSticker(dataMessage.getSticker().get());
        }
        if (dataMessage.getReaction().isPresent() ){
            this.reaction = new JsonReaction(dataMessage.getReaction().get());
        }
        if (dataMessage.getQuote().isPresent() ){
            this.quote = new JsonQuote(dataMessage.getQuote().get());
        }
        if (dataMessage.getAttachments().isPresent()) {
            this.attachments = new ArrayList<>(dataMessage.getAttachments().get().size());
            for (SignalServiceAttachment attachment : dataMessage.getAttachments().get()) {
                this.attachments.add(new JsonAttachment(attachment));
            }
        } else {
            this.attachments = new ArrayList<>();
        }
    }

    public JsonDataMessage(Signal.MessageReceived messageReceived) {
        timestamp = messageReceived.getTimestamp();
        message = messageReceived.getMessage();
        groupInfo = new JsonGroupInfo(messageReceived.getGroupId());
        attachments = messageReceived.getAttachments()
                .stream()
                .map(JsonAttachment::new)
                .collect(Collectors.toList());
    }

    public JsonDataMessage(Signal.SyncMessageReceived messageReceived) {
        timestamp = messageReceived.getTimestamp();
        message = messageReceived.getMessage();
        groupInfo = new JsonGroupInfo(messageReceived.getGroupId());
        attachments = messageReceived.getAttachments()
                .stream()
                .map(JsonAttachment::new)
                .collect(Collectors.toList());
    }
}
