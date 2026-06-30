package com.xuanxuan.aicompanion.client.bot;

public final class AiBotInput {
    public float forward;
    public float sideways;
    public boolean jumping;
    public boolean sneaking;
    public float yaw;
    public float pitch;
    public boolean attacking;
    public boolean using;
    public String chatMessage;

    public void reset() {
        forward = 0;
        sideways = 0;
        jumping = false;
        sneaking = false;
        attacking = false;
        using = false;
        chatMessage = null;
    }
}
