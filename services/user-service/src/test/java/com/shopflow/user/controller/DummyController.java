package com.shopflow.user.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class DummyController {

    @GetMapping
    public String ok() { return "ok"; }

    @GetMapping("/error")
    public String error() { throw new RuntimeException("Boom!"); }

    @GetMapping("/delay")
    public String delay() throws InterruptedException {
        Thread.sleep(50);
        return "delayed";
    }

    @GetMapping("/noheader")
    public String noHeader() { return "headerless"; }
}
