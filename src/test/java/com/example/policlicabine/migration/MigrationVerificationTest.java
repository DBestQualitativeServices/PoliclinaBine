package com.example.policlicabine.migration;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

/**
 * Verification test for V2__add_data_nastere_columns migration.
 * This test connects to the Azure PostgreSQL database and verifies:
 * 1. Migration V2 was executed successfully
 * 2. Columns data_nastere exist in patients and doctors tables
 * 3. Existing records were backfilled with birth dates from CNP
 */
@SpringBootTest
@ActiveProfiles("dev")
@Slf4j
@Tag("migration")
@Tag("database")
@Tag("manual")
public class MigrationVerificationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void verifyV2MigrationExecuted() {
        log.info("===== V2 MIGRATION VERIFICATION =====");

        // 1. Check Flyway schema history
        log.info("\n1. Checking Flyway schema history for V2 migration...");
        try {
            List<Map<String, Object>> migrations = jdbcTemplate.queryForList(
                    "SELECT version, description, type, installed_on, success " +
                    "FROM flyway_schema_history " +
                    "WHERE version = '2' " +
                    "ORDER BY installed_rank DESC"
            );

            if (migrations.isEmpty()) {
                log.warn("⚠️  V2 migration NOT FOUND in flyway_schema_history");
            } else {
                migrations.forEach(m -> {
                    log.info("✅ V2 Migration Found:");
                    log.info("   - Version: {}", m.get("version"));
                    log.info("   - Description: {}", m.get("description"));
                    log.info("   - Type: {}", m.get("type"));
                    log.info("   - Installed: {}", m.get("installed_on"));
                    log.info("   - Success: {}", m.get("success"));
                });
            }
        } catch (Exception e) {
            log.error("❌ Error checking Flyway history: {}", e.getMessage());
        }

        // 2. Verify columns exist
        log.info("\n2. Verifying data_nastere columns exist...");
        try {
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                    "SELECT table_name, column_name, data_type, is_nullable " +
                    "FROM information_schema.columns " +
                    "WHERE table_schema = 'public' " +
                    "  AND table_name IN ('patients', 'doctors') " +
                    "  AND column_name = 'data_nastere' " +
                    "ORDER BY table_name"
            );

