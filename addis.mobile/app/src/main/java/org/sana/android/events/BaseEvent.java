package org.sana.android.events;

/**
 * Created by Albert on 9/3/2016.
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
