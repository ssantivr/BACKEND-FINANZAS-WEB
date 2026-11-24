-- PROPUESTA — no es un archivo original del proyecto. Generado en rama ai/redesign, 2026-11-09.
-- Implements docs/architecture/arquitectura.md section 4 (decisions D-1, D-2, D-3a, D-4..D-9).
-- Syntax valid for PostgreSQL 16+ (tested on 18 only). Fresh scratch DB; no extensions needed
-- (gen_random_uuid() is built in since PG13).

BEGIN;

-- ---------------------------------------------------------------- usuario
CREATE TABLE usuario (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre            VARCHAR(100) NOT NULL,
    correo            VARCHAR(254) NOT NULL,
    hash_contrasena   VARCHAR(100) NOT NULL,
    estado            VARCHAR(10)  NOT NULL DEFAULT 'ACTIVA',
    intentos_fallidos SMALLINT     NOT NULL DEFAULT 0,
    bloqueado_hasta   TIMESTAMPTZ,
    fecha_registro    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    eliminada_en      TIMESTAMPTZ,
    CONSTRAINT ck_usuario_nombre_no_vacio CHECK (btrim(nombre) <> ''),             -- R-04
    CONSTRAINT uq_usuario_correo UNIQUE (correo),                                  -- RN-06
    CONSTRAINT ck_usuario_correo_minuscula CHECK (correo = lower(correo)),         -- RN-06
    CONSTRAINT ck_usuario_estado CHECK (estado IN ('ACTIVA', 'ELIMINADA')),        -- D-1, D-4
    CONSTRAINT ck_usuario_intentos CHECK (intentos_fallidos BETWEEN 0 AND 5),      -- P-18
    -- D-1: logical delete; eliminada_en is set if and only if estado = 'ELIMINADA'
    CONSTRAINT ck_usuario_eliminada CHECK (
        (estado = 'ELIMINADA' AND eliminada_en IS NOT NULL)
        OR (estado = 'ACTIVA' AND eliminada_en IS NULL))
);

-- ---------------------------------------------------------------- categoria
CREATE TABLE categoria (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_propietario UUID CONSTRAINT fk_categoria_usuario REFERENCES usuario (id) ON DELETE RESTRICT,   -- NULL = default category
    nombre         VARCHAR(40) NOT NULL,
    tipo           VARCHAR(7)  NOT NULL,
    icono          VARCHAR(50),
    CONSTRAINT ck_categoria_tipo CHECK (tipo IN ('GASTO', 'INGRESO')),
    CONSTRAINT ck_categoria_nombre CHECK (char_length(nombre) BETWEEN 1 AND 40),   -- RN-09
    CONSTRAINT ck_categoria_nombre_no_vacio CHECK (btrim(nombre) <> ''),           -- R-04
    CONSTRAINT uq_categoria_id_tipo UNIQUE (id, tipo)                              -- RN-03 / RN-10 FK target
);
-- RN-09: case-insensitive uniqueness, per owner and among defaults
CREATE UNIQUE INDEX uq_categoria_propia_nombre
    ON categoria (id_propietario, tipo, lower(nombre)) WHERE id_propietario IS NOT NULL;
CREATE UNIQUE INDEX uq_categoria_predeterminada_nombre
    ON categoria (tipo, lower(nombre)) WHERE id_propietario IS NULL;

-- ---------------------------------------------------------------- movimiento
CREATE TABLE movimiento (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario     UUID          NOT NULL CONSTRAINT fk_movimiento_usuario REFERENCES usuario (id) ON DELETE RESTRICT,
    id_categoria   UUID          NOT NULL,
    tipo           VARCHAR(7)    NOT NULL,
    -- R-01: NUMERIC(14,2) rounds extra decimals silently; the 2-decimal rule of RN-01 is validated in the domain
    monto          NUMERIC(14,2) NOT NULL,
    moneda         CHAR(3)       NOT NULL DEFAULT 'COP',
    fecha          DATE          NOT NULL,
    descripcion    VARCHAR(255),
    metodo_pago    VARCHAR(30),
    fuente         VARCHAR(100),
    fecha_registro TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_movimiento_tipo CHECK (tipo IN ('GASTO', 'INGRESO')),
    CONSTRAINT ck_movimiento_monto CHECK (monto > 0 AND monto <= 999999999999.99),  -- RN-01
    CONSTRAINT ck_movimiento_moneda CHECK (moneda = 'COP'),                         -- D-8
    -- Gasto/Ingreso: written explicitly (avoids the NULL/IN bug of P-22)
    CONSTRAINT ck_movimiento_gasto_ingreso CHECK (
        (tipo = 'GASTO' AND metodo_pago IS NOT NULL AND fuente IS NULL)
        OR (tipo = 'INGRESO' AND metodo_pago IS NULL)),
    -- RN-03: movement type must match category type
    CONSTRAINT fk_movimiento_categoria_tipo FOREIGN KEY (id_categoria, tipo)
        REFERENCES categoria (id, tipo) ON DELETE RESTRICT
);

