package org.asamk.signal.dbus;

import org.asamk.signal.storage.contacts.ContactInfo;
import org.freedesktop.dbus.DBusInterface;

public class DBusContact extends ContactInfo implements DBusInterface {

    public boolean isRemote() {
        return false;
    }

}
