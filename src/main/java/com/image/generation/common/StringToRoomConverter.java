package com.image.generation.common;

import com.image.generation.entity.Room;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Accepts the enum name in whatever spelling it arrives in — {@code livingroom},
 * {@code living room}, {@code living-room} and {@code LIVING_ROOM} all mean the same room.
 */
@Component
class StringToRoomConverter implements Converter<String, Room> {

    @Override
    public Room convert(String source) {
        String normalized = source.replaceAll("[\\s_-]", "").toUpperCase(Locale.ROOT);
        return Arrays.stream(Room.values())
                .filter(room -> room.name().replace("_", "").equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown room '%s'; known rooms are %s"
                        .formatted(source, Arrays.stream(Room.values()).map(Enum::name).collect(Collectors.joining(", ")))));
    }
}
