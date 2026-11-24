-- =====================================================================
-- Schema tests T-01 .. T-16 (docs/architecture/arquitectura.md section 6)
-- Target: docs/architecture/V1__esquema.propuesta.sql (PostgreSQL 18)
-- Run against throwaway database fp_qa_test only.
--
-- Conventions:
--   * Each sub-case is a DO block. Sub-cases that expect an error print
--     PASS when the expected condition is raised, FAIL otherwise.
--   * Sub-cases that expect success print PASS when the insert is accepted,
--     FAIL with SQLSTATE otherwise.
--   * Every sub-case ends with a rollback (custom SQLSTATE P9001) so that
--     test writes do not leak into other sub-cases. Fixtures created in the
--     SETUP section are committed and stay for all tests.
--   * Default categories are looked up by icono (ASCII) to avoid encoding
--     issues with accented names.
--   * Unexpected SQLSTATEs are reported as FAIL with the SQLSTATE and message.
-- =====================================================================

-- ---------------------------------------------------------------- SETUP
-- Fixed UUIDs: user A, user B, user C (performance), own categories, fixtures.
DO $$
BEGIN
    INSERT INTO usuario (id, nombre, correo, hash_contrasena) VALUES
        ('aaaaaaaa-0000-0000-0000-000000000001', 'Ana QA',   'ana@qa.test',   'hash-qa-a'),
        ('bbbbbbbb-0000-0000-0000-000000000002', 'Beto QA',  'beto@qa.test',  'hash-qa-b'),
        ('cccccccc-0000-0000-0000-000000000003', 'Carla QA', 'carla@qa.test', 'hash-qa-c');

    -- Own categories: Mercado QA (user A, GASTO), Gym QA (user B, GASTO)
    INSERT INTO categoria (id, id_propietario, nombre, tipo) VALUES
        ('1a1a1a1a-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'Mercado QA', 'GASTO'),
        ('1b1b1b1b-0000-0000-0000-000000000002', 'bbbbbbbb-0000-0000-0000-000000000002', 'Gym QA',     'GASTO');

    -- Fixture movement for user A (needed by T-12: DELETE with movements)
    INSERT INTO movimiento (id, id_usuario, id_categoria, tipo, monto, fecha, metodo_pago)
    VALUES ('2a2a2a2a-0000-0000-0000-000000000001',
            'aaaaaaaa-0000-0000-0000-000000000001',
            (SELECT id FROM categoria WHERE id_propietario IS NULL AND icono = 'utensils'),
            'GASTO', 100.00, DATE '2026-01-05', 'EFECTIVO');

    -- Fixture budgets for user A (needed by T-10b duplicate and T-15 ON CONFLICT)
    INSERT INTO presupuesto (id_usuario, id_categoria, periodo, limite, notificado) VALUES
        ('aaaaaaaa-0000-0000-0000-000000000001',
         (SELECT id FROM categoria WHERE id_propietario IS NULL AND icono = 'utensils'),
         DATE '2026-01-01', 500000.00, false),
        ('aaaaaaaa-0000-0000-0000-000000000001',
         (SELECT id FROM categoria WHERE id_propietario IS NULL AND icono = 'utensils'),
         DATE '2026-03-01', 100000.00, true);
END $$;

-- ---------------------------------------------------------------- T-01 RN-01 amount bounds
DO $$ BEGIN
    BEGIN
        INSERT INTO movimiento (id_usuario, id_categoria, tipo, monto, fecha, metodo_pago)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001',
                (SELECT id FROM categoria WHERE id_propietario IS NULL AND icono = 'utensils'),
                'GASTO', 0, DATE '2026-02-01', 'EFECTIVO');
        RAISE NOTICE 'FAIL T-01a: monto 0 - expected check_violation, no error raised';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN check_violation THEN RAISE NOTICE 'PASS T-01a: monto 0 rejected with check_violation';
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-01a: monto 0 - expected check_violation, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

DO $$ BEGIN
    BEGIN
        INSERT INTO movimiento (id_usuario, id_categoria, tipo, monto, fecha, metodo_pago)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001',
                (SELECT id FROM categoria WHERE id_propietario IS NULL AND icono = 'utensils'),
                'GASTO', -1.00, DATE '2026-02-01', 'EFECTIVO');
        RAISE NOTICE 'FAIL T-01b: monto -1.00 - expected check_violation, no error raised';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN check_violation THEN RAISE NOTICE 'PASS T-01b: monto -1.00 rejected with check_violation';
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-01b: monto -1.00 - expected check_violation, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

