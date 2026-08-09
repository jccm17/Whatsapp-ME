package com.wpjava.ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;

import com.wpjava.core.Router;

public class ContactScreen extends Form implements CommandListener {

    private Command backCommand = new Command("Atras", Command.BACK, 1);

    public ContactScreen() {
        super("Contacto");
        append("Informacion del contacto");
        addCommand(backCommand);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        Router.navigate(new ChatListScreen());
    }
}
