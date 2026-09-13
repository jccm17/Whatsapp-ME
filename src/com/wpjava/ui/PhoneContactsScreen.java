package com.wpjava.ui;

import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.pim.Contact;
import javax.microedition.pim.ContactList;
import javax.microedition.pim.PIM;

import com.wpjava.core.Router;
import com.wpjava.model.Chat;
import com.wpjava.service.ApiClient;

/** Reads phone contacts through the standard JSR-75 PIM API. */
public class PhoneContactsScreen extends List implements CommandListener, Runnable {

    private final Command backCommand = new Command("Atras", Command.BACK, 1);
    private final Vector phones = new Vector();
    private String error;

    public PhoneContactsScreen() {
        super("Contactos del telefono", List.IMPLICIT);
        append("Cargando agenda...", null);
        addCommand(backCommand);
        setCommandListener(this);
        new Thread(this).start();
    }

    public void run() {
        ContactList list = null;
        try {
            list = (ContactList) PIM.getInstance().openPIMList(PIM.CONTACT_LIST, PIM.READ_ONLY);
            Enumeration items = list.items();
            while (items.hasMoreElements()) {
                Contact contact = (Contact) items.nextElement();
                String name = nameOf(contact);
                int count = contact.countValues(Contact.TEL);
                int i;
                for (i = 0; i < count; i++) {
                    String phone = cleanPhone(contact.getString(Contact.TEL, i));
                    if (phone.length() > 0) {
                        phones.addElement(new PhoneContact(name, phone));
                    }
                }
            }
        } catch (Exception e) {
            error = "No se pudo leer la agenda. Permite Contactos para esta aplicacion.";
        } finally {
            try { if (list != null) list.close(); } catch (Exception ignored) { }
        }
        Router.getDisplay().callSerially(new Runnable() {
            public void run() {
                deleteAll();
                if (error != null) {
                    append(error, null);
                } else if (phones.size() == 0) {
                    append("No hay contactos con numero", null);
                } else {
                    int i;
                    for (i = 0; i < phones.size(); i++) {
                        PhoneContact item = (PhoneContact) phones.elementAt(i);
                        append(item.name + "\n" + item.phone, null);
                    }
                }
            }
        });
    }

    public void commandAction(Command command, Displayable displayable) {
        if (command == backCommand) {
            Router.back();
            return;
        }
        int index = getSelectedIndex();
        if (error != null || index < 0 || index >= phones.size()) return;
        PhoneContact item = (PhoneContact) phones.elementAt(index);
        Chat chat = new Chat();
        chat.id = ApiClient.normalizeRecipient(item.phone);
        chat.name = item.name;
        Router.navigate(new ChatScreen(chat));
    }

    private String nameOf(Contact contact) {
        try {
            if (contact.countValues(Contact.FORMATTED_NAME) > 0) {
                String value = contact.getString(Contact.FORMATTED_NAME, 0);
                if (value != null && value.trim().length() > 0) return value.trim();
            }
        } catch (Exception ignored) { }
        return "Contacto";
    }

    private String cleanPhone(String value) {
        if (value == null) return "";
        StringBuffer out = new StringBuffer();
        int i;
        for (i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') out.append(c);
        }
        return out.toString();
    }

    private static class PhoneContact {
        String name;
        String phone;

        PhoneContact(String valueName, String valuePhone) {
            name = valueName;
            phone = valuePhone;
        }
    }
}
