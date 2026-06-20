package org.example.diplom;

import org.springframework.boot.SpringApplication;

public class TestDiplomApplication {

    public static void main(String[] args) {
        SpringApplication.from(DiplomApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
