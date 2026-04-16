package io.drahlek.dirigo.event;

import lombok.Data;

@Data
public abstract class EventBase {
    public void publish() {
        EventBus.INSTANCE.publish(this);
    }
}
