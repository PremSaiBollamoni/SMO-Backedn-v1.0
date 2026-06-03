package com.cutm.smo.services;

import com.cutm.smo.dto.DailyLedgerDto;
import com.cutm.smo.dto.OperationStockView;
import com.cutm.smo.dto.StockLimitRequest;
import com.cutm.smo.models.DailyStockLedger;
import com.cutm.smo.models.OperationStockLimit;
import com.cutm.smo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class InventoryService {

    @Autowired
    private OperationStockLimitRepository stockLimitRepository;

    @Autowired
    private DailyStockLedgerRepository dailyLedgerRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Get all operation stock view (actual vs target)
    public List<OperationStockView> getOperationStockView() {
        String sql = "SELECT * FROM v_operation_current_stock WHERE sequence > 0 ORDER BY sequence";
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            OperationStockView view = new OperationStockView();
            view.setOperationId(rs.getLong("operation_id"));
            view.setOperationName(rs.getString("operation_name"));
            view.setSequence(rs.getInt("sequence"));
            view.setBinCount(rs.getInt("bin_count"));
            view.setActualQty(rs.getInt("actual_qty"));
            view.setMinTarget(rs.getInt("min_target"));
            view.setMaxTarget(rs.getInt("max_target"));
            view.setVarianceFromMin(rs.getInt("variance_from_min"));
            view.setSpaceRemaining(rs.getInt("space_remaining"));
            view.setStockStatus(rs.getString("stock_status"));
            return view;
        });
    }

    // Get operation stock view filtered by routing
    public List<OperationStockView> getOperationStockViewByRouting(Long routingId) {
        String sql = "SELECT DISTINCT v.* FROM v_operation_current_stock v " +
                     "WHERE v.operation_id IN ( " +
                     "  SELECT DISTINCT from_operation_id FROM routing_edge WHERE routing_id = ? " +
                     "  UNION " +
                     "  SELECT DISTINCT to_operation_id FROM routing_edge WHERE routing_id = ? " +
                     ") " +
                     "ORDER BY v.operation_name";
        
        return jdbcTemplate.query(sql, new Object[]{routingId, routingId}, (rs, rowNum) -> {
            OperationStockView view = new OperationStockView();
            view.setOperationId(rs.getLong("operation_id"));
            view.setOperationName(rs.getString("operation_name"));
            view.setSequence(rs.getInt("sequence"));
            view.setBinCount(rs.getInt("bin_count"));
            view.setActualQty(rs.getInt("actual_qty"));
            view.setMinTarget(rs.getInt("min_target"));
            view.setMaxTarget(rs.getInt("max_target"));
            view.setVarianceFromMin(rs.getInt("variance_from_min"));
            view.setSpaceRemaining(rs.getInt("space_remaining"));
            view.setStockStatus(rs.getString("stock_status"));
            return view;
        });
    }

    // Get all stock limits
    public List<OperationStockLimit> getAllStockLimits() {
        return stockLimitRepository.findAll();
    }

    // Get stock limit by operation ID
    public OperationStockLimit getStockLimitByOperationId(Long operationId) {
        return stockLimitRepository.findByOperationId(operationId).orElse(null);
    }

    // Create or update stock limit
    @Transactional
    public OperationStockLimit saveStockLimit(StockLimitRequest request) {
        OperationStockLimit limit = stockLimitRepository.findByOperationId(request.getOperationId())
                .orElse(new OperationStockLimit());
        
        limit.setOperationId(request.getOperationId());
        limit.setMinQtyPerDay(request.getMinQtyPerDay());
        limit.setMaxQtyPerDay(request.getMaxQtyPerDay());
        limit.setMinQtyPerMonth(request.getMinQtyPerMonth());
        limit.setMaxQtyPerMonth(request.getMaxQtyPerMonth());
        
        // Auto-calculate thresholds if not provided
        if (request.getLowStockThreshold() != null) {
            limit.setLowStockThreshold(request.getLowStockThreshold());
        } else {
            // Default: 80% of min daily
            limit.setLowStockThreshold((int) (request.getMinQtyPerDay() * 0.8));
        }
        
        if (request.getHighStockThreshold() != null) {
            limit.setHighStockThreshold(request.getHighStockThreshold());
        } else {
            // Default: 90% of max daily
            limit.setHighStockThreshold((int) (request.getMaxQtyPerDay() * 0.9));
        }
        
        limit.setUnit(request.getUnit() != null ? request.getUnit() : "PIECES");
        limit.setIsActive(true);
        
        return stockLimitRepository.save(limit);
    }

    // Delete stock limit
    @Transactional
    public void deleteStockLimit(Long operationId) {
        stockLimitRepository.deleteByOperationId(operationId);
    }

    // Get daily ledger for a date
    public List<DailyLedgerDto> getDailyLedger(LocalDate date) {
        String sql = "SELECT " +
                     "  dl.ledger_id, " +
                     "  dl.ledger_date, " +
                     "  dl.operation_id, " +
                     "  o.name as operation_name, " +
                     "  dl.opening_stock, " +
                     "  dl.received_qty, " +
                     "  dl.issued_qty, " +
                     "  dl.adjusted_qty, " +
                     "  dl.closing_stock, " +
                     "  dl.unit, " +
                     "  dl.stock_status, " +
                     "  osl.min_qty_per_day as min_target, " +
                     "  osl.max_qty_per_day as max_target " +
                     "FROM daily_stock_ledger dl " +
                     "JOIN operation o ON dl.operation_id = o.operation_id " +
                     "LEFT JOIN operation_stock_limits osl ON dl.operation_id = osl.operation_id " +
                     "WHERE dl.ledger_date = ? " +
                     "ORDER BY o.sequence";
        
        return jdbcTemplate.query(sql, new Object[]{date}, (rs, rowNum) -> {
            DailyLedgerDto dto = new DailyLedgerDto();
            dto.setLedgerId(rs.getLong("ledger_id"));
            dto.setLedgerDate(rs.getDate("ledger_date").toLocalDate());
            dto.setOperationId(rs.getLong("operation_id"));
            dto.setOperationName(rs.getString("operation_name"));
            dto.setOpeningStock(rs.getInt("opening_stock"));
            dto.setReceivedQty(rs.getInt("received_qty"));
            dto.setIssuedQty(rs.getInt("issued_qty"));
            dto.setAdjustedQty(rs.getInt("adjusted_qty"));
            dto.setClosingStock(rs.getInt("closing_stock"));
            dto.setUnit(rs.getString("unit"));
            dto.setStockStatus(rs.getString("stock_status"));
            dto.setMinTarget(rs.getInt("min_target"));
            dto.setMaxTarget(rs.getInt("max_target"));
            return dto;
        });
    }

    // Get daily ledger for date range
    public List<DailyLedgerDto> getDailyLedgerRange(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT " +
                     "  dl.ledger_id, " +
                     "  dl.ledger_date, " +
                     "  dl.operation_id, " +
                     "  o.name as operation_name, " +
                     "  dl.opening_stock, " +
                     "  dl.received_qty, " +
                     "  dl.issued_qty, " +
                     "  dl.adjusted_qty, " +
                     "  dl.closing_stock, " +
                     "  dl.unit, " +
                     "  dl.stock_status, " +
                     "  osl.min_qty_per_day as min_target, " +
                     "  osl.max_qty_per_day as max_target " +
                     "FROM daily_stock_ledger dl " +
                     "JOIN operation o ON dl.operation_id = o.operation_id " +
                     "LEFT JOIN operation_stock_limits osl ON dl.operation_id = osl.operation_id " +
                     "WHERE dl.ledger_date BETWEEN ? AND ? " +
                     "ORDER BY dl.ledger_date DESC, o.sequence";
        
        return jdbcTemplate.query(sql, new Object[]{startDate, endDate}, (rs, rowNum) -> {
            DailyLedgerDto dto = new DailyLedgerDto();
            dto.setLedgerId(rs.getLong("ledger_id"));
            dto.setLedgerDate(rs.getDate("ledger_date").toLocalDate());
            dto.setOperationId(rs.getLong("operation_id"));
            dto.setOperationName(rs.getString("operation_name"));
            dto.setOpeningStock(rs.getInt("opening_stock"));
            dto.setReceivedQty(rs.getInt("received_qty"));
            dto.setIssuedQty(rs.getInt("issued_qty"));
            dto.setAdjustedQty(rs.getInt("adjusted_qty"));
            dto.setClosingStock(rs.getInt("closing_stock"));
            dto.setUnit(rs.getString("unit"));
            dto.setStockStatus(rs.getString("stock_status"));
            dto.setMinTarget(rs.getInt("min_target"));
            dto.setMaxTarget(rs.getInt("max_target"));
            return dto;
        });
    }

    // Get operations with their names for dropdown
    public List<Map<String, Object>> getOperationsForDropdown() {
        String sql = "SELECT operation_id, name, sequence FROM operation WHERE sequence > 0 ORDER BY sequence";
        return jdbcTemplate.queryForList(sql);
    }

    // Get approved routings for dropdown
    public List<Map<String, Object>> getRoutingsForDropdown() {
        String sql = "SELECT r.routing_id, r.version, p.name as product_name " +
                     "FROM routing r " +
                     "LEFT JOIN product p ON r.product_id = p.product_id " +
                     "WHERE r.approval_status = 'APPROVED' " +
                     "ORDER BY r.routing_id DESC";
        return jdbcTemplate.queryForList(sql);
    }
}
