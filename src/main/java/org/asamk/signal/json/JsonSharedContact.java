package org.asamk.signal.json;

import org.whispersystems.signalservice.api.messages.shared.SharedContact;

import java.util.ArrayList;
import java.util.List;

public class JsonSharedContact {

    Name name;
    JsonAttachment avatar;
    List<Phone> phone;
    List<Email> email;
    List<PostalAddress> address;
    String organization;

    public JsonSharedContact(SharedContact contact) {
        this.name = new Name(contact.getName());
        if (contact.getAvatar().isPresent()) {
            this.avatar = new JsonAttachment(contact.getAvatar().get().getAttachment());
        }
        if (contact.getPhone().isPresent()) {
            this.phone = new ArrayList<>(contact.getPhone().get().size());
            for (SharedContact.Phone phone : contact.getPhone().get()) {
                this.phone.add(new Phone(phone));
            }
        }
        if (contact.getEmail().isPresent()) {
            this.email = new ArrayList<>(contact.getEmail().get().size());
            for (SharedContact.Email email : contact.getEmail().get()) {
                this.email.add(new Email(email));
            }
        }
        if (contact.getAddress().isPresent()) {
            this.address = new ArrayList<>(contact.getAddress().get().size());
            for (SharedContact.PostalAddress address : contact.getAddress().get()) {
                this.address.add(new PostalAddress(address));
            }
        }
    }

    public static class Name {
        public String display;
        public String given;
        public String family;
        public String prefix;
        public String suffix;
        public String middle;

        public Name(SharedContact.Name name) {
            this.display = name.getDisplay().orNull();
            this.given = name.getGiven().orNull();
            this.family = name.getFamily().orNull();
            this.prefix = name.getPrefix().orNull();
            this.suffix = name.getSuffix().orNull();
            this.middle = name.getMiddle().orNull();
        }
    }

    public static class Phone {

        String number;
        String type;
        String label;

        public Phone(SharedContact.Phone phone) {
            this.number = phone.getValue();
            this.type = phone.getType().toString();
            this.label = phone.getLabel().orNull();
        }
    }

    public static class Email {
        String address;
        String type;
        String label;

        public Email(SharedContact.Email email) {
            this.address = email.getValue();
            this.type = email.getType().toString();
            this.label = email.getLabel().orNull();
        }
    }

    public static class PostalAddress {
        String type;
        String label;
        String street;
        String pobox;
        String neighborhood;
        String city;
        String region;
        String postcode;
        String country;

        public PostalAddress(SharedContact.PostalAddress address) {
            this.type = address.getType().toString();
            this.label = address.getLabel().orNull();
            this.street = address.getStreet().orNull();
            this.pobox = address.getPobox().orNull();
            this.neighborhood = address.getNeighborhood().orNull();
            this.city = address.getCity().orNull();
            this.region = address.getRegion().orNull();
            this.postcode = address.getPostcode().orNull();
            this.country = address.getCountry().orNull();
        }
    }

}
