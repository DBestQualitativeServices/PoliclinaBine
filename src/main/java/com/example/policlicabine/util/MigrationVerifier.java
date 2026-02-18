package com.example.policlicabine.util;

import java.sql.*;

/**
 * Standalone migration verification utility.
 * Run this after starting the application to verify V2 migration success.
 *
 * Usage: java -cp target/classes com.example.policlicabine.util.MigrationVerifier
 */
public class MigrationVerifier {

    private static final String DB_URL = "jdbc:postgresql://dbest-db.postgres.database.azure.com:5432/polbine?sslmode=require";
    private static final String DB_USER = "dragos";
    private static final String DB_PASSWORD = "Robert123";

    public static void main(String[] args) {
        System.out.println("===== V2 MIGRATION VERIFICATION =====\n");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("✅ Connected to Azure PostgreSQL database\n");

            // 1. Check Flyway schema history
            checkFlywayHistory(conn);

            // 2. Verify columns exist
            verifyColumnsExist(conn);

            // 3. Check patients statistics
            checkPatientStatistics(conn);

            // 4. Check doctors statistics
            checkDoctorStatistics(conn);

            // 5. Show sample records
            showSampleRecords(conn);

            System.out.println("\n===== VERIFICATION COMPLETE =====");

        } catch (SQLException e) {
            System.err.println("❌ Database connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void checkFlywayHistory(Connection conn) throws SQLException {
        System.out.println("1. Checking Flyway schema history for V2 migration...");
        String sql = "SELECT version, description, installed_on, success " +
                     "FROM flyway_schema_history " +
                     "WHERE version = '2' " +
                     "ORDER BY installed_rank DESC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (!rs.next()) {
                System.out.println("   ⚠️  V2 migration NOT FOUND in flyway_schema_history");
                System.out.println("   ℹ️  This means the application hasn't been started yet or migration failed");
            } else {
                do {
                    System.out.println("   ✅ V2 Migration Found:");
                    System.out.println("      - Version: " + rs.getString("version"));
                    System.out.println("      - Description: " + rs.getString("description"));
                    System.out.println("      - Installed: " + rs.getTimestamp("installed_on"));
                    System.out.println("      - Success: " + rs.getBoolean("success"));
                } while (rs.next());
            }
        }
        System.out.println();
    }

    private static void verifyColumnsExist(Connection conn) throws SQLException {
        System.out.println("2. Verifying data_nastere columns exist...");
        String sql = "SELECT table_name, column_name, data_type, is_nullable " +
                     "FROM information_schema.columns " +
                     "WHERE table_schema = 'public' " +
                     "  AND table_name IN ('patients', 'doctors') " +
                     "  AND column_name = 'data_nastere' " +
                     "ORDER BY table_name";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (!rs.next()) {
                System.out.println("   ⚠️  data_nastere columns NOT FOUND in patients/doctors tables");
            } else {
                do {
                    System.out.printf("   ✅ Column found: %s.%s (%s, nullable: %s)%n",
                            rs.getString("table_name"),
                            rs.getString("column_name"),
                            rs.getString("data_type"),
                            rs.getString("is_nullable"));
                } while (rs.next());
            }
        }
        System.out.println();
    }

    private static void checkPatientStatistics(Connection conn) throws SQLException {
        System.out.println("3. Checking patients backfill statistics...");
        String sql = "SELECT " +
                     "    COUNT(*) as total_patients, " +
                     "    COUNT(cnp) as patients_with_cnp, " +
                     "    COUNT(data_nastere) as patients_with_birthdate, " +
                     "    COUNT(CASE WHEN cnp IS NOT NULL AND data_nastere IS NULL THEN 1 END) as patients_with_cnp_missing_birthdate " +
                     "FROM patients";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                System.out.println("   📊 Patient Statistics:");
                System.out.println("      - Total patients: " + rs.getLong("total_patients"));
                System.out.println("      - Patients with CNP: " + rs.getLong("patients_with_cnp"));
                System.out.println("      - Patients with birth date: " + rs.getLong("patients_with_birthdate"));

                long missing = rs.getLong("patients_with_cnp_missing_birthdate");
                System.out.println("      - Patients with CNP but missing birth date: " + missing);

                if (missing > 0) {
                    System.out.println("      ⚠️  " + missing + " patients have CNP but no birth date (invalid CNP format?)");
                } else if (rs.getLong("patients_with_cnp") > 0) {
                    System.out.println("      ✅ All patients with CNP have birth dates calculated!");
                }
            }
        }
        System.out.println();
    }

    private static void checkDoctorStatistics(Connection conn) throws SQLException {
        System.out.println("4. Checking doctors backfill statistics...");
        String sql = "SELECT " +
                     "    COUNT(*) as total_doctors, " +
                     "    COUNT(cnp) as doctors_with_cnp, " +
                     "    COUNT(data_nastere) as doctors_with_birthdate, " +
                     "    COUNT(CASE WHEN cnp IS NOT NULL AND data_nastere IS NULL THEN 1 END) as doctors_with_cnp_missing_birthdate " +
                     "FROM doctors";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                System.out.println("   📊 Doctor Statistics:");
                System.out.println("      - Total doctors: " + rs.getLong("total_doctors"));
                System.out.println("      - Doctors with CNP: " + rs.getLong("doctors_with_cnp"));
                System.out.println("      - Doctors with birth date: " + rs.getLong("doctors_with_birthdate"));

                long missing = rs.getLong("doctors_with_cnp_missing_birthdate");
                System.out.println("      - Doctors with CNP but missing birth date: " + missing);

                if (missing > 0) {
                    System.out.println("      ⚠️  " + missing + " doctors have CNP but no birth date (invalid CNP format?)");
                } else if (rs.getLong("doctors_with_cnp") > 0) {
                    System.out.println("      ✅ All doctors with CNP have birth dates calculated!");
                }
            }
        }
        System.out.println();
    }

    private static void showSampleRecords(Connection conn) throws SQLException {
        System.out.println("5. Sample patients with calculated birth dates:");
        String patientsSql = "SELECT first_name, last_name, cnp, data_nastere " +
                            "FROM patients " +
                            "WHERE data_nastere IS NOT NULL " +
                            "ORDER BY registration_date DESC " +
                            "LIMIT 5";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(patientsSql)) {

            if (!rs.next()) {
                System.out.println("   No patients with birth dates found");
            } else {
                do {
                    String cnp = rs.getString("cnp");
                    String maskedCnp = cnp != null && cnp.length() >= 3 ? cnp.substring(0, 3) + "**********" : cnp;
                    System.out.printf("   - %s %s (CNP: %s, Birth Date: %s)%n",
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            maskedCnp,
                            rs.getDate("data_nastere"));
                } while (rs.next());
            }
        }

        System.out.println("\n6. Sample doctors with calculated birth dates:");
        String doctorsSql = "SELECT full_name, cnp, data_nastere " +
                           "FROM doctors " +
                           "WHERE data_nastere IS NOT NULL " +
                           "LIMIT 5";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(doctorsSql)) {

            if (!rs.next()) {
                System.out.println("   No doctors with birth dates found");
            } else {
                do {
                    String cnp = rs.getString("cnp");
                    String maskedCnp = cnp != null && cnp.length() >= 3 ? cnp.substring(0, 3) + "**********" : cnp;
                    System.out.printf("   - %s (CNP: %s, Birth Date: %s)%n",
                            rs.getString("full_name"),
                            maskedCnp,
                            rs.getDate("data_nastere"));
                } while (rs.next());
            }
        }
    }
}
