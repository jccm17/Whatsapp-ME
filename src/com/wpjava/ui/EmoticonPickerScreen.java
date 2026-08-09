package com.wpjava.ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

public class EmoticonPickerScreen extends List implements CommandListener {

    private static final String[] VALUES = {
        ":-)", ":-D", ";-)", ":-P", ":-(",
        ":'(", ":-/", "<3", ":*", "ok",
        "jaja", "gracias", "\u263A", "\u2665", "\u2605"
    };

    private Command insertCommand = new Command("Insertar", Command.OK, 1);
    private Command backCommand = new Command("Atras", Command.BACK, 2);
    private NewMessageScreen target;

    public EmoticonPickerScreen(NewMessageScreen target) {
        super("Emoticones", List.IMPLICIT);
        this.target = target;
        int i;
        for (i = 0; i < VALUES.length; i++) {
            append(VALUES[i], null);
        }
        addCommand(insertCommand);
        addCommand(backCommand);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == insertCommand || c == List.SELECT_COMMAND) {
            int index = getSelectedIndex();
            if (index >= 0 && index < VALUES.length) {
                target.appendEmoticon(VALUES[index]);
            }
        }
        com.wpjava.core.Router.navigate(target);
    }
}
