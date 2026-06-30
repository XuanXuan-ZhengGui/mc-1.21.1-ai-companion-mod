package com.xuanxuan.aicompanion.client.bot;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class AiBotActionQueue {
    private final Deque<AiBotAction> queue = new ArrayDeque<>();
    private AiBotAction current = null;
    private int currentTick = 0;

    public void clear() {
        queue.clear();
        current = null;
        currentTick = 0;
    }

    public void add(AiBotAction action) {
        if (action != null) {
            queue.addLast(action);
        }
    }

    public void addAll(List<AiBotAction> actions) {
        if (actions != null) {
            for (AiBotAction a : actions) {
                if (a != null) queue.addLast(a);
            }
        }
    }

    public AiBotAction tick() {
        if (current == null) {
            if (queue.isEmpty()) return null;
            current = queue.pollFirst();
            currentTick = 0;
        }

        currentTick++;
        if (currentTick >= current.durationTicks) {
            AiBotAction finished = current;
            current = null;
            currentTick = 0;
            return finished;
        }

        return current;
    }

    public boolean isEmpty() {
        return current == null && queue.isEmpty();
    }

    public int size() {
        return queue.size() + (current != null ? 1 : 0);
    }

    public AiBotAction getCurrent() {
        return current;
    }

    public String getStatus() {
        if (current != null) {
            return current.type + "(" + currentTick + "/" + current.durationTicks + ") + " + queue.size() + " queued";
        }
        return "idle, " + queue.size() + " queued";
    }
}
