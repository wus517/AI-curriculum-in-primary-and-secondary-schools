package com.example.oj.platform;

import java.time.Duration;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import static com.example.oj.platform.PlatformDtos.*;

public interface TutorModelClient {
    ModelTurn generate(ModelRequest request, Duration timeout, Consumer<String> delta, BooleanSupplier cancelled);
    boolean isAvailable();
}
