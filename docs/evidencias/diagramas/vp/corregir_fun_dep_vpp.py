# Second review of FUN-01 and the deployment diagrams (docs/arquitectura/correcciones-vpp.md, rows 11-13):
# - FUN-01: adds the three required interfaces that ConfiguracionServicios.java wires but the diagram missed
#   (Categorias -> IRepositorioPresupuestos, Presupuestos -> IRepositorioCategorias and IRepositorioUsuarios).
# - DEP-01/DEP-02: removes the tagged values (too much text), the test tools in DEP-01 (Postman, Compass: not part
#   of the system) and the protocol word repeated next to its own stereotype on each path.
# usage: python corregir_fun_dep_vpp.py in.xml out.xml   (works on ExportXML output, with or without namespace)
import copy
import itertools
import sys
import xml.etree.ElementTree as ET

NS = 'http://www.visual-paradigm.com/product/vpuml/modelexporter'
ET.register_namespace('', NS)
entrada, salida = sys.argv[1:3]
tree = ET.parse(entrada)
root = tree.getroot()
T = (lambda tag: '{%s}%s' % (NS, tag)) if root.tag.startswith('{') else (lambda tag: tag)
M = {}
for m in root.iter(T('Model')):
    if m.get('modelType') and m.get('id') not in M:
        M[m.get('id')] = m
padre = {c: p for p in root.iter() for c in p}
_n = itertools.count(1)


def nuevo_id():
    while True:
        i = 'FDy%012dZ' % next(_n)
        if i not in M:
            return i


def prop(e, name):
    sec = e.find(T('ModelProperties'))
    if sec is None:
        sec = e.find(T('DiagramElementProperties'))
    return next((q for q in (sec if sec is not None else []) if q.get('name') == name), None)


def diagrama(prefijo):
    return next(d for d in root.iter(T('Diagram')) if d.get('name').startswith(prefijo))


def relacion(desde, hasta, rel_id):
    for mid, tag in ((desde, 'FromEndRelationships'), (hasta, 'ToEndRelationships')):
        sec = M[mid].find(T(tag))
        if sec is None:
            sec = ET.SubElement(M[mid], T(tag))
        ET.SubElement(sec, T('RelationshipRef'), {'from': desde, 'id': rel_id, 'to': hasta})


# ---------------------------------------------------------------- FUN-01: missing required interfaces
fun = diagrama('FUN-01')
formas = {s.get('id'): s for s in fun.iter(T('Shape'))}
usos = [c for c in fun.iter(T('Connector')) if c.get('shapeType') == 'Usage']
socket = {s.get('name'): s for s in fun.iter(T('Shape')) if s.get('shapeType') == 'StructuredInterface'}
# socket "10" = IRepositorioCategorias, "12" = IRepositorioPresupuestos, "13" = IRepositorioUsuarios
nuevos = [  # (port id of the component, socket, route)
    ('nuypp4WGAqACAxEY', '12', [(155, 488), (155, 594), (605, 594), (605, 630)]),   # Categorias
    ('seypp4WGAqACAxE2', '10', [(573, 488), (573, 569), (125, 569), (125, 630)]),   # Presupuestos
    ('Meypp4WGAqACAxE0', '13', [(604, 488), (604, 532), (900, 532), (900, 630)]),   # Presupuestos
]
for puerto, sock, puntos in nuevos:
    plantilla = next(c for c in usos if c.get('from') == puerto)
    m = copy.deepcopy(M[plantilla.get('model')])
    m.set('id', nuevo_id())
    destino = socket[sock]
    prop(m, 'to').find(T('ModelRef')).set('id', destino.get('model'))
    mv = prop(m, 'masterView')
    if mv is not None:
        for c in list(mv):
            mv.remove(c)
    padre[M[plantilla.get('model')]].append(m)
    desde = prop(m, 'from').find(T('ModelRef')).get('id')
    relacion(desde, destino.get('model'), m.get('id'))

    c = copy.deepcopy(plantilla)
    xs, ys = [p[0] for p in puntos], [p[1] for p in puntos]
    c.attrib.update(id=nuevo_id(), model=m.get('id'), to=destino.get('id'), x=str(min(xs)), y=str(min(ys)),
                    width=str(max(xs) - min(xs)), height=str(max(ys) - min(ys)))
    for k in ('x', 'y', 'width', 'height'):
        q = prop(c, k)
        if q is not None:
            q.set('value', c.get(k))
    q = prop(c, 'metaModelElement')
    if q is not None and q.find(T('ModelRef')) is not None:
        q.find(T('ModelRef')).set('id', m.get('id'))
    pts = c.find(T('Points'))
    for p in list(pts):
        pts.remove(p)
    for x, y in puntos:
        ET.SubElement(pts, T('Point'), {'x': str(x), 'y': str(y)})
    if mv is not None:
        ET.SubElement(mv, T('DiagramElementRef'), {'displayShapeType': 'Usage', 'id': c.get('id'),
                                                  'model': m.get('id'), 'name': '', 'shapeType': 'Usage'})
    padre[plantilla].append(c)
