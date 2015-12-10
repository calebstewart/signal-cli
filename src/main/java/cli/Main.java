/**
 * Copyright (C) 2015 AsamK
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cli;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.*;
import org.apache.commons.io.IOUtils;
import org.whispersystems.textsecure.api.crypto.UntrustedIdentityException;
import org.whispersystems.textsecure.api.messages.*;
import org.whispersystems.textsecure.api.messages.multidevice.TextSecureSyncMessage;
import org.whispersystems.textsecure.api.push.exceptions.EncapsulatedExceptions;
import org.whispersystems.textsecure.api.push.exceptions.NetworkFailureException;
import org.whispersystems.textsecure.api.push.exceptions.UnregisteredUserException;
import org.whispersystems.textsecure.api.util.InvalidNumberException;
import org.whispersystems.textsecure.api.util.PhoneNumberFormatter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        // Workaround for BKS truststore
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);

        Namespace ns = parseArgs(args);
        if (ns == null) {
            System.exit(1);
        }

        final String username = ns.getString("username");
        final Manager m = new Manager(username);
        if (m.userExists()) {
            try {
                m.load();
            } catch (Exception e) {
                System.err.println("Error loading state file \"" + m.getFileName() + "\": " + e.getMessage());
                System.exit(2);
            }
        }

        switch (ns.getString("command")) {
            case "register":
                if (!m.userHasKeys()) {
                    m.createNewIdentity();
                }
                try {
                    m.register(ns.getBoolean("voice"));
                } catch (IOException e) {
                    System.err.println("Request verify error: " + e.getMessage());
                    System.exit(3);
                }
                break;
            case "verify":
                if (!m.userHasKeys()) {
                    System.err.println("User has no keys, first call register.");
                    System.exit(1);
                }
                if (m.isRegistered()) {
                    System.err.println("User registration is already verified");
                    System.exit(1);
                }
                try {
                    m.verifyAccount(ns.getString("verificationCode"));
                } catch (IOException e) {
                    System.err.println("Verify error: " + e.getMessage());
                    System.exit(3);
                }
                break;
            case "send":
                if (!m.isRegistered()) {
                    System.err.println("User is not registered.");
                    System.exit(1);
                }

                byte[] groupId = null;
                List<String> recipients = null;
                if (ns.getString("group") != null) {
                    try {
                        GroupInfo g = m.getGroupInfo(Base64.decode(ns.getString("group")));
                        if (g == null) {
                            System.err.println("Failed to send to group \"" + ns.getString("group") + "\": Unknown group");
                            System.err.println("Aborting sending.");
                            System.exit(1);
                        }
                        groupId = g.groupId;
                        recipients = new ArrayList<>(g.members);
                    } catch (IOException e) {
                        System.err.println("Failed to send to group \"" + ns.getString("group") + "\": " + e.getMessage());
                        System.err.println("Aborting sending.");
                        System.exit(1);
                    }
                } else {
                    recipients = ns.<String>getList("recipient");
                }

                if (ns.getBoolean("endsession")) {
                    sendEndSessionMessage(m, recipients);
                } else {
                    List<TextSecureAttachment> textSecureAttachments = null;
                    try {
                        textSecureAttachments = getTextSecureAttachments(ns.<String>getList("attachment"));
                    } catch (IOException e) {
                        System.err.println("Failed to add attachment: " + e.getMessage());
                        System.err.println("Aborting sending.");
                        System.exit(1);
                    }

                    String messageText = ns.getString("message");
                    if (messageText == null) {
                        try {
                            messageText = IOUtils.toString(System.in);
                        } catch (IOException e) {
                            System.err.println("Failed to read message from stdin: " + e.getMessage());
                            System.err.println("Aborting sending.");
                            System.exit(1);
                        }
                    }

                    sendMessage(m, messageText, textSecureAttachments, recipients, groupId);
                }

                break;
            case "receive":
                if (!m.isRegistered()) {
                    System.err.println("User is not registered.");
                    System.exit(1);
                }
                int timeout = 5;
                if (ns.getInt("timeout") != null) {
                    timeout = ns.getInt("timeout");
                }
                boolean returnOnTimeout = true;
                if (timeout < 0) {
                    returnOnTimeout = false;
                    timeout = 3600;
                }
                try {
                    m.receiveMessages(timeout, returnOnTimeout, new ReceiveMessageHandler(m));
                } catch (IOException e) {
                    System.err.println("Error while receiving message: " + e.getMessage());
                    System.exit(3);
                } catch (AssertionError e) {
                    System.err.println("Failed to receive message (Assertion): " + e.getMessage());
                    System.err.println(e.getStackTrace());
                    System.err.println("If you use an Oracle JRE please check if you have unlimited strength crypto enabled, see README");
                    System.exit(1);
                }
                break;
            case "quitGroup":
                if (!m.isRegistered()) {
                    System.err.println("User is not registered.");
                    System.exit(1);
                }

                try {
                    GroupInfo g = m.getGroupInfo(Base64.decode(ns.getString("group")));
                    if (g == null) {
                        System.err.println("Failed to send to group \"" + ns.getString("group") + "\": Unknown group");
                        System.err.println("Aborting sending.");
                        System.exit(1);
                    }

                    sendQuitGroupMessage(m, new ArrayList<>(g.members), g.groupId);
                } catch (IOException e) {
                    System.err.println("Failed to send to group \"" + ns.getString("group") + "\": " + e.getMessage());
                    System.err.println("Aborting sending.");
                    System.exit(1);
                }
                break;
            case "updateGroup":
                if (!m.isRegistered()) {
                    System.err.println("User is not registered.");
                    System.exit(1);
                }

                try {
                    GroupInfo g;
                    if (ns.getString("group") != null) {
                        g = m.getGroupInfo(Base64.decode(ns.getString("group")));
                        if (g == null) {
                            System.err.println("Failed to send to group \"" + ns.getString("group") + "\": Unknown group");
                            System.err.println("Aborting sending.");
                            System.exit(1);
                        }
                    } else {
                        // Create new group
                        g = new GroupInfo(Util.getSecretBytes(16));
                        g.members.add(m.getUsername());
                        System.out.println("Creating new group \"" + Base64.encodeBytes(g.groupId) + "\" …");
                    }

                    String name = ns.getString("name");
                    if (name != null) {
                        g.name = name;
                    }

                    final List<String> members = ns.getList("member");

                    if (members != null) {
                        for (String member : members) {
                            try {
                                g.members.add(m.canonicalizeNumber(member));
                            } catch (InvalidNumberException e) {
                                System.err.println("Failed to add member \"" + member + "\" to group: " + e.getMessage());
                                System.err.println("Aborting…");
                                System.exit(1);
                            }
                        }
                    }

                    TextSecureGroup.Builder group = TextSecureGroup.newBuilder(TextSecureGroup.Type.UPDATE)
                            .withId(g.groupId)
                            .withName(g.name)
                            .withMembers(new ArrayList<>(g.members));

                    String avatar = ns.getString("avatar");
                    if (avatar != null) {
                        try {
                            group.withAvatar(createAttachment(avatar));
                            // TODO
                            g.avatarId = 0;
                        } catch (IOException e) {
                            System.err.println("Failed to add attachment \"" + avatar + "\": " + e.getMessage());
                            System.err.println("Aborting sending.");
                            System.exit(1);
                        }
                    }

                    m.setGroupInfo(g);

                    sendUpdateGroupMessage(m, group.build());
                } catch (IOException e) {
                    System.err.println("Failed to send to group \"" + ns.getString("group") + "\": " + e.getMessage());
                    System.err.println("Aborting sending.");
                    System.exit(1);
                }

                break;
        }
        m.save();
        System.exit(0);
    }

    private static List<TextSecureAttachment> getTextSecureAttachments(List<String> attachments) {
    private static List<TextSecureAttachment> getTextSecureAttachments(List<String> attachments) throws IOException {
        List<TextSecureAttachment> textSecureAttachments = null;
        if (attachments != null) {
            textSecureAttachments = new ArrayList<>(attachments.size());
            for (String attachment : attachments) {
                textSecureAttachments.add(createAttachment(attachment));
             }
        }
        return textSecureAttachments;
    }

    private static TextSecureAttachmentStream createAttachment(String attachment) throws IOException {
        File attachmentFile = new File(attachment);
        InputStream attachmentStream = new FileInputStream(attachmentFile);
        final long attachmentSize = attachmentFile.length();
        String mime = Files.probeContentType(Paths.get(attachment));
        return new TextSecureAttachmentStream(attachmentStream, mime, attachmentSize, null);
    }

    private static Namespace parseArgs(String[] args) {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("textsecure-cli")
                .defaultHelp(true)
                .description("Commandline interface for TextSecure.")
                .version(Manager.PROJECT_NAME + " " + Manager.PROJECT_VERSION);

        parser.addArgument("-u", "--username")
                .help("Specify your phone number, that will be used for verification.");
        parser.addArgument("-v", "--version")
                .help("Show package version.")
                .action(Arguments.version());

        Subparsers subparsers = parser.addSubparsers()
                .title("subcommands")
                .dest("command")
                .description("valid subcommands")
                .help("additional help");

        Subparser parserRegister = subparsers.addParser("register");
        parserRegister.addArgument("-v", "--voice")
                .help("The verification should be done over voice, not sms.")
                .action(Arguments.storeTrue());

        Subparser parserVerify = subparsers.addParser("verify");
        parserVerify.addArgument("verificationCode")
                .help("The verification code you received via sms or voice call.");

        Subparser parserSend = subparsers.addParser("send");
        parserSend.addArgument("-g", "--group")
                .help("Specify the recipient group ID.");
        parserSend.addArgument("recipient")
                .help("Specify the recipients' phone number.")
                .nargs("*");
        parserSend.addArgument("-m", "--message")
                .help("Specify the message, if missing standard input is used.");
        parserSend.addArgument("-a", "--attachment")
                .nargs("*")
                .help("Add file as attachment");
        parserSend.addArgument("-e", "--endsession")
                .help("Clear session state and send end session message.")
                .action(Arguments.storeTrue());

        Subparser parserLeaveGroup = subparsers.addParser("quitGroup");
        parserLeaveGroup.addArgument("-g", "--group")
                .required(true)
                .help("Specify the recipient group ID.");

        Subparser parserUpdateGroup = subparsers.addParser("updateGroup");
        parserUpdateGroup.addArgument("-g", "--group")
                .help("Specify the recipient group ID.");
        parserUpdateGroup.addArgument("-n", "--name")
                .help("Specify the new group name.");
        parserUpdateGroup.addArgument("-a", "--avatar")
                .help("Specify a new group avatar image file");
        parserUpdateGroup.addArgument("-m", "--member")
                .nargs("*")
                .help("Specify one or more members to add to the group");

        Subparser parserReceive = subparsers.addParser("receive");
        parserReceive.addArgument("-t", "--timeout")
                .type(int.class)
                .help("Number of seconds to wait for new messages (negative values disable timeout)");

        try {
            Namespace ns = parser.parseArgs(args);
            if (ns.getString("username") == null) {
                parser.printUsage();
                System.err.println("You need to specify a username (phone number)");
                System.exit(2);
            }
            if (!PhoneNumberFormatter.isValidNumber(ns.getString("username"))) {
                System.err.println("Invalid username (phone number), make sure you include the country code.");
                System.exit(2);
            }
            if (ns.getList("recipient") != null && !ns.getList("recipient").isEmpty() && ns.getString("group") != null) {
                System.err.println("You cannot specify recipients by phone number and groups a the same time");
                System.exit(2);
            }
            return ns;
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            return null;
        }
    }

    private static void sendMessage(Manager m, String messageText, List<TextSecureAttachment> textSecureAttachments,
                                    List<String> recipients, byte[] groupId) {
        final TextSecureDataMessage.Builder messageBuilder = TextSecureDataMessage.newBuilder().withBody(messageText);
        if (textSecureAttachments != null) {
            messageBuilder.withAttachments(textSecureAttachments);
        }
        if (groupId != null) {
            messageBuilder.asGroupMessage(new TextSecureGroup(groupId));
        }
        TextSecureDataMessage message = messageBuilder.build();

        sendMessage(m, message, recipients);
    }

    private static void sendEndSessionMessage(Manager m, List<String> recipients) {
        final TextSecureDataMessage.Builder messageBuilder = TextSecureDataMessage.newBuilder().asEndSessionMessage();

        TextSecureDataMessage message = messageBuilder.build();

        sendMessage(m, message, recipients);
    }

    private static void sendQuitGroupMessage(Manager m, List<String> recipients, byte[] groupId) {
        final TextSecureDataMessage.Builder messageBuilder = TextSecureDataMessage.newBuilder();
        TextSecureGroup group = TextSecureGroup.newBuilder(TextSecureGroup.Type.QUIT)
                .withId(groupId)
                .build();

        messageBuilder.asGroupMessage(group);

        TextSecureDataMessage message = messageBuilder.build();

        sendMessage(m, message, recipients);
    }

    private static void sendUpdateGroupMessage(Manager m, TextSecureGroup g) {
        final TextSecureDataMessage.Builder messageBuilder = TextSecureDataMessage.newBuilder();

        messageBuilder.asGroupMessage(g);

        TextSecureDataMessage message = messageBuilder.build();

        sendMessage(m, message, g.getMembers().get());
    }

    private static void sendMessage(Manager m, TextSecureDataMessage message, List<String> recipients) {
        try {
            m.sendMessage(recipients, message);
        } catch (IOException e) {
            System.err.println("Failed to send message: " + e.getMessage());
        } catch (EncapsulatedExceptions e) {
            System.err.println("Failed to send (some) messages:");
            for (NetworkFailureException n : e.getNetworkExceptions()) {
                System.err.println("Network failure for \"" + n.getE164number() + "\": " + n.getMessage());
            }
            for (UnregisteredUserException n : e.getUnregisteredUserExceptions()) {
                System.err.println("Unregistered user \"" + n.getE164Number() + "\": " + n.getMessage());
            }
            for (UntrustedIdentityException n : e.getUntrustedIdentityExceptions()) {
                System.err.println("Untrusted Identity for \"" + n.getE164Number() + "\": " + n.getMessage());
            }
        } catch (AssertionError e) {
            System.err.println("Failed to send message (Assertion): " + e.getMessage());
            System.err.println(e.getStackTrace());
            System.err.println("If you use an Oracle JRE please check if you have unlimited strength crypto enabled, see README");
            System.exit(1);
        }
    }

    private static class ReceiveMessageHandler implements Manager.ReceiveMessageHandler {
        final Manager m;

        public ReceiveMessageHandler(Manager m) {
            this.m = m;
        }

        @Override
        public void handleMessage(TextSecureEnvelope envelope, TextSecureContent content, GroupInfo group) {
            System.out.println("Envelope from: " + envelope.getSource());
            System.out.println("Timestamp: " + envelope.getTimestamp());

            if (envelope.isReceipt()) {
                System.out.println("Got receipt.");
            } else if (envelope.isWhisperMessage() | envelope.isPreKeyWhisperMessage()) {
                if (content == null) {
                    System.out.println("Failed to decrypt message.");
                } else {
                    if (content.getDataMessage().isPresent()) {
                        TextSecureDataMessage message = content.getDataMessage().get();

                        System.out.println("Message timestamp: " + message.getTimestamp());

                        if (message.getBody().isPresent()) {
                            System.out.println("Body: " + message.getBody().get());
                        }
                        if (message.getGroupInfo().isPresent()) {
                            TextSecureGroup groupInfo = message.getGroupInfo().get();
                            System.out.println("Group info:");
                            System.out.println("  Id: " + Base64.encodeBytes(groupInfo.getGroupId()));
                            if (groupInfo.getName().isPresent()) {
                                System.out.println("  Name: " + groupInfo.getName().get());
                            } else if (group != null) {
                                System.out.println("  Name: " + group.name);
                            } else {
                                System.out.println("  Name: <Unknown group>");
                            }
                            System.out.println("  Type: " + groupInfo.getType());
                            if (groupInfo.getMembers().isPresent()) {
                                for (String member : groupInfo.getMembers().get()) {
                                    System.out.println("  Member: " + member);
                                }
                            }
                            if (groupInfo.getAvatar().isPresent()) {
                                System.out.println("  Avatar:");
                                printAttachment(groupInfo.getAvatar().get());
                            }
                        }
                        if (message.isEndSession()) {
                            System.out.println("Is end session");
                        }

                        if (message.getAttachments().isPresent()) {
                            System.out.println("Attachments: ");
                            for (TextSecureAttachment attachment : message.getAttachments().get()) {
                                printAttachment(attachment);
                            }
                        }
                    }
                    if (content.getSyncMessage().isPresent()) {
                        TextSecureSyncMessage syncMessage = content.getSyncMessage().get();
                        System.out.println("Received sync message");
                    }
                }
            } else {
                System.out.println("Unknown message received.");
            }
            System.out.println();
        }

        private void printAttachment(TextSecureAttachment attachment) {
            System.out.println("- " + attachment.getContentType() + " (" + (attachment.isPointer() ? "Pointer" : "") + (attachment.isStream() ? "Stream" : "") + ")");
            if (attachment.isPointer()) {
                final TextSecureAttachmentPointer pointer = attachment.asPointer();
                System.out.println("  Id: " + pointer.getId() + " Key length: " + pointer.getKey().length + (pointer.getRelay().isPresent() ? " Relay: " + pointer.getRelay().get() : ""));
                System.out.println("  Size: " + (pointer.getSize().isPresent() ? pointer.getSize().get() + " bytes" : "<unavailable>") + (pointer.getPreview().isPresent() ? " (Preview is available: " + pointer.getPreview().get().length + " bytes)" : ""));
                File file = m.getAttachmentFile(pointer.getId());
                if (file.exists()) {
                    System.out.println("  Stored plaintext in: " + file);
                }
            }
        }
    }
}