DO $$ BEGIN
    BEGIN
        INSERT INTO movimiento (id_usuario, id_categoria, tipo, monto, fecha, metodo_pago)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001',
                (SELECT id FROM categoria WHERE id_propietario IS NULL AND icono = 'utensils'),
                'GASTO', 1000000000000, DATE '2026-02-01', 'EFECTIVO');
        RAISE NOTICE 'FAIL T-01c: monto 1 000 000 000 000 - expected numeric_value_out_of_range, no error raised';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        -- Expectation corrected after the 1st run (was check_violation): NUMERIC(14,2)
        -- overflow (22003) is raised before the CHECK is evaluated. Schema not changed.
        WHEN numeric_value_out_of_range THEN RAISE NOTICE 'PASS T-01c: monto 1 000 000 000 000 rejected with numeric_value_out_of_range (22003)';
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-01c: monto 1 000 000 000 000 - expected numeric_value_out_of_range, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

-- T-02 RN-01: upper bound accepted
DO $$ BEGIN
    BEGIN
        INSERT INTO movimiento (id_usuario, id_categoria, tipo, monto, fecha, metodo_pago)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001',
                (SELECT id FROM categoria WHERE id_propietario IS NULL AND icono = 'utensils'),
                'GASTO', 999999999999.99, DATE '2026-02-01', 'EFECTIVO');
        RAISE NOTICE 'PASS T-02a: monto 999999999999.99 accepted';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-02a: monto 999999999999.99 - expected OK, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

-- ---------------------------------------------------------------- T-03 RN-03 GASTO with INGRESO category
DO $$ BEGIN
    BEGIN
        INSERT INTO movimiento (id_usuario, id_categoria, tipo, monto, fecha, metodo_pago)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001',
                (SELECT id FROM categoria WHERE id_propietario IS NULL AND icono = 'wallet'),
                'GASTO', 10.00, DATE '2026-02-01', 'EFECTIVO');
        RAISE NOTICE 'FAIL T-03a: GASTO with INGRESO category - expected foreign_key_violation, no error raised';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN foreign_key_violation THEN RAISE NOTICE 'PASS T-03a: GASTO with INGRESO category rejected with foreign_key_violation';
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-03a: GASTO with INGRESO category - expected foreign_key_violation, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

-- ---------------------------------------------------------------- T-04 RN-04 category owned by another user
DO $$ BEGIN
    BEGIN
        INSERT INTO movimiento (id_usuario, id_categoria, tipo, monto, fecha, metodo_pago)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001',
                '1b1b1b1b-0000-0000-0000-000000000002',   -- Gym QA, owned by user B
                'GASTO', 10.00, DATE '2026-02-01', 'EFECTIVO');
        RAISE NOTICE 'FAIL T-04a: user A uses category of B - expected trigger exception (P0001), no error raised';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN raise_exception THEN RAISE NOTICE 'PASS T-04a: user A using category of B rejected by RN-04 trigger (P0001)';
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-04a: user A uses category of B - expected P0001, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

DO $$ BEGIN
    BEGIN
        INSERT INTO movimiento (id_usuario, id_categoria, tipo, monto, fecha, metodo_pago)
        VALUES ('bbbbbbbb-0000-0000-0000-000000000002',
                '1b1b1b1b-0000-0000-0000-000000000002',   -- control: owner uses own category
                'GASTO', 10.00, DATE '2026-02-01', 'EFECTIVO');
        RAISE NOTICE 'PASS T-04b: user B uses own category accepted (control)';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-04b: user B uses own category - expected OK, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

-- ---------------------------------------------------------------- T-05 RN-04 default category
DO $$ BEGIN
    BEGIN
        INSERT INTO movimiento (id_usuario, id_categoria, tipo, monto, fecha, metodo_pago)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001',
                (SELECT id FROM categoria WHERE id_propietario IS NULL AND icono = 'bus'),
                'GASTO', 10.00, DATE '2026-02-01', 'EFECTIVO');
        RAISE NOTICE 'PASS T-05a: user A uses default category accepted';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-05a: default category - expected OK, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

