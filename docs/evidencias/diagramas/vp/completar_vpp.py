# Completes the model with everything the project really has in frontend, backend and database:
#  1. DEV-02 (annex): adds the ~35 classes/modules that exist in the code but were missing (Mongo documents and
#     repositories, session/recovery documents, CookieSesion, FinanzasApplication, DTOs, ReportesPage, components...).
#  2. LOG-02 (new): data model of the MongoDB database "finanzas" (collections, fields with BSON type, indexes,
#     references with multiplicity), taken from base-de-datos/01_colecciones.js and 02_indices.js.
#  3. DEV-01: adds the frontend (pages -> api -> controllers), so the layered view goes from the UI to the model.
# usage: python completar_vpp.py in.xml out.xml   (ExportXML -diagram output, no namespace)
import copy
import itertools
import sys
import xml.etree.ElementTree as ET

entrada, salida = sys.argv[1:3]
tree = ET.parse(entrada)
root = tree.getroot()
M = {}
for m in root.iter('Model'):
    if m.get('modelType') and m.get('id') not in M:
        M[m.get('id')] = m
padre = {c: p for p in root.iter() for c in p}
_n = itertools.count(1)
_z = itertools.count(5000, 2)


def nuevo_id():
    while True:
        i = 'CMx%012dZ' % next(_n)
        if i not in M:
            return i


def props(e):
    s = e.find('ModelProperties')
    return s if s is not None else e.find('DiagramElementProperties')


def prop(e, name):
    s = props(e)
    return next((q for q in (s if s is not None else []) if q.get('name') == name), None)


def poner(e, name, valor):
    q = prop(e, name)
    if q is None:
        return
    if len(q) and q[0].tag == 'StringValue':
        q[0].set('value', str(valor))
    else:
        q.set('value', str(valor))


def vaciar(e, name):
    q = prop(e, name)
    if q is not None:
        for c in list(q):
            q.remove(c)


def diagrama(prefijo):
    return next(d for d in root.iter('Diagram') if d.get('name').startswith(prefijo))


def modelo(nombre, tipo='Class', dentro=None):
    for m in M.values():
        if m.get('name') == nombre and m.get('modelType') == tipo:
            if dentro is None or padre.get(padre.get(m)) is M[dentro]:
                return m
    raise KeyError(nombre)


def hijos(m):
    h = m.find('ChildModels')
    if h is None:
        h = ET.Element('ChildModels')
        m.insert(1, h)
    return h


def renombrar(m, nombre):
    m.set('name', nombre)
    poner(m, 'name', nombre)


def limpiar_modelo(m):
    for tag in ('ChildModels', 'FromEndRelationships', 'ToEndRelationships'):
        for x in m.findall(tag):
            m.remove(x)
    for name in ('masterView', 'referencedByDiagramIds', 'referencedByModelAddresses'):
        vaciar(m, name)


def nueva_clase(plantilla, nombre, paquete_id, estereotipos=True):
    m = copy.deepcopy(plantilla)
    limpiar_modelo(m)
    if not estereotipos:
        vaciar(m, 'stereotypes')
    m.set('id', nuevo_id())
    renombrar(m, nombre)
    hijos(M[paquete_id]).append(m)
    M[m.get('id')] = m
    padre[m] = hijos(M[paquete_id])
    return m


TPL_ATR = next(c for c in hijos(modelo('Usuario')) if c.get('name') == 'nombre')
TPL_OP = next(c for c in hijos(modelo('Categoria')) if c.get('name') == 'renombrar')


def atributo(m, nombre, tipo, visibilidad='package', multiplicidad='Unspecified'):
    a = copy.deepcopy(TPL_ATR)
    for e in a.iter('Model'):
        e.set('id', nuevo_id())
    renombrar(a, nombre)
    poner(a, 'type', tipo)
    poner(a, 'visibility', visibilidad)
    poner(a, 'multiplicity', multiplicidad)
    hijos(m).append(a)


