package com.sobalab.bandaid.api;

import org.springframework.boot.SpringApplication;

public class TestBandaidApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(BandaidApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
