package uk.ac.ed.inf.ilpRestServer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SubmissionAnalyzerController {
    @GetMapping("/")
    public String homepage() {
        return "submissionAnalyzer";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }


}
