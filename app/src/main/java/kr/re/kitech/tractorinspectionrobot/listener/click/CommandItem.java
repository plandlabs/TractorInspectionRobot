package kr.re.kitech.tractorinspectionrobot.listener.click;

public class CommandItem {
    public String type; // "C" (coil) or "H" (holding)
    public int key;
    public int value;

    private CommandItem(String type, int key, int value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    public static CommandItem coil(int key, int value) {
        return new CommandItem("C", key, value);
    }

    public static CommandItem holding(int key, int value) {
        return new CommandItem("H", key, value);
    }
}
