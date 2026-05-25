package com.cutm.smo.repositories;

import com.cutm.smo.models.Buttons;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ButtonsRepository extends JpaRepository<Buttons, Long> {
    
    List<Buttons> findByStatus(String status);
    
    @Query("SELECT COALESCE(MAX(b.buttonId), 0) + 1 FROM Buttons b")
    Long getNextButtonId();
}