def operacion(m, nombre, parametros, retorno='', visibilidad='Unspecified', pos=None):
    """parametros: names, or (name, type) pairs."""
    o = copy.deepcopy(TPL_OP)
    for e in o.iter('Model'):
        e.set('id', nuevo_id())
    renombrar(o, nombre)
    poner(o, 'returnType', retorno)
    poner(o, 'visibility', visibilidad)
    ch = o.find('ChildModels')
    molde = ch[0]
    ch.remove(molde)
    for p in parametros:
        pn, pt = p if isinstance(p, tuple) else (p, '')
        q = copy.deepcopy(molde)
        q.set('id', nuevo_id())
        renombrar(q, pn)
        poner(q, 'type', pt)
        ch.append(q)
    h = hijos(m)
    h.insert(len(h) if pos is None else pos, o)


def quitar_operaciones(m, nombres):
    for c in list(hijos(m)):
        if c.get('modelType') == 'Operation' and c.get('name') in nombres:
            hijos(m).remove(c)


# Frontend API clients that did not match frontend/src/api/*.ts
_mov = modelo('MovimientosApi')
quitar_operaciones(_mov, ('registrarGasto', 'registrarIngreso'))
operacion(_mov, 'registrar', [('cmd', 'MovimientoCmd')], 'Promise<MovimientoDTO>', 'public', pos=1)
_http = modelo('ClienteHttp')
quitar_operaciones(_http, ('get', 'enviar', 'renovarYReintentar'))
for _nom, _ps, _r in (('get', [('ruta', 'string'), ('parametros', 'Record')], 'Promise<T>'),
                    ('post', [('ruta', 'string'), ('cuerpo', 'unknown')], 'Promise<T>'),
                    ('put', [('ruta', 'string'), ('cuerpo', 'unknown')], 'Promise<T>'),
                    ('patch', [('ruta', 'string'), ('cuerpo', 'unknown')], 'Promise<T>'),
                    ('delete', [('ruta', 'string'), ('parametros', 'Record')], 'Promise<void>')):
    operacion(_http, _nom, _ps, _r, "public")
operacion(_http, 'enviar', [('metodo', 'string'), ('ruta', 'string'), ('cuerpo', 'unknown'), ('reintentar', 'boolean')],
          'Promise<T>', 'private')


def miembros(m):
    h = m.find('ChildModels')
    h = list(h) if h is not None else []
    return ([c for c in h if c.get('modelType') == 'Attribute'], [c for c in h if c.get('modelType') == 'Operation'])


# ------------------------------------------------------------------------------------------- shapes
def colocar(f, x, y, w, h):
    f.attrib.update(x=str(x), y=str(y), width=str(w), height=str(h), zorder=str(next(_z)))
    for k, v in (('x', x), ('y', y), ('width', w), ('height', h), ('zOrder', f.get('zorder'))):
        poner(f, k, v)


def forma_clase(plantilla, m, x, y, w, atributos=False, operaciones=False, firma=False):
    at, op = miembros(m)
    filas = (len(at) if atributos else 0) + (len(op) if operaciones else 0)
    h = 40 if filas == 0 else 48 + 13 * filas
    f = copy.deepcopy(plantilla)
    for c in f.findall('ChildShapes'):
        f.remove(c)
    f.set('id', nuevo_id())
    f.set('model', m.get('id'))
    f.set('name', m.get('name'))
    colocar(f, x, y, w, h)
    q = prop(f, 'metaModelElement')
    if q is not None and q.find('ModelRef') is not None:
        q.find('ModelRef').set('id', m.get('id'))
    poner(f, 'showAttributeType', 1 if atributos else 0)
    poner(f, 'showOperationType', 1 if operaciones else 0)
    poner(f, 'showOperationSignature', 'true' if firma else 'false')
    poner(f, 'requestResetCaption', 'true')
    cap = f.find('Caption')
    if cap is not None:
        cap.attrib.update(x='0', y='0', width=str(w))
    return f, h


