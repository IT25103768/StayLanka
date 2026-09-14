```java
        package com.staylanka.stay;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class CheckOutForm {

    @NotNull(message = "Actual check-out time is required")
    @PastOrPresent(message = "Check-out time cannot be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime actualCheckOut =
            LocalDateTime.now().withSecond(0).withNano(0);

    // Default constructor
    public CheckOutForm() {
    }

    public LocalDateTime getActualCheckOut() {
        return actualCheckOut;
    }

    public void setActualCheckOut(LocalDateTime actualCheckOut) {
        this.actualCheckOut = actualCheckOut;
    }
}
```
