# Resultados de pruebas del esquema (T-01 a T-16)

- Esquema probado: `docs/architecture/V1__esquema.propuesta.sql` (PostgreSQL 18.6, Windows).
- Plan de pruebas: `docs/architecture/arquitectura.md` §6.
- Script: `docs/evidencias/pruebas/tests_esquema.sql`.
- Log de ejecución: `docs/evidencias/pruebas/F5_tests_esquema.log` (fecha UTC 2026-11-09T17:51:38Z, comando exacto en la cabecera).
- Log de aplicación del DDL: `docs/evidencias/pruebas/F5_ddl_apply.log` (exit 0, `INSERT 0 6`, `COMMIT`).
- Base de datos `fp_qa_test`: creada para la prueba y eliminada al terminar (`DROP DATABASE`; consulta posterior: 0 filas en `pg_database`).

Cada caso es un sub-caso (`T-xxa`, `T-xxb`...). La columna "Obtenido" copia el texto de la línea `NOTICE` del log (sin el prefijo `psql:...:NN:`). Las líneas de tipo `PLAN` del T-16 están en el log, no aquí.

| ID | Regla | Caso | Esperado | Obtenido (log) | Resultado |
|---|---|---|---|---|---|
| T-01a | RN-01 | Monto 0 | check_violation | PASS T-01a: monto 0 rejected with check_violation | PASS |
| T-01b | RN-01 | Monto -1.00 | check_violation | PASS T-01b: monto -1.00 rejected with check_violation | PASS |
| T-01c | RN-01 | Monto 1 000 000 000 000 | check_violation | FAIL T-01c: monto 1 000 000 000 000 - expected check_violation, got SQLSTATE 22003 (desbordamiento de campo numeric) | FAIL |
| T-02a | RN-01 | Monto 999999999999.99 | OK | PASS T-02a: monto 999999999999.99 accepted | PASS |
| T-03a | RN-03 | GASTO con categoría INGRESO | foreign_key_violation | PASS T-03a: GASTO with INGRESO category rejected with foreign_key_violation | PASS |
| T-04a | RN-04 | Usuario A con categoría propia de B | Excepción del trigger (P0001) | PASS T-04a: user A using category of B rejected by RN-04 trigger (P0001) | PASS |
| T-04b | RN-04 | Usuario B con su categoría (control) | OK | PASS T-04b: user B uses own category accepted (control) | PASS |
| T-05a | RN-04 | Categoría predeterminada | OK | PASS T-05a: user A uses default category accepted | PASS |
| T-06a | RN-06 | Correo con mayúsculas | check_violation | PASS T-06a: email with uppercase rejected with check_violation | PASS |
| T-06b | RN-06 | Correo duplicado | unique_violation | PASS T-06b: duplicate email rejected with unique_violation | PASS |
| T-07a | RN-09 | Nombre de 0 caracteres | check_violation | PASS T-07a: empty name rejected with check_violation | PASS |
| T-07b | RN-09 | Nombre de 41 caracteres | check_violation | FAIL T-07b: 41-character name - expected check_violation, got SQLSTATE 22001 (el valor es demasiado largo para el tipo character varying(40)) | FAIL |
| T-07c | RN-09 | Nombre de 40 caracteres (control) | OK | PASS T-07c: 40-character name accepted (control) | PASS |
| T-08 | RN-09 | Predeterminadas "Comida" y "comida" | unique_violation | PASS T-08: default "Comida" and "comida" rejected with unique_violation | PASS |
| T-09a | RN-09 | Mismo nombre (otra capitalización), mismo usuario | unique_violation | PASS T-09a: same owner, same name rejected with unique_violation | PASS |
| T-09b | RN-09 | Mismo nombre, otro usuario | OK | PASS T-09b: same name for other owner accepted | PASS |
| T-10a | RN-10 | Presupuesto sobre categoría INGRESO | foreign_key_violation | PASS T-10a: budget on INGRESO category rejected with foreign_key_violation | PASS |
| T-10b | RN-10 | Presupuesto duplicado (usuario, categoría, periodo) | unique_violation | PASS T-10b: duplicate budget rejected with unique_violation | PASS |
| T-10c | RN-10 | Periodo 2026-02-15 | check_violation | PASS T-10c: periodo not starting on day 1 rejected with check_violation | PASS |
| T-10d | RN-10 | Periodo 2026-02-01 (control) | OK | PASS T-10d: valid budget for period 2026-02-01 accepted (control) | PASS |
| T-11a | Gasto/Ingreso | GASTO sin metodo_pago | check_violation | PASS T-11a: GASTO without metodo_pago rejected with check_violation | PASS |
| T-11b | Gasto/Ingreso | INGRESO con metodo_pago | check_violation | PASS T-11b: INGRESO with metodo_pago rejected with check_violation | PASS |
| T-11c | Gasto/Ingreso | INGRESO sin metodo_pago (control) | OK | PASS T-11c: valid INGRESO without metodo_pago accepted (control) | PASS |
| T-12a | P-07 | DELETE de usuario con movimientos | foreign_key_violation | FAIL T-12a: DELETE user with movimientos - expected foreign_key_violation, got SQLSTATE 23001 (update or delete on table "usuario" violates RESTRICT setting of foreign key constraint "fk_categoria_usuario" on table "categoria") | FAIL |
| T-13a | Moneda | moneda = 'USD' | check_violation | PASS T-13a: moneda USD rejected with check_violation | PASS |
| T-14a | P-16 | reemplazado_por apunta a sesión inexistente | foreign_key_violation | PASS T-14a: reemplazado_por to non-existing session rejected with foreign_key_violation | PASS |
| T-14b | P-16 | Sesión sin reemplazado_por (control) | OK | PASS T-14b: sesion_refresco without reemplazado_por accepted (control) | PASS |
| T-15 | P-19 | ON CONFLICT y segundo UPDATE con NOT notificado | Primer UPDATE 1 fila; segundo 0 | PASS T-15: first UPDATE rows=1, second UPDATE rows=0 (expected 1 and 0) | PASS |
| T-16a | P-20 | EXPLAIN historial (informativa) | Usa ix_movimiento_historial | PASS T-16a: history plan uses ix_movimiento_historial | PASS |
| T-16b | P-20 | EXPLAIN suma por categoría (informativa) | Usa ix_movimiento_suma_categoria | INFO T-16b: planner did not use ix_movimiento_suma_categoria; first plan line: HashAggregate  (cost=1226.13..1226.18 rows=4 width=48) | INFO |