-- ---------------------------------------------------------------- T-06 RN-06 email
DO $$ BEGIN
    BEGIN
        INSERT INTO usuario (nombre, correo, hash_contrasena)
        VALUES ('Mayus QA', 'Mixed@QA.test', 'hash-x');
        RAISE NOTICE 'FAIL T-06a: email with uppercase - expected check_violation, no error raised';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN check_violation THEN RAISE NOTICE 'PASS T-06a: email with uppercase rejected with check_violation';
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-06a: email with uppercase - expected check_violation, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

DO $$ BEGIN
    BEGIN
        INSERT INTO usuario (nombre, correo, hash_contrasena)
        VALUES ('Dup QA', 'ana@qa.test', 'hash-x');
        RAISE NOTICE 'FAIL T-06b: duplicate email - expected unique_violation, no error raised';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN unique_violation THEN RAISE NOTICE 'PASS T-06b: duplicate email rejected with unique_violation';
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-06b: duplicate email - expected unique_violation, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

-- ---------------------------------------------------------------- T-07 RN-09 category name length
DO $$ BEGIN
    BEGIN
        INSERT INTO categoria (id_propietario, nombre, tipo)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001', '', 'GASTO');
        RAISE NOTICE 'FAIL T-07a: empty name - expected check_violation, no error raised';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN check_violation THEN RAISE NOTICE 'PASS T-07a: empty name rejected with check_violation';
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-07a: empty name - expected check_violation, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

DO $$ BEGIN
    BEGIN
        INSERT INTO categoria (id_propietario, nombre, tipo)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001', repeat('x', 41), 'GASTO');
        RAISE NOTICE 'FAIL T-07b: 41-character name - expected string_data_right_truncation, no error raised';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        -- Expectation corrected after the 1st run (was check_violation): VARCHAR(40)
        -- rejects the value (22001) before the CHECK is evaluated. Schema not changed.
        WHEN string_data_right_truncation THEN RAISE NOTICE 'PASS T-07b: 41-character name rejected with string_data_right_truncation (22001)';
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-07b: 41-character name - expected string_data_right_truncation, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

DO $$ BEGIN
    BEGIN
        INSERT INTO categoria (id_propietario, nombre, tipo)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001', repeat('x', 40), 'GASTO');
        RAISE NOTICE 'PASS T-07c: 40-character name accepted (control)';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-07c: 40-character name - expected OK, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

-- ---------------------------------------------------------------- T-08 RN-09 default names case-insensitive
DO $$ BEGIN
    BEGIN
        INSERT INTO categoria (id_propietario, nombre, tipo) VALUES (NULL, 'Comida', 'GASTO');
        INSERT INTO categoria (id_propietario, nombre, tipo) VALUES (NULL, 'comida', 'GASTO');
        RAISE NOTICE 'FAIL T-08: default "Comida" and "comida" - expected unique_violation, no error raised';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN unique_violation THEN RAISE NOTICE 'PASS T-08: default "Comida" and "comida" rejected with unique_violation';
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-08: default "Comida" and "comida" - expected unique_violation, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

-- ---------------------------------------------------------------- T-09 RN-09 same name per owner / other owner
DO $$ BEGIN
    BEGIN
        INSERT INTO categoria (id_propietario, nombre, tipo)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001', 'mercado qa', 'GASTO');   -- same owner, other case
        RAISE NOTICE 'FAIL T-09a: same owner, same name in other case - expected unique_violation, no error raised';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN unique_violation THEN RAISE NOTICE 'PASS T-09a: same owner, same name rejected with unique_violation';
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-09a: same owner, same name - expected unique_violation, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

DO $$ BEGIN
    BEGIN
        INSERT INTO categoria (id_propietario, nombre, tipo)
        VALUES ('bbbbbbbb-0000-0000-0000-000000000002', 'Mercado QA', 'GASTO');   -- other owner
        RAISE NOTICE 'PASS T-09b: same name for other owner accepted';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-09b: same name for other owner - expected OK, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

-- ---------------------------------------------------------------- T-10 RN-10 budgets
DO $$ BEGIN
    BEGIN
        INSERT INTO presupuesto (id_usuario, id_categoria, periodo, limite)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001',
                (SELECT id FROM categoria WHERE id_propietario IS NULL AND icono = 'wallet'),   -- INGRESO category
                DATE '2026-02-01', 1000.00);
        RAISE NOTICE 'FAIL T-10a: budget on INGRESO category - expected foreign_key_violation, no error raised';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN foreign_key_violation THEN RAISE NOTICE 'PASS T-10a: budget on INGRESO category rejected with foreign_key_violation';
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-10a: budget on INGRESO category - expected foreign_key_violation, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

