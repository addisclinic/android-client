package org.moca.events;

/**
 * Created by Albert on 4/3/2016.
 */
public class BaseEvent {
    private String type;

    protected BaseEvent(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
