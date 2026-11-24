# Adds a readable development view ("DEV-01 Capas y dependencias", professor style: Controller -> Service ->
# Repository -> Model) to a Visual Paradigm XML export, keeps the detailed one as an annex, and restores the
# domain OCL constraints.  usage: python capas.py clean.xml full_original.xml out.xml
import copy
import itertools
import sys
import xml.etree.ElementTree as ET

NS = 'http://www.visual-paradigm.com/product/vpuml/modelexporter'
ET.register_namespace('', NS)
ET.register_namespace('xsi', 'http://www.w3.org/2001/XMLSchema-instance')
T = lambda tag: '{%s}%s' % (NS, tag)

limpio, completo, salida = sys.argv[1:4]
tree = ET.parse(limpio)
root = tree.getroot()
parent = {c: p for p in root.iter() for c in p}
modelos = {m.get('id'): m for m in root.iter(T('Model'))}
_n = itertools.count(1)
nuevo_id = lambda: 'CPx%012dZ' % next(_n)


def prop(e, name):
    for p in e:
        if p.tag in (T('ModelProperties'), T('DiagramElementProperties')):
            for q in p:
                if q.get('name') == name:
                    return q
    return None


def poner(e, name, valor):
    p = prop(e, name)
    if p is not None:
        p.set('value', str(valor))


def lista(e, name, valores):
    p = prop(e, name)
    for c in list(p):
        p.remove(c)
    for v in valores:
        ET.SubElement(p, T('Value'), {'value': v})


# ------------------------------------------------------------------ restore the OCL constraints
orig = ET.parse(completo).getroot()
oparent = {c: p for p in orig.iter() for c in p}
restauradas = 0
for c in orig.iter(T('Model')):
    if c.get('modelType') == 'ConstraintElement':
        clase = oparent[oparent[c]]
        destino = modelos.get(clase.get('id'))
        if destino is not None and c.get('id') not in modelos:
            hijos = destino.find(T('ChildModels'))
            if hijos is None:
                hijos = ET.Element(T('ChildModels'))
                destino.insert(1, hijos)
            hijos.append(copy.deepcopy(c))
            restauradas += 1
print('restricciones OCL restauradas:', restauradas)

# ------------------------------------------------------------------ diagrams
dev = next(d for d in root.iter(T('Diagram')) if d.get('name').startswith('DEV-01'))
dev.set('name', 'DEV-02 Detalle de clases por paquete (anexo)')
nuevo = copy.deepcopy(dev)
nuevo.set('id', nuevo_id())
nuevo.set('name', 'DEV-01 Capas y dependencias')
for tag in ('Shapes', 'Connectors'):
    sec = nuevo.find(T(tag))
    for c in list(sec):
        sec.remove(c)
cont = parent[dev]
cont.insert(list(cont).index(dev), nuevo)


def forma_por_modelo(diagrama, model_id):
    return next(s for s in diagrama.iter(T('Shape')) if s.get('model') == model_id)


def clase_por_nombre(paquete_id, nombre):
    hijos = modelos[paquete_id].find(T('ChildModels'))
    return next(c for c in hijos if c.get('modelType') == 'Class' and c.get('name') == nombre)


TPL_PAQ = forma_por_modelo(dev, 'ex8OV4WFYFUgARA6')            # package "web"
TPL_CLASE = forma_por_modelo(dev, 'UJ8OV4WFYFUgARB6')          # MovimientoController
z = itertools.count(2, 2)
formas = {}


def colocar(f, x, y, w, h):
    f.attrib.update(x=str(x), y=str(y), width=str(w), height=str(h), zorder=str(next(z)))
    for k, v in (('x', x), ('y', y), ('width', w), ('height', h), ('zOrder', f.get('zorder'))):
        poner(f, k, v)


def paquete(clave, model_id, x, y, w, h, relleno):
    f = copy.deepcopy(TPL_PAQ)
    for c in f.findall(T('ChildShapes')):
        f.remove(c)
    f.set('id', nuevo_id())
    f.set('model', model_id)
    f.set('name', modelos[model_id].get('name'))
    colocar(f, x, y, w, h)
    prop(f, 'metaModelElement').find(T('ModelRef')).set('id', model_id)
    poner(f, 'background', 'Cr:%s,255' % relleno)
    fc = f.find(T('FillColor'))
    if fc is not None:
        fc.set('color', 'Cr:%s,255' % relleno)
    hs = ET.SubElement(f, T('ChildShapes'))
    nuevo.find(T('Shapes')).append(f)
    formas[clave] = (f, hs, (x, y, w, h))


def clase(dentro, modelo, x, y, w, solo_nombre=False):
    """Class shape: attributes hidden, operations shown as name() only (or hidden for domain classes)."""
    hijos = modelo.find(T('ChildModels'))
    ops = [o for o in (hijos if hijos is not None else []) if o.get('modelType') == 'Operation']
    h = 40 if solo_nombre else 40 + 13 * len(ops)
    f = copy.deepcopy(TPL_CLASE)
    for c in f.findall(T('ChildShapes')):
        f.remove(c)
    f.set('id', nuevo_id())
    f.set('model', modelo.get('id'))
    f.set('name', modelo.get('name'))
    colocar(f, x, y, w, h)
    prop(f, 'metaModelElement').find(T('ModelRef')).set('id', modelo.get('id'))
    poner(f, 'showAttributeType', 0)                      # 0 = hide all attributes
    poner(f, 'showOperationType', 0 if solo_nombre else 1)
    poner(f, 'showOperationSignature', 'false')
    poner(f, 'requestResetCaption', 'true')
    cap = f.find(T('Caption'))
    if cap is not None:
        cap.attrib.update(x='0', y='0', width=str(w))
    formas[dentro][1].append(f)
    return h


