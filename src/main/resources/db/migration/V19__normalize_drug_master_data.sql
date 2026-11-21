-- Normalize imported drug data without changing product or ingredient identifiers.
UPDATE `ingredient_atc_map`
SET `atc_code` = REPLACE(REPLACE(`atc_code`, CHAR(13), ''), CHAR(10), '')
WHERE INSTR(`atc_code`, CHAR(13)) > 0
   OR INSTR(`atc_code`, CHAR(10)) > 0;

UPDATE `drug_master`
SET `drug_name` = REPLACE(
    REPLACE(
        REPLACE(
            REPLACE(
                REPLACE(
                    REPLACE(
                        REPLACE(`drug_name`, CONCAT(CHAR(13), CHAR(10)), ' '),
                        CHAR(13), ' '
                    ),
                    CHAR(10), ' '
                ),
                '밀리그램', 'mg'
            ),
            '밀리그람', 'mg'
        ),
        '밀리리터', 'ml'
    ),
    'mL', 'ml'
)
WHERE INSTR(`drug_name`, CHAR(13)) > 0
   OR INSTR(`drug_name`, CHAR(10)) > 0
   OR `drug_name` LIKE '%밀리그램%'
   OR `drug_name` LIKE '%밀리그람%'
   OR `drug_name` LIKE '%밀리리터%'
   OR BINARY `drug_name` LIKE BINARY '%mL%';
