-- Migration: Populate owner_type for existing form templates
-- Date: 2026-01-21
-- Description: Sets owner_type based on template purpose
--   - Patient consent forms → PATIENT (owner must sign)
--   - Doctor consultation forms → DOCTOR (owner must sign)

-- Patient-owned templates (consent forms that patients sign)
UPDATE form_templates 
SET owner_type = 'PATIENT' 
WHERE name IN (
    'Formular de Consimțământ GDPR',
    'Consimțământ Dermapen'
) AND (owner_type IS NULL OR owner_type = '');

-- Doctor-owned templates (consultation forms that doctors sign)
UPDATE form_templates 
SET owner_type = 'DOCTOR' 
WHERE name IN (
    'Formular Vizită Dermatologie Generală'
) AND (owner_type IS NULL OR owner_type = '');

-- Fallback: Set any remaining NULL owner_type to PATIENT (safe default)
UPDATE form_templates 
SET owner_type = 'PATIENT' 
WHERE owner_type IS NULL OR owner_type = '';

-- Verify the migration
SELECT id, name, owner_type, active 
FROM form_templates 
ORDER BY name;
