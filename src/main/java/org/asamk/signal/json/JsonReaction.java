package org.asamk.signal.json;

import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;

public class JsonReaction {

    String emoji;
    String targetAuthor;
    long targetTimestamp;
    boolean isRemove;

    public JsonReaction(SignalServiceDataMessage.Reaction reaction) {
        this.emoji = reaction.getEmoji();
        this.targetAuthor = reaction.getTargetAuthor().getNumber().get();
        this.targetTimestamp = reaction.getTargetSentTimestamp();
        this.isRemove = reaction.isRemove();
    }

}
