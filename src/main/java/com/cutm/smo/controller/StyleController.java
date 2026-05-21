package com.cutm.smo.controller;

import com.cutm.smo.models.*;
import com.cutm.smo.services.*;
import com.cutm.smo.util.LoggingUtil;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/style")
@CrossOrigin(origins = "*")
public class StyleController {
    private final StyleService styleService;

    public StyleController(StyleService styleService) { this.styleService = styleService; }
    
    private <T> T executeWithLogging(String operationName, String entityType, Long id, java.util.function.Supplier<T> operation) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("=== {} START ===", operationName);
            if (id != null) log.debug("{} ID: {}", entityType, id);
            T result = operation.get();
            long endTime = System.currentTimeMillis();
            LoggingUtil.logPerformance(log, operationName, startTime, endTime);
            log.info("=== {} END - SUCCESS ===", operationName);
            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            LoggingUtil.logError(log, "Failed to execute " + operationName, e);
            LoggingUtil.logPerformance(log, operationName + " (Failed)", startTime, endTime);
            throw e;
        }
    }

    @GetMapping
    public List<Style> getAllStyles() { 
        return executeWithLogging("GET ALL STYLES", "Style", null, () -> styleService.getAllStyles()); 
    }

    @GetMapping("/{id}")
    public Style getStyleById(@PathVariable Long id) { 
        return executeWithLogging("GET STYLE BY ID", "Style", id, () -> styleService.getStyleById(id)); 
    }

    @PostMapping
    public Style createStyle(@RequestBody Style style) { 
        return executeWithLogging("CREATE STYLE", "Style", null, () -> styleService.createStyle(style)); 
    }

    @PutMapping("/{id}")
    public Style updateStyle(@PathVariable Long id, @RequestBody Style style) { 
        return executeWithLogging("UPDATE STYLE", "Style", id, () -> styleService.updateStyle(id, style)); 
    }

    @DeleteMapping("/{id}")
    public void deleteStyle(@PathVariable Long id) { 
        executeWithLogging("DELETE STYLE", "Style", id, () -> { styleService.deleteStyle(id); return null; }); 
    }

    @GetMapping("/variants")
    public List<StyleVariant> getAllVariants() { 
        return executeWithLogging("GET ALL STYLE VARIANTS", "StyleVariant", null, () -> styleService.getAllVariants()); 
    }

    @GetMapping("/variants/{id}")
    public StyleVariant getVariantById(@PathVariable Long id) { 
        return executeWithLogging("GET STYLE VARIANT BY ID", "StyleVariant", id, () -> styleService.getVariantById(id)); 
    }

    @GetMapping("/{styleId}/variants")
    public List<StyleVariant> getVariantsByStyleId(@PathVariable Long styleId) { 
        return executeWithLogging("GET STYLE VARIANTS BY STYLE ID", "StyleVariant", styleId, () -> styleService.getVariantsByStyleId(styleId)); 
    }

    @PostMapping("/variants")
    public StyleVariant createVariant(@RequestBody StyleVariant variant) { 
        return executeWithLogging("CREATE STYLE VARIANT", "StyleVariant", null, () -> styleService.createVariant(variant)); 
    }

    @PutMapping("/variants/{id}")
    public StyleVariant updateVariant(@PathVariable Long id, @RequestBody StyleVariant variant) { 
        return executeWithLogging("UPDATE STYLE VARIANT", "StyleVariant", id, () -> styleService.updateVariant(id, variant)); 
    }

    @DeleteMapping("/variants/{id}")
    public void deleteVariant(@PathVariable Long id) { 
        executeWithLogging("DELETE STYLE VARIANT", "StyleVariant", id, () -> { styleService.deleteVariant(id); return null; }); 
    }

    @GetMapping("/buttons")
    public List<Buttons> getAllButtons() { 
        return executeWithLogging("GET ALL BUTTONS", "Buttons", null, () -> styleService.getAllButtons()); 
    }

    @GetMapping("/buttons/{id}")
    public Buttons getButtonById(@PathVariable Long id) { 
        return executeWithLogging("GET BUTTON BY ID", "Buttons", id, () -> styleService.getButtonById(id)); 
    }

    @PostMapping("/buttons")
    public Buttons createButton(@RequestBody Buttons button) { 
        return executeWithLogging("CREATE BUTTON", "Buttons", null, () -> styleService.createButton(button)); 
    }

    @PutMapping("/buttons/{id}")
    public Buttons updateButton(@PathVariable Long id, @RequestBody Buttons button) { 
        return executeWithLogging("UPDATE BUTTON", "Buttons", id, () -> styleService.updateButton(id, button)); 
    }

    @DeleteMapping("/buttons/{id}")
    public void deleteButton(@PathVariable Long id) { 
        executeWithLogging("DELETE BUTTON", "Buttons", id, () -> { styleService.deleteButton(id); return null; }); 
    }

    @GetMapping("/threads")
    public List<Threads> getAllThreads() { 
        return executeWithLogging("GET ALL THREADS", "Threads", null, () -> styleService.getAllThreads()); 
    }

    @GetMapping("/threads/{id}")
    public Threads getThreadById(@PathVariable Long id) { 
        return executeWithLogging("GET THREAD BY ID", "Threads", id, () -> styleService.getThreadById(id)); 
    }

    @PostMapping("/threads")
    public Threads createThread(@RequestBody Threads thread) { 
        return executeWithLogging("CREATE THREAD", "Threads", null, () -> styleService.createThread(thread)); 
    }

    @PutMapping("/threads/{id}")
    public Threads updateThread(@PathVariable Long id, @RequestBody Threads thread) { 
        return executeWithLogging("UPDATE THREAD", "Threads", id, () -> styleService.updateThread(id, thread)); 
    }

    @DeleteMapping("/threads/{id}")
    public void deleteThread(@PathVariable Long id) { 
        executeWithLogging("DELETE THREAD", "Threads", id, () -> { styleService.deleteThread(id); return null; }); 
    }
}