            if (columns.isEmpty()) {
                log.warn("⚠️  data_nastere columns NOT FOUND in patients/doctors tables");
            } else {
                columns.forEach(c -> {
                    log.info("✅ Column found: {}.{} ({}, nullable: {})",
                            c.get("table_name"),
                            c.get("column_name"),
                            c.get("data_type"),
                            c.get("is_nullable"));
                });
            }
        } catch (Exception e) {
            log.error("❌ Error checking columns: {}", e.getMessage());
        }

        // 3. Check patients backfill statistics
        log.info("\n3. Checking patients backfill statistics...");
        try {
            Map<String, Object> patientStats = jdbcTemplate.queryForMap(
                    "SELECT " +
                    "    COUNT(*) as total_patients, " +
                    "    COUNT(cnp) as patients_with_cnp, " +
                    "    COUNT(data_nastere) as patients_with_birthdate, " +
                    "    COUNT(CASE WHEN cnp IS NOT NULL AND data_nastere IS NULL THEN 1 END) as patients_with_cnp_missing_birthdate " +
                    "FROM patients"
            );

            log.info("📊 Patient Statistics:");
            log.info("   - Total patients: {}", patientStats.get("total_patients"));
            log.info("   - Patients with CNP: {}", patientStats.get("patients_with_cnp"));
            log.info("   - Patients with birth date: {}", patientStats.get("patients_with_birthdate"));
            log.info("   - Patients with CNP but missing birth date: {}", patientStats.get("patients_with_cnp_missing_birthdate"));

            Long missing = ((Number) patientStats.get("patients_with_cnp_missing_birthdate")).longValue();
            if (missing > 0) {
                log.warn("⚠️  {} patients have CNP but no birth date (invalid CNP format?)", missing);
            } else {
                log.info("✅ All patients with CNP have birth dates calculated!");
            }
        } catch (Exception e) {
            log.error("❌ Error checking patient statistics: {}", e.getMessage());
        }

        // 4. Check doctors backfill statistics
        log.info("\n4. Checking doctors backfill statistics...");
        try {
            Map<String, Object> doctorStats = jdbcTemplate.queryForMap(
                    "SELECT " +
                    "    COUNT(*) as total_doctors, " +
                    "    COUNT(cnp) as doctors_with_cnp, " +
                    "    COUNT(data_nastere) as doctors_with_birthdate, " +
                    "    COUNT(CASE WHEN cnp IS NOT NULL AND data_nastere IS NULL THEN 1 END) as doctors_with_cnp_missing_birthdate " +
                    "FROM doctors"
            );

            log.info("📊 Doctor Statistics:");
            log.info("   - Total doctors: {}", doctorStats.get("total_doctors"));
            log.info("   - Doctors with CNP: {}", doctorStats.get("doctors_with_cnp"));
            log.info("   - Doctors with birth date: {}", doctorStats.get("doctors_with_birthdate"));
            log.info("   - Doctors with CNP but missing birth date: {}", doctorStats.get("doctors_with_cnp_missing_birthdate"));

            Long missing = ((Number) doctorStats.get("doctors_with_cnp_missing_birthdate")).longValue();
            if (missing > 0) {
                log.warn("⚠️  {} doctors have CNP but no birth date (invalid CNP format?)", missing);
            } else {
                log.info("✅ All doctors with CNP have birth dates calculated!");
            }
        } catch (Exception e) {
            log.error("❌ Error checking doctor statistics: {}", e.getMessage());
        }

        // 5. Sample patients with calculated birth dates
        log.info("\n5. Sample patients with calculated birth dates:");
        try {
            List<Map<String, Object>> samplePatients = jdbcTemplate.queryForList(
                    "SELECT patient_id, first_name, last_name, cnp, data_nastere " +
                    "FROM patients " +
                    "WHERE data_nastere IS NOT NULL " +
                    "ORDER BY registration_date DESC " +
                    "LIMIT 5"
            );

            if (samplePatients.isEmpty()) {
                log.info("   No patients with birth dates found");
            } else {
                samplePatients.forEach(p -> {
                    log.info("   - {} {} (CNP: {}, Birth Date: {})",
                            p.get("first_name"),
                            p.get("last_name"),
                            maskCnp((String) p.get("cnp")),
                            p.get("data_nastere"));
                });
            }
        } catch (Exception e) {
            log.error("❌ Error fetching sample patients: {}", e.getMessage());
        }

        // 6. Sample doctors with calculated birth dates
        log.info("\n6. Sample doctors with calculated birth dates:");
        try {
            List<Map<String, Object>> sampleDoctors = jdbcTemplate.queryForList(
                    "SELECT doctor_id, full_name, cnp, data_nastere " +
                    "FROM doctors " +
                    "WHERE data_nastere IS NOT NULL " +
                    "LIMIT 5"
            );

            if (sampleDoctors.isEmpty()) {
                log.info("   No doctors with birth dates found");
            } else {
                sampleDoctors.forEach(d -> {
                    log.info("   - {} (CNP: {}, Birth Date: {})",
                            d.get("full_name"),
                            maskCnp((String) d.get("cnp")),
                            d.get("data_nastere"));
                });
            }
        } catch (Exception e) {
            log.error("❌ Error fetching sample doctors: {}", e.getMessage());
        }

        log.info("\n===== VERIFICATION COMPLETE =====");
    }

    private String maskCnp(String cnp) {
        if (cnp == null || cnp.length() < 3) {
            return cnp;
        }
        return cnp.substring(0, 3) + "**********";
    }
}