DO $$ BEGIN
    BEGIN
        INSERT INTO presupuesto (id_usuario, id_categoria, periodo, limite)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001',
                (SELECT id FROM categoria WHERE id_propietario IS NULL AND icono = 'utensils'),
                DATE '2026-01-01', 1000.00);   -- fixture already exists for this period
        RAISE NOTICE 'FAIL T-10b: duplicate budget (user, category, period) - expected unique_violation, no error raised';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN unique_violation THEN RAISE NOTICE 'PASS T-10b: duplicate budget rejected with unique_violation';
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-10b: duplicate budget - expected unique_violation, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

DO $$ BEGIN
    BEGIN
        INSERT INTO presupuesto (id_usuario, id_categoria, periodo, limite)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001',
                (SELECT id FROM categoria WHERE id_propietario IS NULL AND icono = 'utensils'),
                DATE '2026-02-15', 1000.00);   -- not the first day of the month
        RAISE NOTICE 'FAIL T-10c: periodo 2026-02-15 - expected check_violation, no error raised';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN check_violation THEN RAISE NOTICE 'PASS T-10c: periodo not starting on day 1 rejected with check_violation';
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-10c: periodo 2026-02-15 - expected check_violation, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

DO $$ BEGIN
    BEGIN
        INSERT INTO presupuesto (id_usuario, id_categoria, periodo, limite)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001',
                (SELECT id FROM categoria WHERE id_propietario IS NULL AND icono = 'utensils'),
                DATE '2026-02-01', 1000.00);   -- control: valid budget
        RAISE NOTICE 'PASS T-10d: valid budget for period 2026-02-01 accepted (control)';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-10d: valid budget - expected OK, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

-- ---------------------------------------------------------------- T-11 GASTO / INGRESO payment method
DO $$ BEGIN
    BEGIN
        INSERT INTO movimiento (id_usuario, id_categoria, tipo, monto, fecha, metodo_pago)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001',
                (SELECT id FROM categoria WHERE id_propietario IS NULL AND icono = 'utensils'),
                'GASTO', 10.00, DATE '2026-02-01', NULL);   -- GASTO without metodo_pago
        RAISE NOTICE 'FAIL T-11a: GASTO without metodo_pago - expected check_violation, no error raised';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN check_violation THEN RAISE NOTICE 'PASS T-11a: GASTO without metodo_pago rejected with check_violation';
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-11a: GASTO without metodo_pago - expected check_violation, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

DO $$ BEGIN
    BEGIN
        INSERT INTO movimiento (id_usuario, id_categoria, tipo, monto, fecha, metodo_pago)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001',
                (SELECT id FROM categoria WHERE id_propietario IS NULL AND icono = 'wallet'),
                'INGRESO', 10.00, DATE '2026-02-01', 'EFECTIVO');   -- INGRESO with metodo_pago
        RAISE NOTICE 'FAIL T-11b: INGRESO with metodo_pago - expected check_violation, no error raised';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN check_violation THEN RAISE NOTICE 'PASS T-11b: INGRESO with metodo_pago rejected with check_violation';
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-11b: INGRESO with metodo_pago - expected check_violation, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

DO $$ BEGIN
    BEGIN
        INSERT INTO movimiento (id_usuario, id_categoria, tipo, monto, fecha, metodo_pago)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001',
                (SELECT id FROM categoria WHERE id_propietario IS NULL AND icono = 'wallet'),
                'INGRESO', 10.00, DATE '2026-02-01', NULL);   -- control: valid INGRESO
        RAISE NOTICE 'PASS T-11c: valid INGRESO without metodo_pago accepted (control)';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-11c: valid INGRESO - expected OK, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

-- ---------------------------------------------------------------- T-12 P-07 DELETE user with movements
DO $$ BEGIN
    BEGIN
        DELETE FROM usuario WHERE id = 'aaaaaaaa-0000-0000-0000-000000000001';
        RAISE NOTICE 'FAIL T-12a: DELETE user with movimientos - expected restrict_violation, no error raised';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        -- Expectation corrected after the 1st run (was foreign_key_violation): ON DELETE RESTRICT
        -- raises restrict_violation (23001). Schema not changed.
        WHEN restrict_violation THEN RAISE NOTICE 'PASS T-12a: DELETE user with movimientos rejected with restrict_violation (23001)';
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-12a: DELETE user with movimientos - expected restrict_violation, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