## 1.ª ejecución (log F5_tests_esquema.log)

La 1.ª ejecución dio 3 FAIL causados por códigos esperados incorrectos en el script, no por el esquema (ver la nota siguiente). Las expectativas se corrigieron en la 2.ª ejecución; el esquema no se modificó.

## Análisis de los FAIL de la 1.ª ejecución (no se corrigió el esquema)

- **T-01c** (RN-01): `1000000000000` tiene 13 dígitos enteros y `NUMERIC(14,2)` admite 12. PostgreSQL rechaza el valor con 22003 antes de evaluar el `CHECK`. El dato no se guarda, pero el SQLSTATE no es `check_violation`. Hay que decidir si la expectativa de §6 se ajusta a 22003 o si se acepta el resultado.
- **T-07b** (RN-09): `VARCHAR(40)` rechaza un nombre de 41 caracteres con 22001 antes del `CHECK char_length`. Mismo caso: el dato no se guarda, distinto SQLSTATE.
- **T-12a** (P-07): `ON DELETE RESTRICT` produce `restrict_violation` (23001), no `foreign_key_violation` (23503). El esquema se comporta como está definido (el borrado se bloquea). La expectativa de §6 está mal especificada para RESTRICT. Decisión pendiente.

## Recuento (grep sobre el log)

```
$ grep -cE "NOTICE:  PASS T-" F5_tests_esquema.log
26
$ grep -cE "NOTICE:  FAIL T-" F5_tests_esquema.log
3
$ grep -cE "NOTICE:  INFO T-" F5_tests_esquema.log
1
$ grep -cE "^psql:.*ERROR:" F5_tests_esquema.log
0
```

