package org.asamk.signal.json;

import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;

import java.util.ArrayList;
import java.util.List;

public class JsonQuote {

    long id;
    String author;
    String message;
    List<JsonAttachment> attachments;

    public JsonQuote(SignalServiceDataMessage.Quote quote) {
        this.id = quote.getId();
        this.author = quote.getAuthor().getNumber().get();
        this.message = quote.getText();
        this.attachments = new ArrayList<>(quote.getAttachments().size());
        for (SignalServiceDataMessage.Quote.QuotedAttachment a : quote.getAttachments()) {
            this.attachments.add(new JsonAttachment(a.getThumbnail()));
        }
    }

}