def forma_paquete(plantilla, model_id, x, y, w, h, nombre=None):
    f = copy.deepcopy(plantilla)
    for c in f.findall('ChildShapes'):
        f.remove(c)
    f.set('id', nuevo_id())
    f.set('model', model_id)
    f.set('name', nombre or M[model_id].get('name'))
    colocar(f, x, y, w, h)
    q = prop(f, 'metaModelElement')
    if q is not None and q.find('ModelRef') is not None:
        q.find('ModelRef').set('id', model_id)
    ET.SubElement(f, 'ChildShapes')
    return f


def mover(f, dx, dy):
    for s in f.iter('Shape'):
        for k, d in (('x', dx), ('y', dy)):
            v = int(s.get(k)) + d
            s.set(k, str(v))
            q = prop(s, k)
            if q is not None:
                q.set('value', str(v))


def tam(f, w=None, h=None):
    for k, v in (('width', w), ('height', h)):
        if v is not None:
            f.set(k, str(v))
            q = prop(f, k)
            if q is not None:
                q.set('value', str(v))


def caja(f):
    return int(f.get('x')), int(f.get('y')), int(f.get('width')), int(f.get('height'))


def relacion(desde, hasta, rel_id):
    for mid, tag in ((desde, 'FromEndRelationships'), (hasta, 'ToEndRelationships')):
        sec = M[mid].find(tag)
        if sec is None:
            sec = ET.SubElement(M[mid], tag)
        ET.SubElement(sec, 'RelationshipRef', {'from': desde, 'id': rel_id, 'to': hasta})


def conector(diag, plantilla_c, modelo_rel, fa, fb, puntos, nombre, etiqueta=None):
    c = copy.deepcopy(plantilla_c)
    xs, ys = [p[0] for p in puntos], [p[1] for p in puntos]
    c.attrib.update(id=nuevo_id(), model=modelo_rel.get('id'), name=nombre, x=str(min(xs)), y=str(min(ys)),
                    width=str(max(1, max(xs) - min(xs))), height=str(max(1, max(ys) - min(ys))),
                    zorder=str(next(_z) + 2000), **{'from': fa.get('id'), 'to': fb.get('id')})
    for k in ('x', 'y', 'width', 'height'):
        poner(c, k, c.get(k))
    poner(c, 'requestResetCaption', 'true')
    q = prop(c, 'metaModelElement')
    if q is not None and q.find('ModelRef') is not None:
        q.find('ModelRef').set('id', modelo_rel.get('id'))
    pts = c.find('Points')
    for p in list(pts):
        pts.remove(p)
    for x, y in puntos:
        ET.SubElement(pts, 'Point', {'x': str(x), 'y': str(y)})
    cap = c.find('Caption')
    if cap is not None:
        i = len(puntos) // 2
        mx, my = (puntos[i - 1][0] + puntos[i][0]) // 2, (puntos[i - 1][1] + puntos[i][1]) // 2
        cx, cy, cw = etiqueta or (mx + 6, my - 22, 160)
        cap.attrib.update(x=str(cx), y=str(cy), width=str(cw), height='32')
    mv = prop(modelo_rel, 'masterView')
    if mv is not None:
        for x in list(mv):
            mv.remove(x)
        ET.SubElement(mv, 'DiagramElementRef', {'displayShapeType': c.get('shapeType'), 'id': c.get('id'),
                                               'model': modelo_rel.get('id'), 'name': nombre,
                                               'shapeType': c.get('shapeType')})
    diag.find('Connectors').append(c)
    return c


def dependencia(diag, plantilla_c, fa, fb, puntos, nombre, etiqueta=None):
    m = copy.deepcopy(M[plantilla_c.get('model')])
    m.set('id', nuevo_id())
    renombrar(m, nombre)
    vaciar(m, 'stereotypes')
    a_id, b_id = fa.get('model'), fb.get('model')
    refs = list(m.iter('ModelRef'))
    refs[0].set('id', a_id)
    refs[1].set('id', b_id)
    padre[M[plantilla_c.get('model')]].append(m)
    M[m.get('id')] = m
    relacion(a_id, b_id, m.get('id'))
    return conector(diag, plantilla_c, m, fa, fb, puntos, nombre, etiqueta)


