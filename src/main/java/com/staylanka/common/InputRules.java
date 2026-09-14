package com.staylanka.common;

import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

@Component
public class InputRules {
    private final Validator validator;
    public InputRules(Validator validator) { this.validator = validator; }
    public void validate(Object form) {
        var errors = validator.validate(form);
        if (!errors.isEmpty()) {
            var first = errors.stream().sorted(java.util.Comparator.comparing(v -> v.getPropertyPath().toString())).findFirst().orElseThrow();
            throw new BusinessRuleException(first.getPropertyPath() + ": " + first.getMessage());
        }
    }
}
