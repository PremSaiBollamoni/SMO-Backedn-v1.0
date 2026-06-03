package com.cutm.smo.services;

import com.cutm.smo.models.*;
import com.cutm.smo.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Master Data Service - Handles CRUD operations for all master data entities
 * (Styles, GTG, Buttons, Labels, Machines, Threads)
 */
@Service
public class MasterDataService {

    @Autowired
    private StyleRepository styleRepository;

    @Autowired
    private StyleVariantRepository styleVariantRepository;

    @Autowired
    private ButtonsRepository buttonsRepository;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private MachineRepository machineRepository;

    @Autowired
    private ThreadsRepository threadsRepository;

    // ==================== STYLES (Style) ====================

    public List<Style> getAllStyles() {
        return styleRepository.findAll();
    }

    public List<Style> getActiveStyles() {
        return styleRepository.findByStatus("ACTIVE");
    }

    @Transactional
    public Map<String, Object> createStyle(Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        try {
            Style style = new Style();
            Long nextId = styleRepository.getNextStyleId();
            style.setStyleId(nextId);
            style.setStyleNo((String) data.get("styleNo"));
            style.setConcept((String) data.get("concept"));
            style.setMainLabel((String) data.get("mainLabel"));
            style.setBrandingLabel((String) data.get("brandingLabel"));
            style.setPatternImage((String) data.get("patternImage"));
            style.setDescription((String) data.get("description"));
            style.setStatus((String) data.getOrDefault("status", "ACTIVE"));
            style.setCreatedAt(LocalDateTime.now());

            Style saved = styleRepository.save(style);

            response.put("success", true);
            response.put("message", "Style created successfully");
            response.put("data", saved);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating style: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public Map<String, Object> updateStyle(Long styleId, Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<Style> styleOpt = styleRepository.findById(styleId);
            if (!styleOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Style not found");
                return response;
            }

            Style style = styleOpt.get();
            if (data.containsKey("styleNo")) style.setStyleNo((String) data.get("styleNo"));
            if (data.containsKey("concept")) style.setConcept((String) data.get("concept"));
            if (data.containsKey("mainLabel")) style.setMainLabel((String) data.get("mainLabel"));
            if (data.containsKey("brandingLabel")) style.setBrandingLabel((String) data.get("brandingLabel"));
            if (data.containsKey("patternImage")) style.setPatternImage((String) data.get("patternImage"));
            if (data.containsKey("description")) style.setDescription((String) data.get("description"));
            if (data.containsKey("status")) style.setStatus((String) data.get("status"));

            Style saved = styleRepository.save(style);

            response.put("success", true);
            response.put("message", "Style updated successfully");
            response.put("data", saved);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating style: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public Map<String, Object> deleteStyle(Long styleId) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!styleRepository.existsById(styleId)) {
                response.put("success", false);
                response.put("message", "Style not found");
                return response;
            }

            styleRepository.deleteById(styleId);

            response.put("success", true);
            response.put("message", "Style deleted successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting style: " + e.getMessage());
        }
        return response;
    }

    // ==================== GTG (StyleVariant) ====================

    public List<StyleVariant> getAllGtg() {
        return styleVariantRepository.findAll();
    }

    public List<StyleVariant> getActiveGtg() {
        return styleVariantRepository.findByStatus("ACTIVE");
    }

