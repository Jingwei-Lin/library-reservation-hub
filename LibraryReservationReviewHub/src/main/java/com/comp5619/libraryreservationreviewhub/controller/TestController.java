package com.comp5619.libraryreservationreviewhub.controller;

import com.comp5619.libraryreservationreviewhub.common.BaseResponse;
import com.comp5619.libraryreservationreviewhub.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class TestController {
    /**
     * health check
     */
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }
}
