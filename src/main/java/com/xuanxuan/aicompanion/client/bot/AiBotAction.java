package com.xuanxuan.aicompanion.client.bot;

public final class AiBotAction {
    public String type;
    public String value;
    public int durationTicks;
    public float targetYaw;
    public float targetPitch;

    public AiBotAction() {
    }

    public AiBotAction(String type, String value, int durationTicks) {
        this.type = type;
        this.value = value;
        this.durationTicks = durationTicks;
    }

    public static AiBotAction move(String direction, int ticks) {
        return new AiBotAction("move", direction, ticks);
    }

    public static AiBotAction jump(int ticks) {
        return new AiBotAction("jump", "", ticks);
    }

    public static AiBotAction sneak(boolean enabled, int ticks) {
        return new AiBotAction("sneak", enabled ? "on" : "off", ticks);
    }

    public static AiBotAction look(float yaw, float pitch) {
        AiBotAction a = new AiBotAction("look", "", 1);
        a.targetYaw = yaw;
        a.targetPitch = pitch;
        return a;
    }

    public static AiBotAction attack(int ticks) {
        return new AiBotAction("attack", "", ticks);
    }

    public static AiBotAction use(int ticks) {
        return new AiBotAction("use", "", ticks);
    }

    public static AiBotAction chat(String message) {
        return new AiBotAction("chat", message, 1);
    }

    public static AiBotAction stop(int ticks) {
        return new AiBotAction("stop", "", ticks);
    }

    public static AiBotAction selectSlot(int slot) {
        return new AiBotAction("selectSlot", String.valueOf(slot), 1);
    }

    public static AiBotAction mine(int ticks) {
        return new AiBotAction("mine", "", ticks);
    }

    public static AiBotAction place(int ticks) {
        return new AiBotAction("place", "", ticks);
    }
}
