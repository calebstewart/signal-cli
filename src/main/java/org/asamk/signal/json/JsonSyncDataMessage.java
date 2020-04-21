package org.asamk.signal.json;

import org.asamk.Signal;
import org.whispersystems.signalservice.api.messages.multidevice.SentTranscriptMessage;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;

import java.util.ArrayList;
import java.util.List;

class JsonSyncDataMessage extends JsonDataMessage {

    List<String> recipients;

    JsonSyncDataMessage(SentTranscriptMessage transcriptMessage) {
        super(transcriptMessage.getMessage());
        this.recipients = new ArrayList<String>();
        if (transcriptMessage.getDestination().isPresent()) {
            this.recipients.add(transcriptMessage.getDestination().get().getNumber().get());
        } else {
            for( SignalServiceAddress dest : transcriptMessage.getRecipients() ){
                this.recipients.add(dest.getNumber().get());
            }
        }
    }

    JsonSyncDataMessage(Signal.SyncMessageReceived messageReceived) {
        super(messageReceived);
        this.recipients = new ArrayList<String>();
        this.recipients.add(messageReceived.getDestination());
    }
}
