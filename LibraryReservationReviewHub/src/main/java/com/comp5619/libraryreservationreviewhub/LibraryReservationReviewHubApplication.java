package com.comp5619.libraryreservationreviewhub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.comp5619.libraryreservationreviewhub.mapper")
public class LibraryReservationReviewHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryReservationReviewHubApplication.class, args);
    }

}
