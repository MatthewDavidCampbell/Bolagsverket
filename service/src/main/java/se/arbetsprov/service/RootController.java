package se.arbetsprov.service;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Sends a browser opening the root address to Swagger UI. */
@Controller
@Hidden
class RootController {

    @GetMapping("/")
    String root() {
        return "redirect:/swagger-ui.html";
    }
}
