# Applies the four LOG-01 corrections (docs/arquitectura/correcciones-vpp.md, rows 1-4) to a Visual Paradigm
# XML export: removes the non-existent attribute Usuario.bloqueada, adds the missing public operations and the
# id/owner attributes, and turns the Usuario associations around (children hold idUsuario, not the other way).
# usage: python corregir_log01_vpp.py project.xml out.xml
import copy
import itertools
import sys
import xml.etree.ElementTree as ET

NS = 'http://www.visual-paradigm.com/product/vpuml/modelexporter'
ET.register_namespace('', NS)
ET.register_namespace('xsi', 'http://www.w3.org/2001/XMLSchema-instance')
T = lambda tag: '{%s}%s' % (NS, tag)

entrada, salida = sys.argv[1:3]
tree = ET.parse(entrada)
root = tree.getroot()
if not root.tag.startswith('{'):           # ExportXML -diagram writes the same format without a namespace
    T = lambda tag: tag
M = {}
for m in root.iter(T('Model')):          # keep the full definition, not later bare references with the same id
    if m.get('modelType') and m.get('id') not in M:
        M[m.get('id')] = m
cls = {m.get('name'): m for m in M.values() if m.get('modelType') == 'Class'}
_n = itertools.count(1)


def nuevo_id():
    while True:
        i = 'LGx%012dZ' % next(_n)
        if i not in M:
            return i


def props(e):
    return e.find(T('ModelProperties'))


def prop(e, name):
    return next((q for q in props(e) if q.get('name') == name), None)


def poner(e, name, valor):
    q = prop(e, name)
    if q.get('value') is not None or len(q) == 0:
        q.set('value', str(valor))
    else:                                  # TextModelProperty: <StringValue value=.../>
        q[0].set('value', str(valor))


def hijos(clase):
    return cls[clase].find(T('ChildModels'))


def miembro(clase, nombre):
    return next(c for c in hijos(clase) if c.get('name') == nombre)


def clonar(m):
    c = copy.deepcopy(m)
    for e in c.iter(T('Model')):
        e.set('id', nuevo_id())
    return c


def renombrar(m, nombre):
    m.set('name', nombre)
    poner(m, 'name', nombre)


def atributo(clase, plantilla, nombre, tipo, pos=None):
    a = clonar(miembro(clase, plantilla))
    renombrar(a, nombre)
    poner(a, 'type', tipo)
    poner(a, 'multiplicity', 'Unspecified')
    h = hijos(clase)
    h.insert(len(h) if pos is None else pos, a)


def operacion(clase, plantilla_clase, plantilla, nombre, retorno, parametros, despues=None, estatica=False):
    o = clonar(miembro(plantilla_clase, plantilla))
    renombrar(o, nombre)
    poner(o, 'returnType', retorno)
    poner(o, 'static', 'true' if estatica else 'false')
    ch = o.find(T('ChildModels'))
    molde = ch[0] if ch is not None and len(ch) else None
    if ch is not None:
        for p in list(ch):
            ch.remove(p)
    for pn, pt in parametros:
        p = clonar(molde)
        renombrar(p, pn)
        poner(p, 'type', pt)
        ch.append(p)
    if ch is not None and not parametros:
        o.remove(ch)
    h = hijos(clase)
    pos = list(h).index(miembro(clase, despues)) + 1 if despues else len(h)
    h.insert(pos, o)


# 1. Usuario.bloqueada does not exist in Usuario.java (the lock uses bloqueadoHasta + estaBloqueado).
hijos('Usuario').remove(miembro('Usuario', 'bloqueada'))

# 3. Identifiers and owner references (they exist in the entities).
atributo('Usuario', 'nombre', 'id', 'UUID', 0)
atributo('Movimiento', 'fecha', 'id', 'UUID', 0)
atributo('Movimiento', 'fecha', 'idUsuario', 'UUID', 1)
atributo('Categoria', 'nombre', 'id', 'UUID', 0)
atributo('Categoria', 'nombre', 'idPropietario', 'UUID', 5)
atributo('Presupuesto', 'periodo', 'id', 'UUID', 0)
atributo('Presupuesto', 'periodo', 'idUsuario', 'UUID', 1)

# 2. Missing public operations with business logic.
operacion('Usuario', 'Usuario', 'estaBloqueado', 'estaActiva', 'boolean', [], despues='estaBloqueado')
operacion('Gasto', 'Categoria', 'renombrar', 'cambiarMetodoPago', 'void', [('metodoPago', 'MetodoPago')])
operacion('Ingreso', 'Categoria', 'renombrar', 'cambiarFuente', 'void', [('fuente', 'String')])
operacion('Presupuesto', 'Presupuesto', 'debeNotificar', 'reiniciarAvisoSiVolvioAlLimite', 'boolean',
          [('consumo', 'Dinero')], despues='marcarNotificado')
