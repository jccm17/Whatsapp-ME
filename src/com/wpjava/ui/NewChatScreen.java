package com.wpjava.ui;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;

import com.wpjava.core.Router;
import com.wpjava.model.Chat;
import com.wpjava.service.ApiClient;

/** Entry menu for starting a conversation from a number or available chats. */
public class NewChatScreen implements CommandListener {

    private static final Command BACK = new Command("Atras", Command.BACK, 1);
    private static final Command SELECT = new Command("Selec", Command.OK, 1);

    private List menu;

    public NewChatScreen() {
        menu = new List("Nuevo chat", List.IMPLICIT);
        menu.append("Ingresar numero", null);
        menu.append("Contactos WhatsApp", null);
        menu.append("Contactos del telefono", null);
        menu.addCommand(BACK);
        menu.addCommand(SELECT);
        menu.setCommandListener(this);
    }

    public Displayable getDisplayable() { return menu; }

    public void show() { Router.navigate(menu); }

    public void commandAction(Command command, Displayable displayable) {
        if (command == BACK || command.getCommandType() == Command.BACK) {
            Router.back();
            return;
        }
        if (command != List.SELECT_COMMAND && command != SELECT
                && command.getCommandType() != Command.OK) return;

        int selected = menu.getSelectedIndex();
        if (selected == 0) openPhoneInput();
        else if (selected == 1) Router.navigate(new ChatListScreen());
        else if (selected == 2) Router.navigate(new PhoneContactsScreen());
    }

    private void openPhoneInput() {
        final TextBox input = new TextBox("Numero con cod. pais", "", 20,
                TextField.PHONENUMBER);
        final Command start = new Command("Iniciar", Command.OK, 1);
        final Command cancel = new Command("Cancelar", Command.BACK, 2);
        input.addCommand(start);
        input.addCommand(cancel);
        input.setCommandListener(new CommandListener() {
            public void commandAction(Command command, Displayable displayable) {
                if (command == start) {
                    String number = normalizeNumber(input.getString());
                    if (number.length() >= 11) {
                        Chat chat = new Chat();
                        chat.id = ApiClient.normalizeRecipient(number);
                        chat.name = number;
                        chat.lastMessage = "";
                        chat.timestamp = System.currentTimeMillis();
                        Router.navigate(new ChatScreen(chat));
                        return;
                    }
                    showValidation(input);
                    return;
                }
                Router.replace(menu);
            }
        });
        Router.getDisplay().setCurrent(input);
    }

    private String normalizeNumber(String value) {
        StringBuffer result = new StringBuffer();
        int i;
        for (i = 0; value != null && i < value.length(); i++) {
            char current = value.charAt(i);
            if (current >= '0' && current <= '9') result.append(current);
        }
        return result.toString();
    }

    private void showValidation(TextBox input) {
        Alert alert = new Alert("Numero invalido",
                "Ingresa al menos 10 digitos, incluido el codigo de pais.",
                null, AlertType.WARNING);
        alert.setTimeout(2200);
        Router.getDisplay().setCurrent(alert, input);
    }

}
