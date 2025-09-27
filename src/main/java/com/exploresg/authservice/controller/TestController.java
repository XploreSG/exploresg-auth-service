package com.exploresg.authservice.controller;

import com.exploresg.authservice.entity.TestEntity;
import com.exploresg.authservice.repository.TestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {
    private final TestRepository testRepository;

    @PostMapping
    public TestEntity create(@RequestBody TestEntity entity){
        return testRepository.save(entity);
    }


    @GetMapping
    public List<TestEntity> finalAll(){
        return testRepository.findAll();
    }
}
