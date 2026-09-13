package com.staylanka.request;

import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

@Component
public class RequestHistoryListener {
    private final RequestHistoryRepository historyRepository;

    public RequestHistoryListener(RequestHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @EventListener
    public void record(RequestStatusChangedEvent event) {
        historyRepository.save(new RequestHistory(event.request(), event.actor(), event.oldStatus(),
                event.newStatus(), event.note()));
    }
}
