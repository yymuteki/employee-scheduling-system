package com.schedule.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards known frontend routes to index.html so React Router can handle them.
 * Explicit route list avoids regex matching issues with root path and static resources.
 */
@Controller
public class SpaForwardController {

    @RequestMapping({
        "/requirements",
        "/schedule",
        "/admin/requirements",
        "/admin/generate",
        "/admin/schedule"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
