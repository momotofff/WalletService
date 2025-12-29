package com.example.wallet;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@SpringBootApplication
public class WalletApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(WalletApplication.class, args);
    }

    @Bean
    CommandLineRunner checkConfig(Environment env)
    {
        return args -> {
            System.out.println("=== DATABASE CONFIG ===");
            System.out.println("URL: " + env.getProperty("spring.datasource.url"));
            System.out.println("Username: " + env.getProperty("spring.datasource.username"));
            System.out.println("Active profiles: " + Arrays.toString(env.getActiveProfiles()));
        };
    }
}