-- ---------------------------------------------------------------- presupuesto
CREATE TABLE presupuesto (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario     UUID          NOT NULL CONSTRAINT fk_presupuesto_usuario REFERENCES usuario (id) ON DELETE RESTRICT,
    id_categoria   UUID          NOT NULL,
    tipo_categoria VARCHAR(7)    NOT NULL DEFAULT 'GASTO',
    periodo        DATE          NOT NULL,
    limite         NUMERIC(14,2) NOT NULL,
    notificado     BOOLEAN       NOT NULL DEFAULT false,
    CONSTRAINT ck_presupuesto_tipo CHECK (tipo_categoria = 'GASTO'),                -- RN-10
    CONSTRAINT ck_presupuesto_limite CHECK (limite > 0 AND limite <= 999999999999.99),
    CONSTRAINT ck_presupuesto_periodo CHECK (EXTRACT(DAY FROM periodo) = 1), -- RN-10
    CONSTRAINT uq_presupuesto_periodo UNIQUE (id_usuario, id_categoria, periodo),   -- RN-10
    CONSTRAINT fk_presupuesto_categoria_tipo FOREIGN KEY (id_categoria, tipo_categoria)
        REFERENCES categoria (id, tipo) ON DELETE RESTRICT                          -- RN-10
);

-- ---------------------------------------------------------------- sesion_refresco (D-6)
CREATE TABLE sesion_refresco (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hash            CHAR(64)    NOT NULL,   -- hex SHA-256 of the token; the token itself is never stored
    id_usuario      UUID        NOT NULL CONSTRAINT fk_sesion_refresco_usuario REFERENCES usuario (id) ON DELETE RESTRICT,
    expira          TIMESTAMPTZ NOT NULL,
    revocado_en     TIMESTAMPTZ,
    reemplazado_por UUID CONSTRAINT fk_sesion_refresco_reemplazo REFERENCES sesion_refresco (id) ON DELETE RESTRICT,
    CONSTRAINT ck_sesion_refresco_hash CHECK (hash ~ '^[0-9a-f]{64}$'),             -- R-06
    CONSTRAINT uq_sesion_refresco_hash UNIQUE (hash)
);

-- ---------------------------------------------------------------- token_recuperacion (D-7)
CREATE TABLE token_recuperacion (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hash       CHAR(64)    NOT NULL,
    id_usuario UUID        NOT NULL CONSTRAINT fk_token_recuperacion_usuario REFERENCES usuario (id) ON DELETE RESTRICT,
    expira     TIMESTAMPTZ NOT NULL,
    usado_en   TIMESTAMPTZ,
    CONSTRAINT ck_token_recuperacion_hash CHECK (hash ~ '^[0-9a-f]{64}$'),          -- R-06
    CONSTRAINT uq_token_recuperacion_hash UNIQUE (hash)
);

-- ---------------------------------------------------------------- RN-04 triggers (D-5)
-- Second barrier: the application must still filter by id_usuario.
CREATE FUNCTION fn_validar_propietario_categoria() RETURNS trigger
LANGUAGE plpgsql
SET search_path = pg_catalog, public AS $$
DECLARE
    v_propietario UUID;
BEGIN
    SELECT id_propietario INTO v_propietario FROM public.categoria WHERE id = NEW.id_categoria;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'RN-04: categoria % no existe', NEW.id_categoria
            USING ERRCODE = 'P0001';
    END IF;
    IF v_propietario IS NOT NULL AND v_propietario <> NEW.id_usuario THEN
        RAISE EXCEPTION 'RN-04: la categoria % pertenece a otro usuario', NEW.id_categoria
            USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_movimiento_rn04
    BEFORE INSERT OR UPDATE ON movimiento
    FOR EACH ROW EXECUTE FUNCTION fn_validar_propietario_categoria();
