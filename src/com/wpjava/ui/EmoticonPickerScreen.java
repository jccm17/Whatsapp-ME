package com.wpjava.ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

public class EmoticonPickerScreen extends List implements CommandListener {

    private static final String[] VALUES = {
        "\uD83D\uDE00", "\uD83D\uDE03", "\uD83D\uDE04", "\uD83D\uDE01", "\uD83D\uDE06",
        "\uD83E\uDD79", "\uD83D\uDE05", "\uD83D\uDE02", "\uD83E\uDD23", "\uD83E\uDD72",
        "\u263A", "\uD83D\uDE0A", "\uD83D\uDE07", "\uD83D\uDE42", "\uD83D\uDE43",
        "\uD83D\uDE09", "\uD83D\uDE0C", "\uD83D\uDE0D", "\uD83E\uDD70", "\uD83D\uDE18",
        "\uD83D\uDE17", "\uD83D\uDE19", "\uD83D\uDE1A", "\uD83D\uDE0B", "\uD83D\uDE1D",
        "\uD83D\uDE1B", "\uD83E\uDD2A", "\uD83E\uDDD0", "\uD83E\uDD28", "\uD83D\uDE0E",
        "\uD83D\uDE13", "\uD83D\uDCA4", "\uD83D\uDC40", "\uD83D\uDC4D\uD83C\uDFFB", "\uD83D\uDC4E\uD83C\uDFFB",
        "\uD83D\uDC80", "\uD83D\uDEAC"
    };
    private static final String[] LABELS = {
        "Sonrisa", "Feliz", "Risa", "Riendo", "Carcajada", "Emocionado", "Sudor", "Lagrimas", "Risa fuerte", "Emocion",
        "Sonrisa", "Sonriente", "Angel", "Leve", "Al reves", "Guino", "Aliviado", "Enamorado", "Con corazones", "Beso",
        "Beso", "Beso", "Beso", "Rico", "Travieso", "Lengua", "Loco", "Monoculo", "Pensando", "Gafas",
        "Nerd", "Dormir", "Ojos", "Me gusta", "No me gusta", "Calavera", "Cigarrillo"
    };

    private Command insertCommand = new Command("Insertar", Command.OK, 1);
    private Command backCommand = new Command("Atras", Command.BACK, 2);
    private Target target;

    public EmoticonPickerScreen(Target target) {
        super("Emoticones", List.IMPLICIT);
        this.target = target;
        int i;
        for (i = 0; i < VALUES.length; i++) {
            append(LABELS[i], null);
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
        com.wpjava.core.Router.getDisplay().setCurrent(target.getEmoticonDisplayable());
    }

    public interface Target {
        void appendEmoticon(String value);
        Displayable getEmoticonDisplayable();
    }
}
