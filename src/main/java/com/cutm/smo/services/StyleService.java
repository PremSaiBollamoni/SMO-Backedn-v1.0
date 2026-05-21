package com.cutm.smo.services;

import com.cutm.smo.models.*;
import com.cutm.smo.repositories.*;
import com.cutm.smo.util.LoggingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
public class StyleService {
    private final StyleRepository styleRepository;
    private final StyleVariantRepository styleVariantRepository;
    private final ButtonsRepository buttonsRepository;
    private final ThreadsRepository threadsRepository;

    public StyleService(StyleRepository styleRepository, StyleVariantRepository styleVariantRepository,
            ButtonsRepository buttonsRepository, ThreadsRepository threadsRepository) {
        this.styleRepository = styleRepository;
        this.styleVariantRepository = styleVariantRepository;
        this.buttonsRepository = buttonsRepository;
        this.threadsRepository = threadsRepository;
    }

    public List<Style> getAllStyles() { return styleRepository.findAll(); }
    public Style getStyleById(Long id) { return styleRepository.findById(id).orElse(null); }
    public Style createStyle(Style style) { return styleRepository.save(style); }
    public Style updateStyle(Long id, Style style) { style.setStyleId(id); return styleRepository.save(style); }
    public void deleteStyle(Long id) { styleRepository.deleteById(id); }

    public List<StyleVariant> getAllVariants() { return styleVariantRepository.findAll(); }
    public List<StyleVariant> getVariantsByStyleId(Long styleId) {
        return styleVariantRepository.findAll().stream()
                .filter(v -> v.getStyleId() != null && v.getStyleId().equals(styleId))
                .toList();
    }
    public StyleVariant getVariantById(Long id) { return styleVariantRepository.findById(id).orElse(null); }
    public StyleVariant createVariant(StyleVariant variant) { return styleVariantRepository.save(variant); }
    public StyleVariant updateVariant(Long id, StyleVariant variant) { variant.setStyleVariantId(id); return styleVariantRepository.save(variant); }
    public void deleteVariant(Long id) { styleVariantRepository.deleteById(id); }

    public List<Buttons> getAllButtons() { return buttonsRepository.findAll(); }
    public Buttons getButtonById(Long id) { return buttonsRepository.findById(id).orElse(null); }
    public Buttons createButton(Buttons button) { return buttonsRepository.save(button); }
    public Buttons updateButton(Long id, Buttons button) { button.setButtonId(id); return buttonsRepository.save(button); }
    public void deleteButton(Long id) { buttonsRepository.deleteById(id); }

    public List<Threads> getAllThreads() { return threadsRepository.findAll(); }
    public Threads getThreadById(Long id) { return threadsRepository.findById(id).orElse(null); }
    public Threads createThread(Threads thread) { return threadsRepository.save(thread); }
    public Threads updateThread(Long id, Threads thread) { thread.setThreadId(id); return threadsRepository.save(thread); }
    public void deleteThread(Long id) { threadsRepository.deleteById(id); }
}