Totales: **PASS 26 / FAIL 3 / INFO 1** (30 sub-casos).
Los `FAIL` son T-01c, T-07b y T-12a. El `INFO` es T-16b.

## Notas

- El T-16 se ejecutó con 50 000 movimientos para un usuario de prueba y `ANALYZE`. La prueba mide solo el plan del planificador en este equipo y en este volumen (**NO VERIFICADO** en otros volúmenes o versiones).
- No se ejecutó PostgreSQL 16 ni otros motores. No se probó lógica Java ni rendimiento en carga.

## 2.ª ejecución (v2)

- Esquema: `docs/architecture/V1__esquema.propuesta.sql` (incluye RN-14 PROPUESTA, trigger `fn_categoria_nombre_unico_global`).
- Script: `tests_esquema.sql` (versión v2; expectativas de T-01c, T-07b y T-12a corregidas tras la 1.ª ejecución, con comentario en cada una; T-17 nuevo).
- Logs: `F5_ddl_apply_v2.log` (exit 0) y `F5_tests_esquema_v2.log` (fecha UTC 2026-11-09T17:58:19Z, PostgreSQL 18.6, comando exacto en la cabecera). Los logs de la 1.ª ejecución no se modificaron.
- Base `fp_qa_test` recreada desde cero y eliminada al terminar (`DROP DATABASE`; consulta posterior: 0).

