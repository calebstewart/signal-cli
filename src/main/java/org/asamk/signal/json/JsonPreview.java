package org.asamk.signal.json;

import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;

public class JsonPreview {

    String title;
    String url;
    JsonAttachment attachment;

    public JsonPreview(SignalServiceDataMessage.Preview preview) {
        this.title = preview.getTitle();
        this.url = preview.getUrl();
        if (preview.getImage().isPresent()) {
            this.attachment = new JsonAttachment(preview.getImage().get());
        }
    }

}