operacion('Categoria', 'Categoria', 'estaDisponiblePara', 'perteneceA', 'boolean', [('idUsuario', 'UUID')],
          despues='estaDisponiblePara')
operacion('Dinero', 'Dinero', 'esPositivo', 'superaMaximo', 'boolean', [], despues='esPositivo')

# 4. Navigability towards Usuario: the child keeps the UUID of its owner.
log = next(d for d in root.iter(T('Diagram')) if d.get('name').startswith('LOG-01'))
nombres = {'registra': 'pertenece a (idUsuario)', 'define': 'pertenece a (idUsuario)',
           'crea': 'pertenece a (idPropietario)'}
for c in log.iter(T('Connector')):
    if c.get('shapeType') != 'Association' or c.get('name') not in nombres:
        continue
    a = M[c.get('model')]
    extremo_usuario, extremo_hijo = [e for e in a.iter(T('Model')) if e.get('modelType') == 'AssociationEnd']
    poner(extremo_usuario, 'navigable', 'true')
    poner(extremo_hijo, 'navigable', 'unspecified')
    renombrar(a, nombres[c.get('name')])
    c.set('name', nombres[c.get('name')])
    cap = c.find(T('Caption'))
    if cap is not None:                    # room for the longer label
        cap.set('width', '170')
        if c.get('name') == nombres['define'] and int(cap.get('x')) > 150:
            cap.set('x', str(int(cap.get('x')) - 150))   # keep it left of the line, clear of Email

# Taller class boxes for the added rows (about 15 px per row).
filas = {'Usuario': 1, 'Movimiento': 2, 'Categoria': 3, 'Presupuesto': 3, 'Gasto': 1, 'Ingreso': 1, 'Dinero': 1}
anchos = {'Presupuesto': 360, 'Gasto': 300}   # so the new signatures are not cut off


def poner_forma(s, name, valor):
    s.set(name, str(valor))
    dep = s.find(T('DiagramElementProperties'))
    q = next((q for q in (dep if dep is not None else []) if q.get('name') == name), None)
    if q is not None:
        q.set('value', str(valor))


for s in log.find(T('Shapes')):
    n = s.get('name')
    if n in filas:
        poner_forma(s, 'height', int(s.get('height')) + 15 * filas[n])
    if n in anchos:
        x, y, w0 = int(s.get('x')), int(s.get('y')), int(s.get('width'))
        h = int(s.get('height'))
        for c in log.iter(T('Connector')):     # connectors leaving the old right edge move to the new one
            if s.get('id') in (c.get('from'), c.get('to')):
                for pt in c.find(T('Points')):
                    dx = int(pt.get('x')) - (x + w0)
                    if abs(dx) <= 2 and y <= int(pt.get('y')) <= y + h:
                        pt.set('x', str(x + anchos[n] + dx))
        poner_forma(s, 'width', anchos[n])
        cap = s.find(T('Caption'))
        if cap is not None:
            cap.set('width', str(anchos[n]))

# DEV-02 (annex) shows the same classes: give them room too and push down what was below them.
dev2 = next(d for d in root.iter(T('Diagram')) if d.get('name').startswith('DEV-02'))
caja = {s.get('name'): s for s in dev2.iter(T('Shape'))
        if s.get('name') in ('modelo', 'valor', 'Usuario', 'Ingreso', 'Movimiento', 'Gasto', 'Categoria',
                             'Presupuesto', 'Dinero')}
nuevo = {  # name: (y, width, height) after the change; x stays
    'Usuario': (116, 290, 243), 'Ingreso': (374, 204, 99),
    'Movimiento': (116, 477, 178), 'Gasto': (304, 300, 98),
    'Categoria': (116, 284, 173), 'Presupuesto': (299, 360, 161),
    'Dinero': (582, 218, 145), 'modelo': (80, 1203, 411), 'valor': (546, 425, 200),
}
for n, (y, w, h) in nuevo.items():
    s = caja[n]
    for k, v in (('y', y), ('width', w), ('height', h)):
        poner_forma(s, k, v)
    cap = s.find(T('Caption'))
    if cap is not None and n not in ('modelo', 'valor'):
        cap.set('width', str(w))

dominio = next(s for s in dev2.iter(T('Shape')) if (s.get('name') or '').startswith('DOMINIO'))
poner_forma(dominio, 'width', 1263)      # the outer package must still contain "modelo"

tree.write(salida, encoding='UTF-8', xml_declaration=True)
print('ok')