# =========================================================================== 1. DEV-02: missing classes
dev2 = diagrama('DEV-02')
# ClienteHttp now lists its 6 real operations (was 3): taller box, and the classes under it move down
_fh = next(s for s in dev2.iter('Shape') if s.get('name') == 'ClienteHttp')
_x0, _y0, _w0, _h0 = int(_fh.get('x')), int(_fh.get('y')), int(_fh.get('width')), int(_fh.get('height'))
for _s in dev2.iter('Shape'):
    if _s is not _fh and _s.get('shapeType') == 'Class' and int(_s.get('x')) == _x0 and int(_s.get('y')) > _y0:
        mover(_s, 0, 40)
tam(_fh, h=_h0 + 40)
pk2 = {s.get('name'): s for s in dev2.iter('Shape') if s.get('shapeType') == 'Package'}
TPL_C2 = next(s for s in dev2.iter('Shape') if s.get('name') == 'NotificadorSmtp')
T_JAVA = modelo('NotificadorSmtp')
T_INTF = modelo('IRepositorioUsuarios')
T_TS = modelo('Aviso')

nuevas = {}   # name -> model


def clase_java(nombre, paquete, intf=False):
    m = nueva_clase(T_INTF if intf else T_JAVA, nombre, pk2[paquete].get('model'), estereotipos=intf)
    nuevas[nombre] = m
    return m


T_PAGE = modelo('ResumenPage')
T_MODULE = modelo('ClienteHttp')


def clase_ts(nombre, paquete, plantilla=T_TS):
    m = nueva_clase(plantilla, nombre, pk2[paquete].get('model'))
    nuevas[nombre] = m
    return m


DOCS = {
    'UsuarioDocumento': ('persistencia', [('id', 'String'), ('correo', 'String'), ('nombre', 'String'),
                         ('hashContrasena', 'String'), ('estado', 'String'), ('intentosFallidos', 'int'),
                         ('bloqueadoHasta', 'LocalDateTime'), ('fechaRegistro', 'LocalDateTime'),
                         ('eliminadaEn', 'LocalDateTime')]),
    'CategoriaDocumento': ('persistencia', [('id', 'String'), ('nombre', 'String'), ('nombreClave', 'String'),
                           ('tipo', 'String'), ('icono', 'String'), ('color', 'String'), ('idPropietario', 'String')]),
    'MovimientoDocumento': ('persistencia', [('id', 'String'), ('idUsuario', 'String'), ('tipo', 'String'),
                            ('monto', 'BigDecimal'), ('moneda', 'String'), ('fecha', 'String'),
                            ('descripcion', 'String'), ('idCategoria', 'String'), ('metodoPago', 'String'),
                            ('fuente', 'String'), ('fechaRegistro', 'LocalDateTime')]),
    'PresupuestoDocumento': ('persistencia', [('id', 'String'), ('idUsuario', 'String'), ('idCategoria', 'String'),
                             ('periodo', 'String'), ('limite', 'BigDecimal'), ('moneda', 'String'),
                             ('notificado', 'boolean')]),
    'SesionDocumento': ('seguridad', [('id', 'String'), ('idUsuario', 'String'), ('hash', 'String'),
                        ('expiraEn', 'Instant'), ('reemplazadaEn', 'Instant')]),
    'TokenRecuperacionDocumento': ('seguridad', [('id', 'String'), ('idUsuario', 'String'), ('hash', 'String'),
                                   ('expiraEn', 'Instant'), ('usado', 'boolean')]),
}
for nombre, (paq, campos) in DOCS.items():
    m = clase_java(nombre, paq)
    for n, t in campos:
        atributo(m, n, t)
for n in ('UsuarioRepositorioMongo', 'CategoriaRepositorioMongo', 'MovimientoRepositorioMongo',
          'PresupuestoRepositorioMongo'):
    clase_java(n, 'persistencia', intf=True)
for n in ('SesionRepositorioMongo', 'TokenRecuperacionRepositorioMongo'):
    clase_java(n, 'seguridad', intf=True)
