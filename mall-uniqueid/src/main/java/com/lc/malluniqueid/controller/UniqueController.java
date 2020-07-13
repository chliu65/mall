package com.lc.malluniqueid.controller;

import com.lc.malluniqueid.service.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@Slf4j
@RestController
@RequestMapping("/uniqueid")
public class UniqueController {
    @Autowired
    private IdGenerator idGenerator;
    @RequestMapping("/getUniqueId")
    public String getUniqueId(){
        String uniqueId=idGenerator.getUniqueId();
        log.info("uniqueId: {}",uniqueId);
        return uniqueId;
    }
}
