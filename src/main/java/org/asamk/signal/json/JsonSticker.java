package org.asamk.signal.json;

import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;

public class JsonSticker {

    byte[] packid;
    byte[] packkey;
    int id;
    JsonAttachment attachment;

    public JsonSticker(SignalServiceDataMessage.Sticker sticker) {
        this.packid = sticker.getPackId();
        this.packkey = sticker.getPackKey();
        this.id = sticker.getStickerId();
        this.attachment = new JsonAttachment(sticker.getAttachment());
    }

}
