package com.sobalab.mymuse.api;

import org.springframework.boot.SpringApplication;

public class TestMymuseApiApplication {

    public static void main(String[] args) {
        SpringApplication.from( MymuseApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