clase_java('CookieSesion', 'web')
clase_java('FinanzasApplication', 'configuracion')
clase_java('PuntoTendencia', 'dto')
clase_java('TotalDiario', 'dto')
clase_java('Validaciones', 'servicios')
clase_ts('ReportesPage', 'features', T_PAGE)                     # pages keep the «page» stereotype
for n in ('PantallaAcceso', 'ContextoMovimientos'):
    clase_ts(n, 'features')                                       # «component»
clase_ts('validacion', 'features', T_MODULE)                      # plain TS modules: «module»
clase_ts('tipos', 'api', T_MODULE)
for n in ('Plantilla', 'Graficos', 'Campo', 'Indicador', 'SelectorPeriodo', 'Marca', 'Iconos', 'Tema'):
    clase_ts(n, 'components')
for n in ('formato', 'useConteo', 'useTrampaFoco'):
    clase_ts(n, 'components', T_MODULE)

# where each package gets its new row(s): (classes, class width, new package width or None)
PLAN = {
    'features': (['ReportesPage', 'PantallaAcceso', 'ContextoMovimientos', 'validacion'], 230, None),
    'api': (['tipos'], 230, None),
    'components': (['Plantilla', 'Graficos', 'Campo', 'Indicador', 'SelectorPeriodo', 'Marca', 'Iconos', 'Tema',
                    'formato', 'useConteo', 'useTrampaFoco'], 150, None),
    'web': (['CookieSesion'], 230, None),
    'configuracion': (['FinanzasApplication'], 230, None),
    'seguridad': (['SesionDocumento', 'TokenRecuperacionDocumento', 'SesionRepositorioMongo',
                   'TokenRecuperacionRepositorioMongo'], 250, 1422),
    'persistencia': (['UsuarioDocumento', 'CategoriaDocumento', 'MovimientoDocumento', 'PresupuestoDocumento',
                      'UsuarioRepositorioMongo', 'CategoriaRepositorioMongo', 'MovimientoRepositorioMongo',
                      'PresupuestoRepositorioMongo'], 250, 1422),
    'dto': (['PuntoTendencia', 'TotalDiario'], 230, None),
    'servicios': (['Validaciones'], 230, None),
}
COLUMNAS = [['app', 'features', 'api', 'components'], ['web', 'configuracion', 'seguridad', 'persistencia', 'notificacion'],
            ['dto', 'puertos.entrada', 'servicios', 'puertos.salida'], ['modelo', 'valor', 'tipos', 'excepciones']]
conectores2 = list(dev2.find('Connectors'))
originales = {id(c): [(int(q.get('x')), int(q.get('y'))) for q in c.find('Points')] for c in conectores2}
movidas = []   # (zone before the move, dx, dy, right edge before, right edge after)
for columna in COLUMNAS:
    desplazamiento = 0
    for nombre in columna:
        p = pk2[nombre]
        x, y, w, h = caja(p)
        zona = (x - 3, y - 3, x + w + 3, y + h + 3)
        if desplazamiento:
            mover(p, 0, desplazamiento)
        derecha_antes = x + w
        if nombre not in PLAN:
            movidas.append((zona, desplazamiento, derecha_antes, derecha_antes))
            continue
        clases, cw, nuevo_w = PLAN[nombre]
        x, y, w, h = caja(p)
        if nuevo_w:
            tam(p, w=nuevo_w)
            w = nuevo_w
        movidas.append((zona, desplazamiento, derecha_antes, x + w))
        hijos_p = p.find('ChildShapes')
        fondo = max(int(k.get('y')) + int(k.get('height')) for k in hijos_p)
        cx, cy, fila_h = x + 20, fondo + 20, 0
        for n in clases:
            m = nuevas[n]
            es_doc = n.endswith('Documento')
            if cx + cw > x + w - 10:
                cx, cy, fila_h = x + 20, cy + fila_h + 15, 0
            f, fh = forma_clase(TPL_C2, m, cx, cy, cw, atributos=es_doc)
            z = str(int(p.get('zorder')) + 1)      # same layer as the package's own classes, under the outer box
            f.set('zorder', z)
            poner(f, 'zOrder', z)
            hijos_p.append(f)
            cx += cw + 15
            fila_h = max(fila_h, fh)
        nuevo_fondo = cy + fila_h + 20
        extra = max(0, nuevo_fondo - (y + h))
        tam(p, h=h + extra)
        desplazamiento += extra