print('FUN-01: dependencias añadidas', len(nuevos))


def renombrar_conector(c, nuevo):
    c.set('name', nuevo)
    m = M.get(c.get('model'))
    if m is not None:
        m.set('name', nuevo)
        q = prop(m, 'name')
        if q is not None:
            q.set('value', nuevo)
    q = prop(c, 'requestResetCaption')
    if q is not None:
        q.set('value', 'true')


# Requirement codes on every line made FUN-01 unreadable; traceability lives in docs/06-trazabilidad.
for c in fun.iter(T('Connector')):
    n = c.get('name') or ''
    if 'RF-' in n or 'HU-' in n:
        renombrar_conector(c, 'HTTP + JSON' if n.startswith('HTTPS + JSON') else '')

# ---------------------------------------------------------------- DEP-01 / DEP-02: less text
borrados = set()
for prefijo in ('DEP-01', 'DEP-02'):
    dep = diagrama(prefijo)
    formas = {s.get('id'): s for s in dep.iter(T('Shape'))}
    if prefijo == 'DEP-01':
        quitar = [s for s in dep.iter(T('Shape')) if (s.get('name') or '').startswith(('Postman', 'MongoDB Compass'))]
        ids = {x.get('id') for s in quitar for x in s.iter(T('Shape'))}
        modelos = {x.get('model') for s in quitar for x in s.iter(T('Shape'))}
        for c in list(dep.iter(T('Connector'))):
            if c.get('from') in ids or c.get('to') in ids:
                modelos.add(c.get('model'))
                padre[c].remove(c)
        for s in quitar:
            padre[s].remove(s)
        for mid in modelos:
            if mid in M and M[mid] in padre:
                padre[M[mid]].remove(M[mid])
                borrados.add(mid)
        pc = next(s for s in dep.iter(T('Shape')) if (s.get('name') or '').startswith('Computador'))
        for k, v in (('height', 500),):
            pc.set(k, str(v))
            q = prop(pc, k)
            if q is not None:
                q.set('value', str(v))
    for s in dep.iter(T('Shape')):
        m = M.get(s.get('model'))
        tv = prop(m, 'taggedValues') if m is not None else None
        for cont in (tv.iter(T('Model')) if tv is not None else []):
            if cont.get('modelType') == 'TaggedValueContainer':
                hijos = cont.find(T('ChildModels'))
                if hijos is not None:
                    for h in list(hijos):
                        hijos.remove(h)
    for c in dep.iter(T('Connector')):
        n = c.get('name') or ''
        for palabra in ('HTTPS', 'HTTP', 'SMTP'):
            if n.startswith(palabra) and len(n) > len(palabra) and n[len(palabra)] in ' \n':
                nuevo = n[len(palabra):].lstrip(' ·+\n')
                nuevo = {'443\n(SPA y /api)': 'puerto 443\n(SPA y /api)',
                         '443\nreescritura /api/*': 'puerto 443\nreescritura /api/*'}.get(nuevo, nuevo)
                renombrar_conector(c, nuevo)
                break
        puertos = {'Protocolo MongoDB\nsobre TLS': 'Protocolo MongoDB\nsobre TLS · 27017', 'STARTTLS': 'STARTTLS · 587'}
        if c.get('name') in puertos:
            renombrar_conector(c, puertos[c.get('name')])

# LOG-01 / DEV-02: hollow (white) generalization triangle, as UML requires, and "tiene correo" (1 to 1).
for prefijo in ('LOG-01', 'DEV-02'):
    for c in diagrama(prefijo).iter(T('Connector')):
        if c.get('shapeType') == 'Generalization':
            for q in c.iter():
                if q.get('name') == 'background':
                    q.set('value', 'Cr:255,255,255,255')
for e in root.iter():
    if e.get('name') == 'tiene correos':
        e.set('name', 'tiene correo')
    if e.get('name') == 'name' and e.get('value') == 'tiene correos':
        e.set('value', 'tiene correo')

# references to the removed models
for sec in root.iter():
    for r in list(sec):
        if r.tag == T('RelationshipRef') and r.get('id') in borrados:
            sec.remove(r)
print('DEP: modelos quitados', len(borrados))
tree.write(salida, encoding='UTF-8', xml_declaration=True)