def columna(dentro, paquete_id, nombres, x, w, solo_nombre=False, por_id=False):
    y = 80
    for n in nombres:
        m = modelos[n] if por_id else clase_por_nombre(paquete_id, n)
        y += clase(dentro, m, x, y, w, solo_nombre) + 14
    return y


def dependencia(desde, hasta, nombre, puntos):
    plantilla_c = next(c for c in dev.iter(T('Connector')) if c.get('shapeType') == 'Dependency')
    m = copy.deepcopy(modelos[plantilla_c.get('model')])
    m.set('id', nuevo_id())
    m.set('name', nombre)
    poner(m, 'name', nombre)
    fa, fb = formas[desde][0], formas[hasta][0]
    a_id, b_id = fa.get('model'), fb.get('model')
    refs = [r for r in m.iter(T('ModelRef'))]
    refs[0].set('id', a_id)
    refs[1].set('id', b_id)
    st = prop(m, 'stereotypes')
    for c in list(st):
        st.remove(c)
    mv = prop(m, 'masterView')
    for c in list(mv):
        mv.remove(c)
    parent_m = parent[modelos[plantilla_c.get('model')]]
    parent_m.append(m)
    for mid, tag in ((a_id, 'FromEndRelationships'), (b_id, 'ToEndRelationships')):
        sec = modelos[mid].find(T(tag))
        if sec is None:
            sec = ET.SubElement(modelos[mid], T(tag))
        ET.SubElement(sec, T('RelationshipRef'), {'from': a_id, 'id': m.get('id'), 'to': b_id})

    c = copy.deepcopy(plantilla_c)
    xs, ys = [p[0] for p in puntos], [p[1] for p in puntos]
    c.attrib.update(id=nuevo_id(), model=m.get('id'), name=nombre, **{'from': fa.get('id'), 'to': fb.get('id')},
                    x=str(min(xs)), y=str(min(ys)), width=str(max(1, max(xs) - min(xs))),
                    height=str(max(1, max(ys) - min(ys))), zorder=str(next(z) + 200))
    for k in ('x', 'y', 'width', 'height'):
        poner(c, k, c.get(k))
    poner(c, 'zOrder', c.get('zorder'))
    poner(c, 'requestResetCaption', 'true')
    prop(c, 'metaModelElement').find(T('ModelRef')).set('id', m.get('id'))
    pts = c.find(T('Points'))
    for p in list(pts):
        pts.remove(p)
    for px, py in puntos:
        ET.SubElement(pts, T('Point'), {'x': str(px), 'y': str(py)})
    cap = c.find(T('Caption'))
    if cap is not None:
        mx = (puntos[len(puntos) // 2 - 1][0] + puntos[len(puntos) // 2][0]) // 2
        my = (puntos[len(puntos) // 2 - 1][1] + puntos[len(puntos) // 2][1]) // 2
        cap.attrib.update(x=str(mx - 75), y=str(my - 48), width='150', height='44')
    ET.SubElement(mv, T('DiagramElementRef'), {'displayShapeType': 'Dependency', 'id': c.get('id'),
                                              'model': m.get('id'), 'name': nombre, 'shapeType': 'Dependency'})
    nuevo.find(T('Connectors')).append(c)


WEB, SERV, PERS, MOD = 'ex8OV4WFYFUgARA6', 'OZ8OV4WFYFUgARHW', 'vJ8OV4WFYFUgARDe', 'x58OV4WFYFUgARKh'
paquete('web', WEB, 40, 40, 330, 820, '232,243,252')
paquete('serv', SERV, 530, 40, 330, 820, '232,245,233')
paquete('pers', PERS, 1020, 40, 330, 520, '255,243,224')
paquete('mod', MOD, 1510, 40, 260, 420, '243,229,245')
alto = max(columna('web', WEB, ['AuthController', 'MovimientoController', 'PresupuestoController', 'CategoriaController',
                                'ConsultaController', 'CuentaController'], 60, 290),
           columna('serv', SERV, ['ServicioAutenticacion', 'ServicioMovimientos', 'ServicioPresupuestos', 'ServicioCategorias',
                                  'ServicioConsultaFinanciera', 'ServicioCuenta'], 550, 290))
alto_p = columna('pers', PERS, ['RepositorioUsuariosAdaptador', 'RepositorioMovimientosAdaptador',
                                'RepositorioPresupuestosAdaptador', 'RepositorioCategoriasAdaptador'], 1040, 290)
alto_m = columna('mod', MOD, ['FDx00000000010fZ', 'FDx000000000126Z', 'FDx00000000013dZ', 'FDx000000000141Z',
                              'FDx000000000145Z', 'FDx000000000150Z'], 1530, 220, solo_nombre=True, por_id=True)
for clave, h in (('web', alto), ('serv', alto), ('pers', alto_p), ('mod', alto_m)):
    f = formas[clave][0]
    colocar(f, int(f.get('x')), 40, int(f.get('width')), h - 40 + 10)
fondo = alto + 10
dependencia('web', 'serv', 'delega el caso de uso\n(puertos de entrada)', [(370, 300), (530, 300)])
dependencia('serv', 'pers', 'guarda y consulta\n(puertos de salida)', [(860, 250), (1020, 250)])
dependencia('pers', 'mod', 'persiste en MongoDB\ny reconstruye', [(1350, 200), (1510, 200)])
dependencia('serv', 'mod', 'usa las entidades\ny sus reglas', [(695, fondo), (695, fondo + 50), (1640, fondo + 50), (1640, alto_m + 10)])

tree.write(salida, encoding='UTF-8', xml_declaration=True)
print('ok')