# taller outer columns and push down whatever ran along the bottom
for s in dev2.find('Shapes'):
    if s.get('shapeType') == 'Package':
        tam(s, h=2640)
# Re-route: a point that touched a moved package moves with it; its neighbour on the same horizontal (or
# vertical) segment moves too, so the lines stay orthogonal instead of turning diagonal.
for c in conectores2:
    pts = list(c.find('Points'))
    orig = originales[id(c)]
    nuevo = [list(q) for q in orig]
    for i, (px, py) in enumerate(orig):
        for (zx0, zy0, zx1, zy1), dy, der0, der1 in movidas:
            if zx0 <= px <= zx1 and zy0 <= py <= zy1:
                dx = der1 - der0 if abs(px - der0) <= 3 else 0
                nuevo[i] = [px + dx, py + dy]
                for j in (i - 1, i + 1):
                    if 0 <= j < len(orig):
                        if dy and orig[j][1] == py:
                            nuevo[j][1] = orig[j][1] + dy
                        if dx and orig[j][0] == px:
                            nuevo[j][0] = orig[j][0] + dx
                break
    for i, q in enumerate(pts):
        if nuevo[i][1] > 2450 and orig[i][1] > 2450:
            nuevo[i][1] += 200
        q.set('x', str(nuevo[i][0]))
        q.set('y', str(nuevo[i][1]))
print('DEV-02: clases añadidas', len(nuevas))

# =========================================================================== 2. LOG-02: MongoDB data model
log1 = diagrama('LOG-01')
log2 = copy.deepcopy(log1)
log2.set('id', nuevo_id())
log2.set('name', 'LOG-02 Modelo de datos (MongoDB)')
for tag in ('Shapes', 'Connectors'):
    for c in list(log2.find(tag)):
        log2.find(tag).remove(c)
padre[log1].insert(list(padre[log1]).index(log1) + 1, log2)
TPL_C1 = next(s for s in log1.iter('Shape') if s.get('name') == 'Usuario')
TPL_ASOC = next(c for c in log1.iter('Connector') if c.get('name') == 'se clasifica en')

paq_padre = padre[M['FDx0000000001adZ']]               # same level as the four layer packages
paq_bd = copy.deepcopy(M['2p8OV4WFYFUgAREl'])          # clone the "notificacion" package model
limpiar_modelo(paq_bd)
paq_bd.set('id', nuevo_id())
renombrar(paq_bd, 'BASE DE DATOS · MongoDB "finanzas"')
paq_padre.append(paq_bd)
M[paq_bd.get('id')] = paq_bd
padre[paq_bd] = paq_padre

