package src.main.java.com.cadfancode.Backend.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
public class Controller {

    @GetMapping("/api/test")
    public Map<String, String> getTestMessage() {
        HashMap<String, String> map = new HashMap<>();
        map.put("message", "Cais app");
        return map;
    }
}