CREATE TRIGGER trg_presupuesto_rn04
    BEFORE INSERT OR UPDATE ON presupuesto
    FOR EACH ROW EXECUTE FUNCTION fn_validar_propietario_categoria();

-- categoria.id_propietario is immutable, otherwise RN-04 could be bypassed after insert
CREATE FUNCTION fn_categoria_propietario_inmutable() RETURNS trigger
LANGUAGE plpgsql
SET search_path = pg_catalog, public AS $$
BEGIN
    IF NEW.id_propietario IS DISTINCT FROM OLD.id_propietario THEN
        RAISE EXCEPTION 'RN-04: categoria.id_propietario es inmutable'
            USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_categoria_propietario_inmutable
    BEFORE UPDATE ON categoria
    FOR EACH ROW EXECUTE FUNCTION fn_categoria_propietario_inmutable();

-- RN-14 (PROPUESTA): an own category cannot share tipo + normalized name (lower(btrim(nombre)))
-- with a default category (id_propietario IS NULL), and vice versa. The advisory lock serializes
-- concurrent inserts of the same tipo/name, so the cross-row check cannot race.
CREATE FUNCTION fn_categoria_nombre_unico_global() RETURNS trigger
LANGUAGE plpgsql
SET search_path = pg_catalog, public AS $$
BEGIN
    PERFORM pg_advisory_xact_lock(hashtext(NEW.tipo || ':' || lower(btrim(NEW.nombre))));
    IF NEW.id_propietario IS NOT NULL THEN
        IF EXISTS (SELECT 1 FROM public.categoria c
                   WHERE c.id_propietario IS NULL AND c.tipo = NEW.tipo
                     AND lower(btrim(c.nombre)) = lower(btrim(NEW.nombre))) THEN
            RAISE EXCEPTION 'RN-14: ya existe una categoria predeterminada % "%"', NEW.tipo, NEW.nombre
                USING ERRCODE = '23505';
        END IF;
    ELSE
        IF EXISTS (SELECT 1 FROM public.categoria c
                   WHERE c.id_propietario IS NOT NULL AND c.tipo = NEW.tipo
                     AND lower(btrim(c.nombre)) = lower(btrim(NEW.nombre))
                     AND c.id <> NEW.id) THEN
            RAISE EXCEPTION 'RN-14: ya existe una categoria propia % "%"', NEW.tipo, NEW.nombre
                USING ERRCODE = '23505';
        END IF;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER trg_categoria_nombre_unico_global
    BEFORE INSERT OR UPDATE OF nombre, tipo, id_propietario ON categoria
    FOR EACH ROW EXECUTE FUNCTION fn_categoria_nombre_unico_global();

-- ---------------------------------------------------------------- indexes (P-20)
CREATE INDEX ix_movimiento_historial ON movimiento (id_usuario, fecha DESC, id DESC);          -- 1
CREATE INDEX ix_movimiento_suma_categoria ON movimiento (id_usuario, id_categoria, fecha)
    INCLUDE (monto);                                                                           -- 2
CREATE INDEX ix_movimiento_categoria_tipo ON movimiento (id_categoria, tipo);                  -- 3
CREATE INDEX ix_presupuesto_usuario_periodo ON presupuesto (id_usuario, periodo);              -- 4
CREATE INDEX ix_presupuesto_categoria ON presupuesto (id_categoria);                         -- R-05
CREATE INDEX ix_categoria_propietario ON categoria (id_propietario);                           -- R-05
CREATE INDEX ix_sesion_refresco_reemplazo ON sesion_refresco (reemplazado_por);                -- R-05
-- 5: partial unique indexes of categoria (above)
CREATE INDEX ix_sesion_refresco_usuario ON sesion_refresco (id_usuario);                       -- 6
CREATE INDEX ix_token_recuperacion_usuario ON token_recuperacion (id_usuario);                 -- 6

-- ---------------------------------------------------------------- seed: default categories
INSERT INTO categoria (id_propietario, nombre, tipo, icono) VALUES
    (NULL, 'Alimentación',    'GASTO',   'utensils'),
    (NULL, 'Transporte',      'GASTO',   'bus'),
    (NULL, 'Vivienda',        'GASTO',   'home'),
    (NULL, 'Salud',           'GASTO',   'heart'),
    (NULL, 'Salario',         'INGRESO', 'wallet'),
    (NULL, 'Otros ingresos',  'INGRESO', 'plus');

COMMIT;
