package com.ai.tutor.appointment.architecture;

import com.ai.tutor.appointment.controller.UserController;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentBoundaryArchTest {

    @Test
    void userControllerShouldNotDependOnMapperTypes() {
        List<String> mapperDependencies = Arrays.stream(UserController.class.getDeclaredFields())
                .filter(this::containsMapperDependency)
                .map(field -> field.getName() + " -> " + field.getGenericType().getTypeName())
                .collect(Collectors.toList());

        assertTrue(mapperDependencies.isEmpty(),
                () -> "UserController must not depend on mapper types: " + mapperDependencies);
    }

    private boolean containsMapperDependency(Field field) {
        String rawTypeName = field.getType().getName();
        String genericTypeName = field.getGenericType().getTypeName();
        return rawTypeName.contains(".mapper.") || genericTypeName.contains(".mapper.");
    }
}
