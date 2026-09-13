package com.staylanka.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RequestResponseForm {
    @NotBlank @Size(min = 2, max = 4000)
    private String message;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