U, S_, D, F, B, I = 'string (UUID)', 'string', 'decimal (Decimal128)', 'date', 'bool', 'int'
COLECCIONES = {   # name: (fields [(name, type, optional)], indexes [(kind, fields)], box)
    'usuarios': ([('_id', U, 0), ('correo', S_, 0), ('nombre', S_, 0), ('hashContrasena', 'string (BCrypt)', 0),
                  ('estado', 'ACTIVA | ELIMINADA', 0), ('intentosFallidos', 'int 0..5', 0), ('bloqueadoHasta', F, 1),
                  ('fechaRegistro', F, 0), ('eliminadaEn', F, 1)],
                 [('unique', ['correo'])], (40, 330, 330)),
    'categorias': ([('_id', U, 0), ('nombre', S_, 0), ('nombreClave', S_, 0), ('tipo', 'GASTO | INGRESO', 0),
                    ('icono', S_, 1), ('color', 'string #rrggbb', 1), ('idPropietario', U, 1)],
                   [('unique', ['idPropietario', 'tipo', 'nombreClave'])], (560, 40, 330)),
    'movimientos': ([('_id', U, 0), ('idUsuario', U, 0), ('tipo', 'GASTO | INGRESO', 0), ('monto', D, 0),
                     ('moneda', 'string (3)', 0), ('fecha', 'string AAAA-MM-DD', 0), ('descripcion', 'string (140)', 1),
                     ('idCategoria', U, 0), ('metodoPago', S_, 1), ('fuente', S_, 1), ('fechaRegistro', F, 0)],
                    [('index', ['idUsuario', 'fecha', 'fechaRegistro']),
                     ('index', ['idUsuario', 'idCategoria', 'fecha', 'fechaRegistro'])], (560, 300, 360)),
    'presupuestos': ([('_id', U, 0), ('idUsuario', U, 0), ('idCategoria', U, 0), ('periodo', 'string AAAA-MM', 0),
                      ('limite', D, 0), ('moneda', 'string (3)', 0), ('notificado', B, 0)],
                     [('unique', ['idUsuario', 'idCategoria', 'periodo']), ('index', ['idUsuario', 'periodo'])],
                     (1080, 300, 360)),
    'sesiones': ([('_id', U, 0), ('idUsuario', U, 0), ('hash', 'string (SHA-256)', 0), ('expiraEn', F, 0),
                  ('reemplazadaEn', F, 1)],
                 [('unique', ['hash']), ('index', ['idUsuario']), ('ttl', ['expiraEn'])], (40, 640, 300)),
    'tokens_recuperacion': ([('_id', U, 0), ('idUsuario', U, 0), ('hash', 'string (SHA-256)', 0),
                             ('expiraEn', F, 0), ('usado', B, 0)],
                            [('unique', ['hash']), ('ttl', ['expiraEn'])], (400, 640, 300)),
}
fcol = {}
for nombre, (campos, indices, (x, y, w)) in COLECCIONES.items():
    m = nueva_clase(T_JAVA, nombre, paq_bd.get('id'), estereotipos=False)
    for n, t, opc in campos:
        atributo(m, n, t + (' [0..1]' if opc else ''), visibilidad='Unspecified')   # [0..1] = optional field
    for kind, cols in indices:
        operacion(m, kind, cols)
    f, h = forma_clase(TPL_C1, m, x, y, w, atributos=True, operaciones=True, firma=True)
    log2.find('Shapes').append(f)
    fcol[nombre] = (f, h)


def asociacion(desde, hasta, campo, mult_hasta, puntos):
    fa, fb = fcol[desde][0], fcol[hasta][0]
    a_id, b_id = fa.get('model'), fb.get('model')
    m = copy.deepcopy(M[TPL_ASOC.get('model')])
    for e in m.iter('Model'):
        e.set('id', nuevo_id())
    renombrar(m, campo)
    prop(m, 'endRelationshipFromMetaModelElement').find('ModelRef').set('id', a_id)
    prop(m, 'endRelationshipToMetaModelElement').find('ModelRef').set('id', b_id)
    for tag, mid, mult, nav in (('FromEnd', a_id, '0..*', 'unspecified'), ('ToEnd', b_id, mult_hasta, 'true')):
        end = m.find(tag).find('Model')
        prop(end, 'EndModelElement').find('ModelRef').set('id', mid)
        poner(end, 'multiplicity', mult)
        poner(end, 'navigable', nav)
    padre[M[TPL_ASOC.get('model')]].append(m)
    M[m.get('id')] = m
    relacion(a_id, b_id, m.get('id'))
    conector(log2, TPL_ASOC, m, fa, fb, puntos, campo)


def borde(nombre):
    f, h = fcol[nombre]
    x, y, w, _ = caja(f)
    return x, y, w, h


ux, uy, uw, uh = borde('usuarios')
cx_, cy_, cw_, ch_ = borde('categorias')
mx, my, mw, mh = borde('movimientos')
px, py, pw, ph = borde('presupuestos')
sx, sy, sw, sh = borde('sesiones')
tx, ty, tw, th = borde('tokens_recuperacion')
asociacion('movimientos', 'usuarios', 'idUsuario', '1', [(mx, my + 60), (ux + uw, my + 60)])
asociacion('movimientos', 'categorias', 'idCategoria', '1', [(mx + 120, my), (mx + 120, cy_ + ch_)])
asociacion('presupuestos', 'categorias', 'idCategoria', '1', [(px + 150, py), (px + 150, cy_ + 60), (cx_ + cw_, cy_ + 60)])
fondo = max(my + mh, py + ph) + 30
asociacion('presupuestos', 'usuarios', 'idUsuario', '1',
           [(px + 150, py + ph), (px + 150, fondo), (ux + 230, fondo), (ux + 230, uy + uh)])