-- ---------------------------------------------------------------- T-13 moneda COP only
DO $$ BEGIN
    BEGIN
        INSERT INTO movimiento (id_usuario, id_categoria, tipo, monto, moneda, fecha, metodo_pago)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001',
                (SELECT id FROM categoria WHERE id_propietario IS NULL AND icono = 'utensils'),
                'GASTO', 10.00, 'USD', DATE '2026-02-01', 'EFECTIVO');
        RAISE NOTICE 'FAIL T-13a: moneda USD - expected check_violation, no error raised';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN check_violation THEN RAISE NOTICE 'PASS T-13a: moneda USD rejected with check_violation';
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-13a: moneda USD - expected check_violation, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

-- ---------------------------------------------------------------- T-14 P-16 reemplazado_por FK
DO $$ BEGIN
    BEGIN
        INSERT INTO sesion_refresco (hash, id_usuario, expira, reemplazado_por)
        VALUES (repeat('a', 64), 'aaaaaaaa-0000-0000-0000-000000000001',
                now() + interval '30 days', '3d3d3d3d-0000-0000-0000-000000000009');   -- non-existing session
        RAISE NOTICE 'FAIL T-14a: reemplazado_por to non-existing session - expected foreign_key_violation, no error raised';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN foreign_key_violation THEN RAISE NOTICE 'PASS T-14a: reemplazado_por to non-existing session rejected with foreign_key_violation';
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-14a: reemplazado_por to non-existing session - expected foreign_key_violation, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

DO $$ BEGIN
    BEGIN
        INSERT INTO sesion_refresco (hash, id_usuario, expira, reemplazado_por)
        VALUES (repeat('b', 64), 'aaaaaaaa-0000-0000-0000-000000000001',
                now() + interval '30 days', NULL);   -- control: no replacement
        RAISE NOTICE 'PASS T-14b: sesion_refresco without reemplazado_por accepted (control)';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-14b: sesion_refresco control - expected OK, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

-- ---------------------------------------------------------------- T-15 P-19 ON CONFLICT + notificado guard
DO $$
DECLARE
    v_id UUID;
    n1   INTEGER;
    n2   INTEGER;
BEGIN
    BEGIN
        INSERT INTO presupuesto (id_usuario, id_categoria, periodo, limite)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001',
                (SELECT id FROM categoria WHERE id_propietario IS NULL AND icono = 'utensils'),
                DATE '2026-03-01', 200000.00)
        ON CONFLICT (id_usuario, id_categoria, periodo)
        DO UPDATE SET limite = EXCLUDED.limite, notificado = false
        RETURNING id INTO v_id;

        UPDATE presupuesto SET notificado = true WHERE id = v_id AND NOT notificado;
        GET DIAGNOSTICS n1 = ROW_COUNT;
        UPDATE presupuesto SET notificado = true WHERE id = v_id AND NOT notificado;
        GET DIAGNOSTICS n2 = ROW_COUNT;

        IF n1 = 1 AND n2 = 0 THEN
            RAISE NOTICE 'PASS T-15: first UPDATE rows=%, second UPDATE rows=% (expected 1 and 0)', n1, n2;
        ELSE
            RAISE NOTICE 'FAIL T-15: first UPDATE rows=%, second UPDATE rows=% (expected 1 and 0)', n1, n2;
        END IF;
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-15: unexpected SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

-- ---------------------------------------------------------------- T-17 RN-14 global normalized name (PROPUESTA)
-- Normalized name = lower(btrim(nombre)) with the same tipo, across own and default categories.
-- Fixtures checked for collisions: "Mercado QA" (GASTO, user A), "Gym QA" (GASTO, user B) do not
-- match any default name of the same tipo, so no fixture needed renaming.
DO $$ BEGIN
    BEGIN
        INSERT INTO categoria (id_propietario, nombre, tipo)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001', '  alimentación ', 'GASTO');   -- own vs default "Alimentación"
        RAISE NOTICE 'FAIL T-17a: own name normalized like default Alimentacion - expected unique_violation, no error raised';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN unique_violation THEN RAISE NOTICE 'PASS T-17a: own category "  alimentación " rejected by RN-14 (unique_violation)';
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-17a: own name normalized like default - expected unique_violation, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