| ID | Regla | Caso | Esperado | Obtenido (log v2) | Resultado |
|---|---|---|---|---|---|
| T-01a | RN-01 | Monto 0 | check_violation | PASS T-01a: monto 0 rejected with check_violation | PASS |
| T-01b | RN-01 | Monto -1.00 | check_violation | PASS T-01b: monto -1.00 rejected with check_violation | PASS |
| T-01c | RN-01 | Monto 1 000 000 000 000 | numeric_value_out_of_range (22003) | PASS T-01c: monto 1 000 000 000 000 rejected with numeric_value_out_of_range (22003) | PASS |
| T-02a | RN-01 | Monto 999999999999.99 | OK | PASS T-02a: monto 999999999999.99 accepted | PASS |
| T-03a | RN-03 | GASTO con categoría INGRESO | foreign_key_violation | PASS T-03a: GASTO with INGRESO category rejected with foreign_key_violation | PASS |
| T-04a | RN-04 | Usuario A con categoría propia de B | Excepción del trigger (P0001) | PASS T-04a: user A using category of B rejected by RN-04 trigger (P0001) | PASS |
| T-04b | RN-04 | Usuario B con su categoría (control) | OK | PASS T-04b: user B uses own category accepted (control) | PASS |
| T-05a | RN-04 | Categoría predeterminada | OK | PASS T-05a: user A uses default category accepted | PASS |
| T-06a | RN-06 | Correo con mayúsculas | check_violation | PASS T-06a: email with uppercase rejected with check_violation | PASS |
| T-06b | RN-06 | Correo duplicado | unique_violation | PASS T-06b: duplicate email rejected with unique_violation | PASS |
| T-07a | RN-09 | Nombre de 0 caracteres | check_violation | PASS T-07a: empty name rejected with check_violation | PASS |
| T-07b | RN-09 | Nombre de 41 caracteres | string_data_right_truncation (22001) | PASS T-07b: 41-character name rejected with string_data_right_truncation (22001) | PASS |
| T-07c | RN-09 | Nombre de 40 caracteres (control) | OK | PASS T-07c: 40-character name accepted (control) | PASS |
| T-08 | RN-09 | Predeterminadas "Comida" y "comida" | unique_violation | PASS T-08: default "Comida" and "comida" rejected with unique_violation | PASS |
| T-09a | RN-09 | Mismo nombre (otra capitalización), mismo usuario | unique_violation | PASS T-09a: same owner, same name rejected with unique_violation | PASS |
| T-09b | RN-09 | Mismo nombre, otro usuario | OK | PASS T-09b: same name for other owner accepted | PASS |
| T-10a | RN-10 | Presupuesto sobre categoría INGRESO | foreign_key_violation | PASS T-10a: budget on INGRESO category rejected with foreign_key_violation | PASS |
| T-10b | RN-10 | Presupuesto duplicado | unique_violation | PASS T-10b: duplicate budget rejected with unique_violation | PASS |
| T-10c | RN-10 | Periodo 2026-02-15 | check_violation | PASS T-10c: periodo not starting on day 1 rejected with check_violation | PASS |
| T-10d | RN-10 | Periodo 2026-02-01 (control) | OK | PASS T-10d: valid budget for period 2026-02-01 accepted (control) | PASS |
| T-11a | Gasto/Ingreso | GASTO sin metodo_pago | check_violation | PASS T-11a: GASTO without metodo_pago rejected with check_violation | PASS |
| T-11b | Gasto/Ingreso | INGRESO con metodo_pago | check_violation | PASS T-11b: INGRESO with metodo_pago rejected with check_violation | PASS |
| T-11c | Gasto/Ingreso | INGRESO sin metodo_pago (control) | OK | PASS T-11c: valid INGRESO without metodo_pago accepted (control) | PASS |
| T-12a | P-07 | DELETE de usuario con movimientos | restrict_violation (23001) | PASS T-12a: DELETE user with movimientos rejected with restrict_violation (23001) | PASS |
| T-13a | Moneda | moneda = 'USD' | check_violation | PASS T-13a: moneda USD rejected with check_violation | PASS |
| T-14a | P-16 | reemplazado_por apunta a sesión inexistente | foreign_key_violation | PASS T-14a: reemplazado_por to non-existing session rejected with foreign_key_violation | PASS |
| T-14b | P-16 | Sesión sin reemplazado_por (control) | OK | PASS T-14b: sesion_refresco without reemplazado_por accepted (control) | PASS |
| T-15 | P-19 | ON CONFLICT y segundo UPDATE con NOT notificado | Primer UPDATE 1 fila; segundo 0 | PASS T-15: first UPDATE rows=1, second UPDATE rows=0 (expected 1 and 0) | PASS |
| T-16a | P-20 | EXPLAIN historial (informativa) | Usa ix_movimiento_historial | PASS T-16a: history plan uses ix_movimiento_historial | PASS |
| T-16b | P-20 | EXPLAIN suma por categoría (informativa) | Usa ix_movimiento_suma_categoria | INFO T-16b: planner did not use ix_movimiento_suma_categoria; first plan line: HashAggregate  (cost=1224.37..1224.42 rows=4 width=48) | INFO |
| T-17a | RN-14 (PROPUESTA) | Propia "  alimentación " (GASTO) con el nombre de la predeterminada "Alimentación" | unique_violation | PASS T-17a: own category "  alimentación " rejected by RN-14 (unique_violation) | PASS |
| T-17b | RN-14 (PROPUESTA) | Predeterminada "  mercado qa " (GASTO) con el nombre de una propia | unique_violation | PASS T-17b: new default "  mercado qa " rejected by RN-14 (unique_violation) | PASS |
| T-17c | RN-14 (PROPUESTA) | Propia "Salario" GASTO (la predeterminada es INGRESO) | OK | PASS T-17c: own "Salario" GASTO accepted (default name exists only as INGRESO) | PASS |

Totales (grep sobre `F5_tests_esquema_v2.log`):

```
$ grep -cE "NOTICE:  PASS T-" F5_tests_esquema_v2.log
32
$ grep -cE "NOTICE:  FAIL T-" F5_tests_esquema_v2.log
0
$ grep -cE "NOTICE:  INFO T-" F5_tests_esquema_v2.log
1
$ grep -cE "^psql:.*ERROR:" F5_tests_esquema_v2.log
0
```

Totales v2: **PASS 32 / FAIL 0 / INFO 1** (33 sub-casos). El INFO es T-16b (el planificador usó `ix_movimiento_historial` en el Bitmap Index Scan, no `ix_movimiento_suma_categoria`).