asociacion('categorias', 'usuarios', 'idPropietario', '0..1', [(cx_, cy_ + 30), (ux + 150, cy_ + 30), (ux + 150, uy)])
asociacion('sesiones', 'usuarios', 'idUsuario', '1', [(sx + 80, sy), (sx + 80, uy + uh)])
asociacion('tokens_recuperacion', 'usuarios', 'idUsuario', '1',
           [(tx + 150, ty), (tx + 150, uy + uh + 50), (ux + 300, uy + uh + 50), (ux + 300, uy + uh)])
print('LOG-02: colecciones', len(fcol))

# =========================================================================== 3. DEV-01: add the frontend
dev1 = diagrama('DEV-01')
DX = 760
for s in dev1.find('Shapes'):
    mover(s, DX, 0)
for c in dev1.find('Connectors'):
    for pt in c.find('Points'):
        pt.set('x', str(int(pt.get('x')) + DX))
    for k in ('x',):
        v = int(c.get(k)) + DX
        c.set(k, str(v))
        poner(c, k, v)
    cap = c.find('Caption')
    if cap is not None and cap.get('x') is not None:
        cap.set('x', str(int(cap.get('x')) + DX))
TPL_P1 = next(s for s in dev1.find('Shapes') if s.get('name') == 'web')
TPL_C1D = next(s for s in dev1.iter('Shape') if s.get('name') == 'MovimientoController')
TPL_D1 = next(c for c in dev1.iter('Connector') if c.get('shapeType') == 'Dependency')
f_feat = forma_paquete(TPL_P1, pk2['features'].get('model'), 40, 40, 300, 100)
f_api = forma_paquete(TPL_P1, pk2['api'].get('model'), 410, 40, 300, 100)
for fp in (f_feat, f_api):                     # frontend packages in a warm colour
    poner(fp, 'background', 'Cr:255,248,225,255')
    if fp.find('FillColor') is not None:
        fp.find('FillColor').set('color', 'Cr:255,248,225,255')


def columna_dev1(fp, paquete, nombres, x, w, ops):
    y = 80
    for n in nombres:
        m = nuevas.get(n) or modelo(n, dentro=pk2[paquete].get('model'))
        f, h = forma_clase(TPL_C1D, m, x, y, w, operaciones=ops)
        fp.find('ChildShapes').append(f)
        y += h + 14
    return y


alto_f = columna_dev1(f_feat, 'features', ['InicioSesionPage', 'RegistroPage', 'RecuperarContrasenaPage', 'ResumenPage',
                                           'ReportesPage', 'HistorialPage', 'CategoriasPage', 'PresupuestosPage',
                                           'ConfiguracionCuentaPage'], 60, 260, False)
alto_a = columna_dev1(f_api, 'api', ['AuthApi', 'MovimientosApi', 'CategoriasApi', 'PresupuestosApi', 'ResumenApi',
                                     'CuentaApi', 'ClienteHttp'], 430, 260, True)
tam(f_feat, h=alto_f - 30)
tam(f_api, h=alto_a - 30)
dev1.find('Shapes').insert(0, f_api)
dev1.find('Shapes').insert(0, f_feat)
web = next(s for s in dev1.find('Shapes') if s.get('name') == 'web')
dependencia(dev1, TPL_D1, f_feat, f_api, [(340, 200), (410, 200)], 'usa', (345, 165, 60))
dependencia(dev1, TPL_D1, f_api, web, [(710, 200), (int(web.get('x')), 200)], 'HTTP + JSON\n/api/v1',
            (713, 150, 84))
print('DEV-01: frontend añadido')

tree.write(salida, encoding='UTF-8', xml_declaration=True)