DO $$ BEGIN
    BEGIN
        INSERT INTO categoria (id_propietario, nombre, tipo)
        VALUES (NULL, '  mercado qa ', 'GASTO');   -- default vs existing own "Mercado QA" (user A)
        RAISE NOTICE 'FAIL T-17b: new default normalized like own category - expected unique_violation, no error raised';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN unique_violation THEN RAISE NOTICE 'PASS T-17b: new default "  mercado qa " rejected by RN-14 (unique_violation)';
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-17b: new default normalized like own category - expected unique_violation, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

DO $$ BEGIN
    BEGIN
        INSERT INTO categoria (id_propietario, nombre, tipo)
        VALUES ('aaaaaaaa-0000-0000-0000-000000000001', 'Salario', 'GASTO');   -- default "Salario" is INGRESO
        RAISE NOTICE 'PASS T-17c: own "Salario" GASTO accepted (default name exists only as INGRESO)';
        RAISE EXCEPTION USING ERRCODE = 'P9001', MESSAGE = 'qa-rollback';
    EXCEPTION
        WHEN SQLSTATE 'P9001' THEN NULL;
        WHEN OTHERS THEN RAISE NOTICE 'FAIL T-17c: own "Salario" GASTO - expected OK, got SQLSTATE % (%)', SQLSTATE, SQLERRM;
    END;
END $$;

-- ---------------------------------------------------------------- T-16 P-20 index usage (INFORMATIVE)
-- Bulk data for user C (no other test uses user C).
INSERT INTO movimiento (id_usuario, id_categoria, tipo, monto, fecha, metodo_pago)
SELECT 'cccccccc-0000-0000-0000-000000000003',
       c.ids[1 + (g % 4)],
       'GASTO',
       (g % 1000) + 1,
       DATE '2024-01-01' + (g % 900),
       'EFECTIVO'
FROM generate_series(1, 50000) AS g
CROSS JOIN (SELECT array_agg(id ORDER BY icono) AS ids
            FROM categoria WHERE id_propietario IS NULL AND tipo = 'GASTO') AS c;

ANALYZE movimiento;

DO $$
DECLARE
    q    TEXT;
    r    RECORD;
    plan TEXT := '';
BEGIN
    -- T-16a: history query (expected index: ix_movimiento_historial)
    q := $q$EXPLAIN SELECT * FROM movimiento
            WHERE id_usuario = 'cccccccc-0000-0000-0000-000000000003'
            ORDER BY fecha DESC, id DESC LIMIT 20$q$;
    plan := '';
    FOR r IN EXECUTE q LOOP
        RAISE NOTICE 'PLAN T-16a | %', r."QUERY PLAN";
        plan := plan || r."QUERY PLAN" || E'\n';
    END LOOP;
    IF plan LIKE '%ix_movimiento_historial%' THEN
        RAISE NOTICE 'PASS T-16a: history plan uses ix_movimiento_historial';
    ELSE
        RAISE NOTICE 'INFO T-16a: planner did not use ix_movimiento_historial; first plan line: %',
            split_part(plan, E'\n', 1);
    END IF;

    -- T-16b: SUM by category (expected index: ix_movimiento_suma_categoria)
    q := $q$EXPLAIN SELECT id_categoria, SUM(monto) FROM movimiento
            WHERE id_usuario = 'cccccccc-0000-0000-0000-000000000003'
              AND fecha BETWEEN DATE '2024-03-01' AND DATE '2024-06-30'
            GROUP BY id_categoria$q$;
    plan := '';
    FOR r IN EXECUTE q LOOP
        RAISE NOTICE 'PLAN T-16b | %', r."QUERY PLAN";
        plan := plan || r."QUERY PLAN" || E'\n';
    END LOOP;
    IF plan LIKE '%ix_movimiento_suma_categoria%' THEN
        RAISE NOTICE 'PASS T-16b: category sum plan uses ix_movimiento_suma_categoria';
    ELSE
        RAISE NOTICE 'INFO T-16b: planner did not use ix_movimiento_suma_categoria; first plan line: %',
            split_part(plan, E'\n', 1);
    END IF;
END $$;

-- End of tests. The database is dropped by the calling shell, not by this file.