    @Transactional
    public Map<String, Object> createGtg(Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        try {
            // ========== MANDATORY FIELD VALIDATION ==========
            
            // Validate styleId
            if (!data.containsKey("styleId") || data.get("styleId") == null) {
                response.put("success", false);
                response.put("message", "styleId is required");
                return response;
            }
            
            // Validate buttonId (MANDATORY)
            if (!data.containsKey("buttonId") || data.get("buttonId") == null) {
                response.put("success", false);
                response.put("message", "buttonId is required - please select a button");
                return response;
            }
            
            // threadId is now OPTIONAL - removed validation

            // ========== FETCH AND VALIDATE STYLE ==========
            Long styleId = Long.parseLong(data.get("styleId").toString());
            Optional<Style> styleOpt = styleRepository.findById(styleId);
            
            if (!styleOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Style not found with ID: " + styleId);
                return response;
            }
            
            Style style = styleOpt.get();
            
            // Validate style has labels
            if (style.getMainLabel() == null || style.getMainLabel().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Style does not have main_label set. Please update the style first.");
                return response;
            }
            
            if (style.getBrandingLabel() == null || style.getBrandingLabel().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Style does not have branding_label set. Please update the style first.");
                return response;
            }

            // ========== VALIDATE BUTTON EXISTS AND ACTIVE ==========
            Long buttonId = Long.parseLong(data.get("buttonId").toString());
            Optional<Buttons> buttonOpt = buttonsRepository.findById(buttonId);
            
            if (!buttonOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Button not found with ID: " + buttonId);
                return response;
            }
            
            Buttons button = buttonOpt.get();
            if (!"ACTIVE".equalsIgnoreCase(button.getStatus())) {
                response.put("success", false);
                response.put("message", "Button is not active. Please select an active button.");
                return response;
            }

            // ========== VALIDATE THREAD (OPTIONAL) ==========
            Long threadId = null;
            if (data.containsKey("threadId") && data.get("threadId") != null) {
                threadId = Long.parseLong(data.get("threadId").toString());
                Optional<Threads> threadOpt = threadsRepository.findById(threadId);
                
                if (!threadOpt.isPresent()) {
                    response.put("success", false);
                    response.put("message", "Thread not found with ID: " + threadId);
                    return response;
                }
                
                Threads thread = threadOpt.get();
                if (!"ACTIVE".equalsIgnoreCase(thread.getStatus())) {
                    response.put("success", false);
                    response.put("message", "Thread is not active. Please select an active thread.");
                    return response;
                }
            }

            // ========== CREATE GTG WITH ALL REQUIRED FIELDS ==========
            StyleVariant variant = new StyleVariant();
            Long nextId = styleVariantRepository.getNextStyleVariantId();
            variant.setStyleVariantId(nextId);
            variant.setGtgId((String) data.get("gtgId"));
            variant.setSize((String) data.get("size"));
            variant.setColor((String) data.get("color"));
            variant.setSleeveType((String) data.get("sleeveType"));
            variant.setStatus((String) data.getOrDefault("status", "ACTIVE"));
            
            // Set mandatory foreign keys
            variant.setStyleId(styleId);
            variant.setButtonId(buttonId);
            if (threadId != null) {
                variant.setThreadId(threadId); // Only set if provided
            }
            
            // Auto-copy labels from style
            variant.setMainLabel(style.getMainLabel());
            variant.setBrandingLabel(style.getBrandingLabel());
            
            // Copy other optional fields from style if needed
            if (style.getConcept() != null) {
                variant.setConcept(style.getConcept());
            }
            if (style.getStyleNo() != null) {
                variant.setStyleNo(style.getStyleNo());
            }
            if (style.getDescription() != null) {
                variant.setDescription(style.getDescription());
            }
            if (style.getPatternImage() != null) {
                variant.setPatternImage(style.getPatternImage());
            }
            
            // Set optional numeric fields
            if (data.containsKey("consumptionPerShirt")) {
                variant.setConsumptionPerShirt(new java.math.BigDecimal(data.get("consumptionPerShirt").toString()));
            }
            if (data.containsKey("noOfShirtsTarget")) {
                variant.setNoOfShirtsTarget(Integer.parseInt(data.get("noOfShirtsTarget").toString()));
            }
            
            variant.setCreatedAt(LocalDateTime.now());

            StyleVariant saved = styleVariantRepository.save(variant);

            response.put("success", true);
            response.put("message", "GTG created successfully");
            response.put("data", saved);
        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("message", "Invalid number format: " + e.getMessage());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating GTG: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public Map<String, Object> updateGtg(Long variantId, Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<StyleVariant> variantOpt = styleVariantRepository.findById(variantId);
            if (!variantOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "GTG not found");
                return response;
            }

            StyleVariant variant = variantOpt.get();
            if (data.containsKey("gtgId")) variant.setGtgId((String) data.get("gtgId"));
            if (data.containsKey("size")) variant.setSize((String) data.get("size"));
            if (data.containsKey("color")) variant.setColor((String) data.get("color"));
            if (data.containsKey("sleeveType")) variant.setSleeveType((String) data.get("sleeveType"));
            if (data.containsKey("status")) variant.setStatus((String) data.get("status"));
            if (data.containsKey("buttonId")) variant.setButtonId(Long.parseLong(data.get("buttonId").toString()));
            if (data.containsKey("threadId")) variant.setThreadId(Long.parseLong(data.get("threadId").toString()));

            StyleVariant saved = styleVariantRepository.save(variant);

            response.put("success", true);
            response.put("message", "GTG updated successfully");
            response.put("data", saved);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating GTG: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public Map<String, Object> deleteGtg(Long variantId) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!styleVariantRepository.existsById(variantId)) {
                response.put("success", false);
                response.put("message", "GTG not found");
                return response;
            }

            styleVariantRepository.deleteById(variantId);

            response.put("success", true);
            response.put("message", "GTG deleted successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting GTG: " + e.getMessage());
        }
        return response;
    }

    // ==================== BUTTONS ====================

    public List<Buttons> getAllButtons() {
        return buttonsRepository.findAll();
    }

    public List<Buttons> getActiveButtons() {
        return buttonsRepository.findByStatus("ACTIVE");
    }

    @Transactional
    public Map<String, Object> createButton(Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        try {
            Buttons button = new Buttons();
            Long nextId = buttonsRepository.getNextButtonId();
            button.setButtonId(nextId);
            button.setButtonCode((String) data.get("buttonCode"));
            button.setButtonName((String) data.get("buttonName"));
            button.setStatus((String) data.getOrDefault("status", "ACTIVE"));

            Buttons saved = buttonsRepository.save(button);

            response.put("success", true);
            response.put("message", "Button created successfully");
            response.put("data", saved);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating button: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public Map<String, Object> updateButton(Long buttonId, Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<Buttons> buttonOpt = buttonsRepository.findById(buttonId);
            if (!buttonOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Button not found");
                return response;
            }

            Buttons button = buttonOpt.get();
            if (data.containsKey("buttonCode")) button.setButtonCode((String) data.get("buttonCode"));
            if (data.containsKey("buttonName")) button.setButtonName((String) data.get("buttonName"));
            if (data.containsKey("status")) button.setStatus((String) data.get("status"));

            Buttons saved = buttonsRepository.save(button);

            response.put("success", true);
            response.put("message", "Button updated successfully");
            response.put("data", saved);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating button: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public Map<String, Object> deleteButton(Long buttonId) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!buttonsRepository.existsById(buttonId)) {
                response.put("success", false);
                response.put("message", "Button not found");
                return response;
            }

            buttonsRepository.deleteById(buttonId);

            response.put("success", true);
            response.put("message", "Button deleted successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting button: " + e.getMessage());
        }
        return response;
    }

    // ==================== LABELS ====================

    public List<Label> getAllLabels() {
        return labelRepository.findAll();
    }

    public List<Label> getActiveLabels() {
        return labelRepository.findByStatus("ACTIVE");
    }

    @Transactional
    public Map<String, Object> createLabel(Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        try {
            Label label = new Label();
            Long nextId = labelRepository.getNextLabelId();
            label.setLabelId(nextId);
            label.setLabelCode((String) data.get("labelCode"));
            label.setLabelName((String) data.get("labelName"));
            label.setLabelType((String) data.get("labelType"));
            label.setDescription((String) data.get("description"));
            label.setStatus((String) data.getOrDefault("status", "ACTIVE"));
            label.setCreatedAt(LocalDateTime.now());

            Label saved = labelRepository.save(label);

            response.put("success", true);
            response.put("message", "Label created successfully");
            response.put("data", saved);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating label: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public Map<String, Object> updateLabel(Long labelId, Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<Label> labelOpt = labelRepository.findById(labelId);
            if (!labelOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Label not found");
                return response;
            }

            Label label = labelOpt.get();
            if (data.containsKey("labelCode")) label.setLabelCode((String) data.get("labelCode"));
            if (data.containsKey("labelName")) label.setLabelName((String) data.get("labelName"));
            if (data.containsKey("labelType")) label.setLabelType((String) data.get("labelType"));
            if (data.containsKey("description")) label.setDescription((String) data.get("description"));
            if (data.containsKey("status")) label.setStatus((String) data.get("status"));
            label.setUpdatedAt(LocalDateTime.now());

            Label saved = labelRepository.save(label);

            response.put("success", true);
            response.put("message", "Label updated successfully");
            response.put("data", saved);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating label: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public Map<String, Object> deleteLabel(Long labelId) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!labelRepository.existsById(labelId)) {
                response.put("success", false);
                response.put("message", "Label not found");
                return response;
            }

            labelRepository.deleteById(labelId);

            response.put("success", true);
            response.put("message", "Label deleted successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting label: " + e.getMessage());
        }
        return response;
    }

    // ==================== MACHINES ====================

    public List<Machine> getAllMachines() {
        return machineRepository.findAll();
    }

    public List<Machine> getActiveMachines() {
        return machineRepository.findByStatus("ACTIVE");
    }

    @Transactional
    public Map<String, Object> createMachine(Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        try {
            Machine machine = new Machine();
            machine.setMachineId((String) data.get("machineId"));
            machine.setName((String) data.get("name"));
            machine.setType((String) data.get("type"));
            machine.setStatus((String) data.getOrDefault("status", "ACTIVE"));

            Machine saved = machineRepository.save(machine);

            response.put("success", true);
            response.put("message", "Machine created successfully");
            response.put("data", saved);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating machine: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public Map<String, Object> updateMachine(String machineId, Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<Machine> machineOpt = machineRepository.findById(machineId);
            if (!machineOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Machine not found");
                return response;
            }

            Machine machine = machineOpt.get();
            if (data.containsKey("name")) machine.setName((String) data.get("name"));
            if (data.containsKey("type")) machine.setType((String) data.get("type"));
            if (data.containsKey("status")) machine.setStatus((String) data.get("status"));

            Machine saved = machineRepository.save(machine);

            response.put("success", true);
            response.put("message", "Machine updated successfully");
            response.put("data", saved);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating machine: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public Map<String, Object> deleteMachine(String machineId) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!machineRepository.existsById(machineId)) {
                response.put("success", false);
                response.put("message", "Machine not found");
                return response;
            }

            machineRepository.deleteById(machineId);

            response.put("success", true);
            response.put("message", "Machine deleted successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting machine: " + e.getMessage());
        }
        return response;
    }

    // ==================== THREADS ====================

    public List<Threads> getAllThreads() {
        return threadsRepository.findAll();
    }

    public List<Threads> getActiveThreads() {
        return threadsRepository.findByStatus("ACTIVE");
    }

    @Transactional
    public Map<String, Object> createThread(Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        try {
            Threads thread = new Threads();
            Long nextId = threadsRepository.getNextThreadId();
            thread.setThreadId(nextId);
            thread.setThreadCode((String) data.get("threadCode"));
            thread.setThreadName((String) data.get("threadName"));
            thread.setColorCode((String) data.get("colorCode"));
            thread.setStatus((String) data.getOrDefault("status", "ACTIVE"));

            Threads saved = threadsRepository.save(thread);

            response.put("success", true);
            response.put("message", "Thread created successfully");
            response.put("data", saved);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating thread: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public Map<String, Object> updateThread(Long threadId, Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<Threads> threadOpt = threadsRepository.findById(threadId);
            if (!threadOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Thread not found");
                return response;
            }

            Threads thread = threadOpt.get();
            if (data.containsKey("threadCode")) thread.setThreadCode((String) data.get("threadCode"));
            if (data.containsKey("threadName")) thread.setThreadName((String) data.get("threadName"));
            if (data.containsKey("colorCode")) thread.setColorCode((String) data.get("colorCode"));
            if (data.containsKey("status")) thread.setStatus((String) data.get("status"));

            Threads saved = threadsRepository.save(thread);

            response.put("success", true);
            response.put("message", "Thread updated successfully");
            response.put("data", saved);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating thread: " + e.getMessage());
        }
        return response;
    }

    @Transactional
    public Map<String, Object> deleteThread(Long threadId) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (!threadsRepository.existsById(threadId)) {
                response.put("success", false);
                response.put("message", "Thread not found");
                return response;
            }

            threadsRepository.deleteById(threadId);

            response.put("success", true);
            response.put("message", "Thread deleted successfully");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting thread: " + e.getMessage());
        }
        return response;
    }
}
