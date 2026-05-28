package com.kraft.lotto.it;

import org.testcontainers.DockerClientFactory;

final class TestcontainersAvailability {

    private TestcontainersAvailability() {
    }

    static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
